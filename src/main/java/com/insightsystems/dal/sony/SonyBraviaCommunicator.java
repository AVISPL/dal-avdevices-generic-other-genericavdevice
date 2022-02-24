/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.insightsystems.dal.sony;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableMap;
import com.insightsystems.dal.sony.data.Constant;
import com.insightsystems.dal.sony.data.commands.LCDCommands;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;

import java.util.*;

import static com.insightsystems.dal.sony.data.Constant.*;
import static com.insightsystems.dal.sony.data.commands.LCDCommands.*;

/**
 * <h1>Sony LCD Device Adapter</h1>
 * Company: Insight Systems
 *
 * @author Jayden Loone (JaydenL-Insight), Maksym Rossiitsev (MaksimR-AVISPL)
 * @version 0.3
 * @implSpec Device must support REST API and have PSK authentication enabled. PSK should be entered as device HTTP password. Login username field is unused
 * @see <a href="https://pro-bravia.sony.net/develop/integrate/rest-api/spec/">Sony Rest API Spec</a>
 */
public class SonyBraviaCommunicator extends RestCommunicator implements Controller, Monitorable {
    public SonyBraviaCommunicator() {
        this.setBaseUri("sony");
        this.setContentType("application/json");
    }

    @Override
    protected void authenticate() {
    }

    @Override
    public List<Statistics> getMultipleStatistics() throws Exception {
        ExtendedStatistics extStats = new ExtendedStatistics();
        Map<String, String> stats = new HashMap<>();
        List<AdvancedControllableProperty> controls = new ArrayList<>();
        //Device Info
        generateDeviceInfoStatistics(stats);
        //Network Information
        generateNetworkStatistics(stats);
        //Device Inputs
        generateInputStatistics(stats, controls);
        //Volume Information
        generateVolumeStatistics(stats, controls);
        //Power State
        generatePowerStatistics(stats, controls);
        //Available applications
        generateApplicationsStatistics(stats, controls);
        //Led Indicator
        generateLEDIndicatorStatistics(stats, controls);
        //Audio devices
        generateAudioStatistics(stats, controls);
        //Application status
        generateApplicationStatusStatistics(stats);
        //Content statistics (ports)
        generateContentStatistics(stats);
        //System DateTime
        stats.put(DATE_TIME_PROPERTY, this.doPost(SYSTEM_URI, getCurrentTime, JsonNode.class).at(DATE_TIME_URI).asText());
        //Power Save Modes
        JsonNode powerSave = this.doPost(SYSTEM_URI, getPowerSavingMode, JsonNode.class);
        createDropdown(stats, controls, POWER_SAVE_MODES_LABELS, POWER_SAVE_MODES_VALUES, POWER_SAVE_MODE_PROPERTY, powerSave.at(MODE_0_URI).asText());
        //Stateless Control Buttons
        createButton(stats, controls, REBOOT_PROPERTY, "Reboot", "Rebooting", 10_000L);
        createButton(stats, controls, TERMINATE_APPS_PROPERTY, "Kill Apps", "Killing", 1_000L);

        extStats.setStatistics(stats);
        extStats.setControllableProperties(controls);
        return Collections.singletonList(extStats);
    }

    /**
     * Retrieve data from {@link Constant#SYSTEM_URI} endpoint with
     * {@link LCDCommands#interfaceInformation} and
     * {@link LCDCommands#getSystemInformation} payloads,
     * and populate it as a set of properties:
     *
     * {@value Constant#MODEL_PROPERTY}, {@value Constant#PRODUCT_NAME_PROPERTY},
     * {@value Constant#PRODUCT_CATEGORY_PROPERTY}, {@value Constant#INTERFACE_VERSION_PROPERTY}
     * {@value Constant#SERIAL_NUMBER_PROPERTY}
     *
     * @param statistics to keep collected properties
     * @throws Exception if any error occurs
     * */
    private void generateDeviceInfoStatistics(Map<String, String> statistics) throws Exception {
        JsonNode devInfo = this.doPost(SYSTEM_URI, interfaceInformation, JsonNode.class).at(RESULT_0_URI);
        statistics.put(MODEL_PROPERTY, devInfo.at(MODEL_NAME_URI).asText());
        statistics.put(PRODUCT_NAME_PROPERTY, devInfo.at(PRODUCT_NAME_URI).asText());
        statistics.put(PRODUCT_CATEGORY_PROPERTY, devInfo.at(PRODUCT_CATEGORY_URI).asText());
        statistics.put(INTERFACE_VERSION_PROPERTY, devInfo.at(INTERFACE_VERSION_URI).asText());

        JsonNode sysInfo = this.doPost(SYSTEM_URI, getSystemInformation, JsonNode.class).at(RESULT_0_URI);
        statistics.put(SERIAL_NUMBER_PROPERTY, sysInfo.at(SERIAL_URI).asText());
    }

    /**
     * Retrieve data from {@link Constant#AV_CONTENT_URI} endpoint with
     * {@link LCDCommands#getCurrentExternalInputsStatus} and
     * {@link LCDCommands#getPlayingContentInfo} payloads,
     * and populate it as a set of properties:
     *
     * {@value Constant#SIGNAL_PRESENCE_GROUP} (with labels attached, per input),
     * {@value Constant#INPUT_PROPERTY} (a dropdown control)
     *
     * @param statistics to keep collected properties
     * @param controls to keep controllable properties
     * @throws Exception if any error occurs
     * */
    private void generateInputStatistics(Map<String, String> statistics, List<AdvancedControllableProperty> controls) throws Exception {
        ArrayNode inputs = (ArrayNode) this.doPost(AV_CONTENT_URI, getCurrentExternalInputsStatus, JsonNode.class).at(RESULT_0_URI);
        String[] inputLabels = new String[inputs.size()];
        String[] inputOptions = new String[inputs.size()];
        for (int i = 0; i < inputs.size(); i++) {
            JsonNode input = inputs.get(i);
            inputLabels[i] = tidyString(input.at(TITLE_URI).asText());
            inputOptions[i] = input.at(URI_URI).asText();
            statistics.put(SIGNAL_PRESENCE_GROUP + inputLabels[i], input.at(CONNECTION_URI).asBoolean() ? "Connected" : "Not connected");
        }

        String currentInput = this.doPost(AV_CONTENT_URI, getPlayingContentInfo, JsonNode.class).at(RESULT_0_URI_URI).asText();
        createDropdown(statistics, controls, inputLabels, inputOptions, INPUT_PROPERTY, currentInput);
    }

    /**
     * Retrieve data from {@link Constant#AUDIO_URI} endpoint with
     * {@link LCDCommands#getVolumeInformation} payload
     * and populate it as a set of properties:
     *
     * {@value Constant#CONTROL_GROUP} (with {@link Constant#TARGET_URI} and Mute/Volume values attached),
     * effectively generates controllable properties for them.
     *
     * @param statistics to keep collected properties
     * @param controls to keep controllable properties
     * @throws Exception if any error occurs
     * */
    private void generateVolumeStatistics(Map<String, String> statistics, List<AdvancedControllableProperty> controls) throws Exception {
        JsonNode audioResponse = this.doPost(AUDIO_URI, getVolumeInformation, JsonNode.class);
        String audioErrors = getErrors(audioResponse);
        if (audioErrors.equals("")) {
            ArrayNode audioOuts = (ArrayNode) audioResponse.at(RESULT_0_URI);
            for (int i = 0; i < audioOuts.size(); i++) {
                JsonNode out = audioOuts.get(i);
                String name = CONTROL_GROUP + out.at(TARGET_URI);
                createSlider(statistics, controls, name + "Volume",
                        (float) out.at(MIN_VOLUME_URI).asDouble(), (float) out.at(MAX_VOLUME_URI).asDouble(), (float) out.at(VOLUME_URI).asDouble());
                createSwitch(statistics, controls, name + "Mute", out.at(MUTE_URI).asBoolean(), "On", "Off");
            }
        } else {
            if (this.logger.isTraceEnabled()) {
                this.logger.trace(audioErrors);
            }
        }
    }

    /**
     * Retrieve data from {@link Constant#SYSTEM_URI} endpoint with
     * {@link LCDCommands#getPowerStatus} payload
     * and populate it as a set of properties:
     *
     * {@value Constant#POWER_PROPERTY}, effectively generates controllable property for it.
     *
     * @param statistics to keep collected properties
     * @param controls to keep controllable properties
     * @throws Exception if any error occurs
     * */
    private void generatePowerStatistics(Map<String, String> statistics, List<AdvancedControllableProperty> controls) throws Exception {
        String powerState = this.doPost(SYSTEM_URI, getPowerStatus, JsonNode.class).at(STATUS_0_URI).asText();
        createSwitch(statistics, controls, POWER_PROPERTY, powerState.equalsIgnoreCase("active"), "On", "Off");
    }

    /**
     * Retrieve data from {@link Constant#APP_CONTROL_URI} endpoint with
     * {@link LCDCommands#getApplicationList} payload
     * and populate it as a set of properties:
     *
     * {@value Constant#LAUNCH_APPLICATION_PROPERTY}, effectively generates controllable property for it,
     * allowing to launch one of the available applications.
     *
     * @param statistics to keep collected properties
     * @param controls to keep controllable properties
     * @throws Exception if any error occurs
     * */
    private void generateApplicationsStatistics(Map<String, String> statistics, List<AdvancedControllableProperty> controls) throws Exception {
        ArrayNode apps = (ArrayNode) this.doPost(APP_CONTROL_URI, getApplicationList, JsonNode.class).at(RESULT_0_URI);
        String[] appLabels = new String[apps.size() + 1];
        String[] appOptions = new String[apps.size() + 1];

        appLabels[apps.size()] = "-Select Application-";
        appOptions[apps.size()] = "";
        for (int i = 0; i < apps.size(); i++) {
            JsonNode app = apps.get(i);
            appLabels[i] = app.at(TITLE_URI).asText();
            appOptions[i] = app.at(URI_URI).asText();
        }
        createDropdown(statistics, controls, appLabels, appOptions, LAUNCH_APPLICATION_PROPERTY, "");

    }

    /**
     * Retrieve data from {@link Constant#AV_CONTENT_URI} endpoint with
     * {@link LCDCommands#getContentList} payload with different set of parameters,
     *
     * @param statistics to keep collected properties
     * @throws Exception if any error occurs
     * */
    private void generateContentStatistics(Map<String, String> statistics) throws Exception {
        JsonNode cecCount = this.doPost(AV_CONTENT_URI, getContentList
                .withParams(ImmutableMap.of("stIdx", 0, "cnt", 50, "uri", "extInput:cec")), JsonNode.class).at(RESULT_0_URI);
        JsonNode hdmiCount = this.doPost(AV_CONTENT_URI, getContentList
                .withParams(ImmutableMap.of("stIdx", 0, "cnt", 50, "uri", "extInput:hdmi")), JsonNode.class).at(RESULT_0_URI);
        JsonNode componentCount = this.doPost(AV_CONTENT_URI, getContentList
                .withParams(ImmutableMap.of("stIdx", 0, "cnt", 50, "uri", "extInput:component")), JsonNode.class).at(RESULT_0_URI);
        JsonNode compositeCount = this.doPost(AV_CONTENT_URI, getContentList
                .withParams(ImmutableMap.of("stIdx", 0, "cnt", 50, "uri", "extInput:composite")), JsonNode.class).at(RESULT_0_URI);
        JsonNode widiCount = this.doPost(AV_CONTENT_URI, getContentList
                .withParams(ImmutableMap.of("stIdx", 0, "cnt", 50, "uri", "extInput:widi")), JsonNode.class).at(RESULT_0_URI);

        populateContentInformation(hdmiCount, statistics, CONTENT_INFORMATION_TOTAL_HDMI_COUNT, CONTENT_INFORMATION_HDMI_PORT);
        populateContentInformation(cecCount, statistics, CONTENT_INFORMATION_TOTAL_CEC_COUNT, CONTENT_INFORMATION_CEC_PORT);
        populateContentInformation(componentCount, statistics, CONTENT_INFORMATION_TOTAL_COMPONENT_COUNT, CONTENT_INFORMATION_COMPONENT_PORT);
        populateContentInformation(compositeCount, statistics, CONTENT_INFORMATION_TOTAL_COMPOSITE_COUNT, CONTENT_INFORMATION_COMPOSITE_PORT);
        populateContentInformation(widiCount, statistics, CONTENT_INFORMATION_TOTAL_WIDI_COUNT, CONTENT_INFORMATION_WIDI_PORT);
    }

    /**
     * Populate content information with "Unavailable" value for the contentCount if data is missing
     * for the given source.
     *
     * @param json source
     * @param statistics to add data to
     * @param contentCountProperty to put content count information
     * @param contentPortProperty to put port information to (port number: name)
     */
    private void populateContentInformation(JsonNode json, Map<String, String> statistics,
                                            String contentCountProperty, String contentPortProperty){
        if (!json.isMissingNode()) {
            statistics.put(contentCountProperty, String.valueOf(json.size()));
            json.forEach(jsonNode -> {
                int portNumber = jsonNode.at("/index").asInt() + 1;
                statistics.put(contentPortProperty + portNumber, jsonNode.at(TITLE_URI).asText());
            });
        } else {
            statistics.put(contentCountProperty, "Unavailable");
        }
    }

    /**
     * Retrieve data from {@link Constant#AUDIO_URI} endpoint with
     * {@link LCDCommands#getSoundSettings} and {@link LCDCommands#getSpeakerSettings} payloads
     *
     * @param statistics to keep collected properties
     * @param controls to keep controllable properties
     * @throws Exception if any error occurs
     * */
    private void generateAudioStatistics(Map<String, String> statistics, List<AdvancedControllableProperty> controls) throws Exception {
        ArrayNode soundSettings = (ArrayNode) this.doPost(AUDIO_URI, getSoundSettings, JsonNode.class).at(RESULT_0_URI);
        ArrayNode speakerSettings = (ArrayNode) this.doPost(AUDIO_URI, getSpeakerSettings, JsonNode.class).at(RESULT_0_URI);

        soundSettings.forEach(jsonNode -> {
            String name = jsonNode.at(TARGET_URI).asText();
            String value = jsonNode.at(CURRENT_VALUE_URI).asText();
            statistics.put(SPEAKER_SETTINGS_GROUP + StringUtils.capitalize(name), value);
            if (StringUtils.endsWithIgnoreCase(name, OUTPUT_TERMINAL)) {
                createDropdown(statistics, controls, SOUND_SETTINGS_LABELS, SOUND_SETTINGS_VALUES,
                        SPEAKER_SETTINGS_GROUP + StringUtils.capitalize(jsonNode.at(TARGET_URI).asText()), value);
            }
        });

        speakerSettings.forEach(jsonNode -> {
            String name = jsonNode.at(TARGET_URI).asText();
            String value = jsonNode.at(CURRENT_VALUE_URI).asText();

            String propertyName = SPEAKER_SETTINGS_GROUP + StringUtils.capitalize(name);
            if (name.equalsIgnoreCase(TV_POSITION)) {
                createDropdown(statistics, controls, SPEAKER_SETTINGS_TV_POSITION_LABELS, SPEAKER_SETTINGS_TV_POSITION_VALUES,
                        propertyName, value);
            } else if (name.equalsIgnoreCase(SUBWOOFER_LEVEL)) {
                createSlider(statistics, controls, propertyName, 0.0f, 24.0f, Float.parseFloat(value));
            } else if (name.equalsIgnoreCase(SUBWOOFER_FREQ)) {
                createSlider(statistics, controls, propertyName, 0.0f, 30.0f, Float.parseFloat(value));
            } else if (name.equalsIgnoreCase(SUBWOOFER_PHASE)) {
                createDropdown(statistics, controls, SPEAKER_SETTINGS_SUBWOOFER_PHASE_LABELS, SPEAKER_SETTINGS_SUBWOOFER_PHASE_VALUES,
                        propertyName, value);
            } else if (name.equalsIgnoreCase(SUBWOOFER_POWER)) {
                createSwitch(statistics, controls, propertyName, value.equalsIgnoreCase("true"), "On", "Off");
            }
            statistics.put(SPEAKER_SETTINGS_GROUP + StringUtils.capitalize(jsonNode.at(TARGET_URI).asText()), jsonNode.at(CURRENT_VALUE_URI).asText());
        });
    }

    /**
     * Retrieve data from {@link Constant#APP_CONTROL_URI} endpoint with
     * {@link LCDCommands#getApplicationStatusList} payload
     *
     * @param statistics to keep collected properties
     * @throws Exception if any error occurs
     * */
    private void generateApplicationStatusStatistics(Map<String, String> statistics) throws Exception {
        ArrayNode applicationStatusList = (ArrayNode) this.doPost(APP_CONTROL_URI, getApplicationStatusList, JsonNode.class).at(RESULT_0_URI);

        applicationStatusList.forEach(jsonNode -> {
            String propertyName = "ApplicationStatus#" + StringUtils.capitalize(jsonNode.at("/name").asText());
            statistics.put(propertyName, jsonNode.at(STATUS_URI).asText());
        });
    }

    /**
     * Retrieve data from {@link Constant#SYSTEM_URI} endpoint with
     * {@link LCDCommands#getNetworkSettings} payload
     * and populate it as a set of properties:
     *
     * {@value Constant#INTERFACE_NAME_PROPERTY}, {@value Constant#MAC_ADDRESS_PROPERTY}
     * {@value Constant#IPV4_ADDRESS_PROPERTY}, {@value Constant#IPV6_ADDRESS_PROPERTY}
     * {@value Constant#NETMASK_PROPERTY}, {@value Constant#GATEWAY_PROPERTY}
     * {@value Constant#DNS_PRIMARY_PROPERTY}, {@value Constant#DNS_SECONDARY_PROPERTY}
     * {@value Constant#NETWORK_GROUP} (with interface and type attached)
     *
     * @param statistics to keep collected properties
     * @throws Exception if any error occurs
     * */
    private void generateNetworkStatistics(Map<String, String> statistics) throws Exception {
        ArrayNode netInterfaces = (ArrayNode) this.doPost(SYSTEM_URI, getNetworkSettings, JsonNode.class).at(RESULT_0_URI);
        if (netInterfaces.size() == 1) {
            JsonNode netif = netInterfaces.get(0);
            statistics.put(INTERFACE_NAME_PROPERTY, netif.at(NETIF_URI).asText());
            statistics.put(MAC_ADDRESS_PROPERTY, netif.at(HW_ADDR_URI).asText());
            statistics.put(IPV4_ADDRESS_PROPERTY, netif.at(IP_ADDR_V4_URI).asText());
            statistics.put(IPV6_ADDRESS_PROPERTY, netif.at(IP_ADDR_V6_URI).asText());
            statistics.put(NETMASK_PROPERTY, netif.at(NETMASK_URI).asText());
            statistics.put(GATEWAY_PROPERTY, netif.at(GATEWAY_URI).asText());

            ArrayNode dns = (ArrayNode) netif.at(DNS_URI);
            JsonNode primaryDns = dns.get(0);
            if (primaryDns != null) {
                statistics.put(DNS_PRIMARY_PROPERTY, primaryDns.asText(""));
            }
            JsonNode secondaryDns = dns.get(1);
            if (secondaryDns != null) {
                statistics.put(DNS_SECONDARY_PROPERTY, secondaryDns.asText(""));
            }
        } else {
            for (int i = 0; i < netInterfaces.size(); i++) {
                JsonNode netif = netInterfaces.get(i);
                String ifaceName = netif.at(NETIF_URI).asText();
                statistics.put(NETWORK_GROUP + ifaceName + MAC_ADDRESS, netif.at(HW_ADDR_URI).asText());
                statistics.put(NETWORK_GROUP + ifaceName + IPV4_ADDRESS, netif.at(IP_ADDR_V4_URI).asText());
                statistics.put(NETWORK_GROUP + ifaceName + IPV6_ADDRESS, netif.at(IP_ADDR_V6_URI).asText());
                statistics.put(NETWORK_GROUP + ifaceName + NETMASK, netif.at(NETMASK_URI).asText());
                statistics.put(NETWORK_GROUP + ifaceName + GATEWAY, netif.at(GATEWAY_URI).asText());

                ArrayNode dns = (ArrayNode) netif.at(DNS_URI);
                statistics.put(NETWORK_GROUP + ifaceName + DNS_PRIMARY, dns.get(0).asText(""));
                statistics.put(NETWORK_GROUP + ifaceName + DNS_SECONDARY, dns.get(1).asText(""));
            }
        }
    }

    /**
     * Retrieve data from {@link Constant#SYSTEM_URI} endpoint with
     * {@link LCDCommands#getLEDIndicatorStatus} payload
     * and populate it as property:
     *
     * {@value Constant#LED_INDICATOR_STATE_PROPERTY}
     *
     * @param statistics to keep collected properties
     * @throws Exception if any error occurs
     * */
    private void generateLEDIndicatorStatistics(Map<String, String> statistics, List<AdvancedControllableProperty> controls) throws Exception {
        JsonNode indicatorStatus = this.doPost(SYSTEM_URI, getLEDIndicatorStatus, JsonNode.class).at(RESULT_0_URI);
        createDropdown(statistics, controls, LED_INDICATOR_MODES, LED_INDICATOR_MODES, LED_INDICATOR_MODE_PROPERTY, indicatorStatus.at(MODE_URI).asText());
        if (indicatorStatus.at(STATUS_URI).isNull()) {
            statistics.put(LED_INDICATOR_STATE_PROPERTY, "Unknown");
        } else {
            statistics.put(LED_INDICATOR_STATE_PROPERTY, indicatorStatus.at(STATUS_URI).asText());
        }
    }
    /**
     * Check for errors in device response.
     *
     * @param jsonResponse Response from a device command
     * @return String of errors, or an empty string if there are no errors found.
     */
    private String getErrors(JsonNode jsonResponse) {
        if (jsonResponse.has("error")) {
            StringBuilder errorOutput = new StringBuilder();
            ArrayNode errors = (ArrayNode) jsonResponse.at("/error");
            for (int i = 0; i < errors.size(); i += 2) {
                if (errorOutput.length() > 0)
                    errorOutput.append(", ");
                errorOutput.append("Error Code: ").append(errors.get(i).asText());
                if ((i + 1) < errors.size()) {
                    errorOutput.append("- ").append(errors.get(i + 1).asText());
                }
            }
            return errorOutput.toString();
        }
        return "";
    }

    @Override
    public void controlProperty(ControllableProperty cp) throws Exception {
        String prop = cp.getProperty();
        String value = String.valueOf(cp.getValue());

        if (prop.endsWith(VOLUME)) {
            String target = prop.substring(8, prop.length() - 7);
            this.doPost("audio", setAudioVolume.withParams(ImmutableMap.of("volume", value, target, target)));
            return;
        } else if (prop.endsWith(MUTE)) {
            this.doPost("audio", setAudioMute.withParams(ImmutableMap.of("status", value.equals("1"))));
            return;
        }

        switch (prop) {
            case SOUND_OUTPUT_TERMINAL:
                this.doPost(AUDIO_URI, setSoundSettings.withParams(ImmutableMap.of("settings",
                        ImmutableMap.of("value", value, "target", OUTPUT_TERMINAL))));
                break;
            case SPEAKER_TV_POSITION:
                this.doPost(AUDIO_URI, setSpeakerSettings.withParams(ImmutableMap.of("settings",
                        ImmutableMap.of("value", value, "target", TV_POSITION))));
                break;
            case SPEAKER_SUBWOOFER_LEVEL:
                this.doPost(AUDIO_URI, setSpeakerSettings.withParams(ImmutableMap.of("settings",
                        ImmutableMap.of("value", value, "target", SUBWOOFER_LEVEL))));
                break;
            case SPEAKER_SUBWOOFER_FREQ:
                this.doPost(AUDIO_URI, setSpeakerSettings.withParams(ImmutableMap.of("settings",
                        ImmutableMap.of("value", value, "target", SUBWOOFER_FREQ))));
                break;
            case SPEAKER_SUBWOOFER_PHASE:
                this.doPost(AUDIO_URI, setSpeakerSettings.withParams(ImmutableMap.of("settings",
                        ImmutableMap.of("value", value, "target", SUBWOOFER_PHASE))));
                break;
            case SPEAKER_SUBWOOFER_POWER:
                this.doPost(AUDIO_URI, setSpeakerSettings.withParams(ImmutableMap.of("settings",
                        ImmutableMap.of("value", "0".equals(value) ? "off" : "on", "target", SUBWOOFER_POWER))));
                break;
            case REBOOT_PROPERTY:
                this.doPost(SYSTEM_URI, requestReboot);
                break;
            case TERMINATE_APPS_PROPERTY:
                this.doPost(APP_CONTROL_URI, terminateApps);
                break;
            case LED_INDICATOR_MODE_PROPERTY:
                this.doPost(SYSTEM_URI, setLEDIndicatorStatus.withParams(
                        ImmutableMap.of("mode", value, "status", null)));
                break;
            case LAUNCH_APPLICATION_PROPERTY:
                if (!value.equals("")) {
                    this.doPost(APP_CONTROL_URI, setActiveApp.withParams(
                            ImmutableMap.of("uri", value)));
                }
                break;
            case POWER_PROPERTY:
                this.doPost(SYSTEM_URI, setPowerStatus.withParams(ImmutableMap.of("status", value.equals("1"))));
                break;
            case INPUT_PROPERTY:
                this.doPost(AV_CONTENT_URI, setPlayContent.withParams(ImmutableMap.of("uri", value)));
                break;
            case POWER_SAVE_MODE_PROPERTY:
                this.doPost(SYSTEM_URI, setPowerSavingMode.withParams(ImmutableMap.of("mode", value)));
                break;
            default:
                if (this.logger.isWarnEnabled())
                    this.logger.warn("Control property \"" + prop + "\" is invalid");
                throw new UnsupportedOperationException("Control property \"" + prop + "\" is invalid");
        }
    }

    @Override
    public void controlProperties(List<ControllableProperty> list) throws Exception {
        for (ControllableProperty cp : list)
            controlProperty(cp);
    }

    @Override
    protected HttpHeaders putExtraRequestHeaders(HttpMethod httpMethod, String uri, HttpHeaders headers) {
        headers.add("X-Auth-PSK", this.getPassword());
        return headers;
    }

    /**
     * Tidy String for neat formatting on the Symphony portal by converting string to camelcase
     *
     * @param string Input String
     * @return String converted to CamelCase
     */
    private static String tidyString(String string) {
        StringBuilder sb = new StringBuilder();
        for (String s : string.split(" ")) {
            sb.append(s.charAt(0)).append(s.substring(1).toLowerCase());
        }
        return sb.toString().trim();
    }

    /**
     * Creates an Advanced Control Switch and adds corresponding values to stats and controls
     *
     * @param stats    Extended Statistics Map
     * @param controls Advanced Controls List
     * @param name     Name for the control property
     * @param state    current state of the switch
     * @param onLabel  Switch on label
     * @param offLabel Switch off label
     */
    private static void createSwitch(Map<String, String> stats, List<AdvancedControllableProperty> controls, String name, boolean state, String onLabel, String offLabel) {
        AdvancedControllableProperty.Switch toggle = new AdvancedControllableProperty.Switch();
        toggle.setLabelOn(onLabel);
        toggle.setLabelOff(offLabel);
        stats.put(name, state ? "1" : "0");
        controls.add(new AdvancedControllableProperty(name, new Date(), toggle, state ? "1" : "0"));
    }

    /**
     * Creates an Advanced Control Dropdown and adds corresponding values to stats and controls
     *
     * @param stats    Extended Statistics Map
     * @param controls Advanced Controls List
     * @param labels   Array of display labels for the dropdown
     * @param options  Array of options for the dropdown
     * @param name     Name for the control property
     * @param state    current selected option
     */
    private static void createDropdown(Map<String, String> stats, List<AdvancedControllableProperty> controls, String[] labels, String[] options, String name, String state) {
        AdvancedControllableProperty.DropDown dropdown = new AdvancedControllableProperty.DropDown();
        dropdown.setLabels(labels);
        dropdown.setOptions(options);
        stats.put(name, state);
        controls.add(new AdvancedControllableProperty(name, new Date(), dropdown, state));
    }

    /**
     * Creates an Advanced Control Slider and adds corresponding values to stats and controls
     *
     * @param stats        Extended Statistics Map
     * @param controls     Advanced Controls List
     * @param name         Name for the control property
     * @param label        Label for the button
     * @param pressedLabel Pressed label for the button
     * @param gracePeriod  Grace period between button presses
     */
    private static void createButton(Map<String, String> stats, List<AdvancedControllableProperty> controls, String name, String label, String pressedLabel, long gracePeriod) {
        AdvancedControllableProperty.Button button = new AdvancedControllableProperty.Button();
        button.setLabel(label);
        button.setLabelPressed(pressedLabel);
        button.setGracePeriod(gracePeriod);
        stats.put(name, "0");
        controls.add(new AdvancedControllableProperty(name, new Date(), button, "0"));
    }

    /**
     * Creates an Advanced Control Slider and adds corresponding values to stats and controls
     *
     * @param stats        Extended Statistics Map
     * @param controls     Advanced Controls List
     * @param name         Name for the control property
     * @param minValue     minimum value for the slider
     * @param maxValue     maximum value for the slider
     * @param currentValue current value of the slider
     */
    private static void createSlider(Map<String, String> stats, List<AdvancedControllableProperty> controls, String name, float minValue, float maxValue, float currentValue) {
        AdvancedControllableProperty.Slider slider = new AdvancedControllableProperty.Slider();
        slider.setLabelStart("0%");
        slider.setLabelEnd("100%");
        slider.setRangeStart(minValue);
        slider.setRangeEnd(maxValue);
        stats.put(name, String.valueOf(currentValue));
        controls.add(new AdvancedControllableProperty(name, new Date(), slider, currentValue));
    }
}
