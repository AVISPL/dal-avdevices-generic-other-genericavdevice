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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.*;

/**
 * <h1>Sony LCD Device Adapter</h1>
 * Company: Insight Systems
 * @author Jayden Loone (JaydenL-Insight)
 * @version 0.3
 * @implSpec Device must support REST API and have PSK authentication enabled. PSK should be entered as device HTTP password. Login username field is unused
 * @see <a href="https://pro-bravia.sony.net/develop/integrate/rest-api/spec/">Sony Rest API Spec</a>
 */
public class Lcd extends RestCommunicator implements Controller, Monitorable {
    private static final String[] powerSaveModes = new String[]{"off","low","high","pictureOff"};
    private static final String[] ledIndicatorModes = new String[]{"Demo","AutoBrightnessAdjust","Dark","SimpleResponse","Off"};

    public Lcd(){
        this.setBaseUri("sony");
        this.setContentType("application/json");
    }

    @Override
    protected void authenticate(){}

    @Override
    public List<Statistics> getMultipleStatistics() throws Exception {
        ExtendedStatistics extStats = new ExtendedStatistics();
        Map<String,String> stats = new HashMap<>();
        List<AdvancedControllableProperty> controls = new ArrayList<>();

        //Device Info
        JsonNode devInfo = this.doPost("system","{\"method\": \"getInterfaceInformation\",\"id\": 33,\"params\": [],\"version\": \"1.0\"}",JsonNode.class).at("/result/0");
        stats.put("model",devInfo.at("/modelName").asText());
        stats.put("Device#ProductName",devInfo.at("/productName").asText());
        stats.put("Device#ProductCategory",devInfo.at("/productCategory").asText());
        stats.put("Device#InterfaceVersion",devInfo.at("/interfaceVersion").asText());

        JsonNode sysInfo = this.doPost("system","{\"method\": \"getSystemInformation\",\"id\": 33,\"params\": [],\"version\": \"1.0\"}",JsonNode.class).at("/result/0");
        stats.put("Device#serialNumber",sysInfo.at("/serial").asText());

        //Device Inputs
        ArrayNode inputs =  (ArrayNode) this.doPost("avContent","{\"method\": \"getCurrentExternalInputsStatus\",\"id\": 105,\"params\": [],\"version\": \"1.1\"}",JsonNode.class).at("/result/0");
        String[] inputLabels = new String[inputs.size()];
        String[] inputOptions = new String[inputs.size()];
        for (int i=0;i < inputs.size();i++){
            JsonNode input =  inputs.get(i);
            inputLabels[i] = tidyString(input.at("/title").asText());
            inputOptions[i] = input.at("/uri").asText();
            stats.put("Signal Presence#"+inputLabels[i],input.at("/connection").asBoolean() ? "Connected" : "Not connected");
        }

        String currentInput = this.doPost("avContent","{\"method\": \"getPlayingContentInfo\",\"id\": 103,\"params\": [],\"version\": \"1.0\"}",JsonNode.class).at("/result/0/uri").asText();
        createDropdown(stats,controls, inputLabels, inputOptions,"Controls#Input",currentInput);

        //Volume Information
        JsonNode audioResponse = this.doPost("audio","{\"method\": \"getVolumeInformation\",\"id\": 33,\"params\": [],\"version\": \"1.0\"}",JsonNode.class);
        String audioErrors = getErrors(audioResponse);
        if (audioErrors.equals("")) {
            ArrayNode audioOuts = (ArrayNode) audioResponse.at("/result/0");
            for (int i = 0; i < audioOuts.size(); i++) {
                JsonNode out = audioOuts.get(i);
                String name = "Control#" + out.at("/target");
                createSlider(stats, controls, name + "Volume", (float) out.at("/minVolume").asDouble(), (float) out.at("/macVolume").asDouble(), (float) out.at("/volume").asDouble());
                createSwitch(stats, controls, name + "Mute", out.at("/mute").asBoolean(), "On", "Off");
            }
        } else {
            if (this.logger.isTraceEnabled())
                this.logger.trace(audioErrors);
        }
        //Power State
        String powerState = this.doPost("system","{\"method\": \"getPowerStatus\",\"id\": 50,\"params\": [],\"version\": \"1.0\"}",JsonNode.class).at("/result/0/status").asText();
        createSwitch(stats,controls,"Controls#Power",powerState.equalsIgnoreCase("active"),"On","Off");

        //Available applications
        ArrayNode apps = (ArrayNode) this.doPost("appControl","{\"method\": \"getApplicationList\",\"id\": 60,\"params\": [],\"version\": \"1.0\"}",JsonNode.class).at("/result/0");
        String[] appLabels = new String[apps.size()+1];
        String[] appOptions = new String[apps.size()+1];

        appLabels[apps.size()] = "-Select Application-";
        appOptions[apps.size()] = "";
        for (int i=0;i < apps.size();i++){
            JsonNode app = apps.get(i);
            appLabels[i] = app.at("/title").asText();
            appOptions[i] = app.at("/uri").asText();
        }
        createDropdown(stats,controls,appLabels,appOptions,"Controls#Launch Application","");

        //Network Information
        ArrayNode netInterfaces = (ArrayNode) this.doPost("/system","{\"method\": \"getNetworkSettings\",\"id\": 2,\"params\": [{\"netif\": \"\"}],\"version\": \"1.0\"}",JsonNode.class).at("/result/0");
        if (netInterfaces.size() == 1){
            JsonNode netif = netInterfaces.get(0);
            stats.put("Network#InterfaceName",netif.at("/netif").asText());
            stats.put("Network#Mac Address",netif.at("/hwAddr").asText());
            stats.put("Network#Ipv4 Address",netif.at("/ipAddrV4").asText());
            stats.put("Network#Ipv6 Address",netif.at("/ipAddrV6").asText());
            stats.put("Network#Netmask",netif.at("/netmask").asText());
            stats.put("Network#Gateway",netif.at("/gateway").asText());

            ArrayNode dns = (ArrayNode) netif.at("/dns");
            stats.put("Network#DnsPrimary",dns.get(0).asText(""));
            stats.put("Network#DnsSecondary",dns.get(1).asText(""));
        } else{
            for (int i=0;i<netInterfaces.size();i++){
                JsonNode netif = netInterfaces.get(i);
                String ifaceName = netif.at("/netif").asText();
                stats.put("Network#"+ifaceName+" Mac Address",netif.at("/hwAddr").asText());
                stats.put("Network#"+ifaceName+" Ipv4 Address",netif.at("/ipAddrV4").asText());
                stats.put("Network#"+ifaceName+" Ipv6 Address",netif.at("/ipAddrV6").asText());
                stats.put("Network#"+ifaceName+" Netmask",netif.at("/netmask").asText());
                stats.put("Network#"+ifaceName+" Gateway",netif.at("/gateway").asText());

                ArrayNode dns = (ArrayNode) netif.at("/dns");
                stats.put("Network#"+ifaceName+" DnsPrimary",dns.get(0).asText(""));
                stats.put("Network#"+ifaceName+" DnsSecondary",dns.get(1).asText(""));
            }
        }

        //Led Indicator
        JsonNode indicatorStatus = this.doPost("system","{\"method\": \"getLEDIndicatorStatus\",\"id\": 45,\"params\": [],\"version\": \"1.0\"}",JsonNode.class).at("/result/0");
        createDropdown(stats,controls,ledIndicatorModes,ledIndicatorModes,"Device#LedIndicatorMode",indicatorStatus.at("/mode").asText());
        if (indicatorStatus.at("/status").isNull())
            stats.put("Device#LedIndicatorState","Unknown");
        else
            stats.put("Device#LedIndicatorState",indicatorStatus.at("/status").asText());

        //System DateTime
        stats.put("Device#dateTime",this.doPost("system","{\"method\": \"getCurrentTime\",\"id\": 51,\"params\": [],\"version\": \"1.1\"}",JsonNode.class).at("/result/0/dateTime").asText());

        //Power Save Modes
        JsonNode powerSave = this.doPost("system","{\"method\": \"getPowerSavingMode\",\"id\": 51,\"params\": [],\"version\": \"1.0\"}",JsonNode.class);
        createDropdown(stats,controls,powerSaveModes,powerSaveModes,"Controls#PowerSaveMode",powerSave.at("/result/0/mode").asText());

        //Stateless Control Buttons
        createButton(stats,controls,"Controls#Reboot","Reboot","Rebooting",10_000L);
        createButton(stats,controls,"Controls#TerminateApps","Kill Apps","Killing",1_000L);

        extStats.setStatistics(stats);
        extStats.setControllableProperties(controls);
        return Collections.singletonList(extStats);
    }

    /**
     * Check for errors in device response.
     * @param jsonResponse Response from a device command
     * @return String of errors, or an empty string if there are no errors found.
     */
    private String getErrors(JsonNode jsonResponse) {
        if (jsonResponse.has("error")){
            StringBuilder errorOutput = new StringBuilder();
          ArrayNode errors = (ArrayNode) jsonResponse.at("/error");
          for (int i = 0;i < errors.size(); i += 2){
              if (errorOutput.length() >0)
                  errorOutput.append(", ");
              errorOutput.append("Error Code: ").append(errors.get(i).asText());
              if ((i+1) < errors.size()){
                  errorOutput.append("- ").append(errors.get(i+1).asText());
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

        if (prop.equals("Controls#Reboot")){
            this.doPost("system","{\"method\": \"requestReboot\",\"id\": 10,\"params\": [],\"version\": \"1.0\"}");
        } else if (prop.equals("Controls#TerminateApps")){
            this.doPost("appControl","{\"method\": \"terminateApps\",\"id\": 55,\"params\": [],\"version\": \"1.0\"}");
        } else if (prop.equals("Device#LedIndicatorMode")){
            this.doPost("system","{\"method\": \"setLEDIndicatorStatus\",\"id\": 53,\"params\": [{\"mode\": \""+value+"\",\"status\": null}],\"version\": \"1.1\"}");
        } else if (prop.equals("Controls#Launch Application")){
            if (!value.equals("")){
                this.doPost("appControl","{\"method\": \"setActiveApp\",\"id\": 601,\"params\": [{\"uri\": \""+value+"\"}],\"version\": \"1.0\"}");
            }
        } else if (prop.equals("Controls#Power")){
            this.doPost("system","{\"method\": \"setPowerStatus\",\"id\": 55,\"params\": [{\"status\": "+value.equals("1")+"}],\"version\": \"1.0\"}");
        } else if (prop.endsWith("Volume")){ // Control#<target>Volume
            String target = prop.substring(8,prop.length()-7);
            this.doPost("audio","{\"method\": \"setAudioVolume\",\"id\": 601,\"params\": [{\"volume\": \""+value+"\",\"target\": \""+target+"\"}],\"version\": \"1.0\"}");
        } else if (prop.endsWith("Mute")){
            this.doPost("audio","{\"method\": \"setAudioMute\",\"id\": 601,\"params\": [{\"status\": "+value.equals("1")+"}],\"version\": \"1.0\"}");
        } else if (prop.equals("Controls#Input")){
            this.doPost("avContent","{\"method\": \"setPlayContent\",\"id\": 101,\"params\": [{\"uri\": \""+value+"\"}],\"version\": \"1.0\"}");
        } else if (prop.equals("Controls#PowerSaveMode")){
            this.doPost("system","{\"method\": \"setPowerSavingMode\",\"id\": 52,\"params\": [{\"mode\": \""+value+"\"}],\"version\": \"1.0\"}");
        } else {
            if (this.logger.isWarnEnabled())
                this.logger.warn("Control property \""+prop+"\" is invalid");
            throw new UnsupportedOperationException("Control property \""+prop+"\" is invalid");
        }
    }

    @Override
    public void controlProperties(List<ControllableProperty> list) throws Exception {
        for (ControllableProperty cp : list)
            controlProperty(cp);
    }

    @Override
    protected HttpHeaders putExtraRequestHeaders(HttpMethod httpMethod, String uri, HttpHeaders headers){
        headers.add("X-Auth-PSK",this.getPassword());
        return headers;
    }

    /**
     * Tidy String for neat formatting on the Symphony portal by converting string to camelcase
     * @param string Input String
     * @return String converted to CamelCase
     */
    private static String tidyString(String string){
        StringBuilder sb = new StringBuilder();
        for (String s : string.split(" ")) {
            sb.append(s.charAt(0)).append(s.substring(1).toLowerCase());
        }
        return sb.toString().trim();
    }

    /**
     * Creates an Advanced Control Switch and adds corresponding values to stats and controls
     * @param stats Extended Statistics Map
     * @param controls Advanced Controls List
     * @param name Name for the control property
     * @param state current state of the switch
     * @param onLabel Switch on label
     * @param offLabel Switch off label
     */
    private static void createSwitch(Map<String, String> stats, List<AdvancedControllableProperty> controls, String name, boolean state, String onLabel, String offLabel) {
        AdvancedControllableProperty.Switch toggle = new AdvancedControllableProperty.Switch();
        toggle.setLabelOn(onLabel);
        toggle.setLabelOff(offLabel);
        stats.put(name,state?"1":"0");
        controls.add(new AdvancedControllableProperty(name,new Date(),toggle,state?"1":"0"));
    }

    /**
     * Creates an Advanced Control Dropdown and adds corresponding values to stats and controls
     * @param stats Extended Statistics Map
     * @param controls Advanced Controls List
     * @param labels Array of display labels for the dropdown
     * @param options Array of options for the dropdown
     * @param name Name for the control property
     * @param state current selected option
     */
    private static void createDropdown(Map<String, String> stats, List<AdvancedControllableProperty> controls, String[] labels, String[] options,String name, String state) {
        AdvancedControllableProperty.DropDown dropdown = new AdvancedControllableProperty.DropDown();
        dropdown.setLabels(labels);
        dropdown.setOptions(options);
        stats.put(name,state);
        controls.add(new AdvancedControllableProperty(name,new Date(),dropdown,state));
    }

    /**
     * Creates an Advanced Control Slider and adds corresponding values to stats and controls
     * @param stats Extended Statistics Map
     * @param controls Advanced Controls List
     * @param name Name for the control property
     * @param label Label for the button
     * @param pressedLabel Pressed label for the button
     * @param gracePeriod Grace period between button presses
     */
    private static void createButton(Map<String, String> stats, List<AdvancedControllableProperty> controls,String name,String label,String pressedLabel, long gracePeriod){
        AdvancedControllableProperty.Button button = new AdvancedControllableProperty.Button();
        button.setLabel(label);
        button.setLabelPressed(pressedLabel);
        button.setGracePeriod(gracePeriod);
        stats.put(name,"0");
        controls.add(new AdvancedControllableProperty(name,new Date(),button,"0"));
    }

    /**
     * Creates an Advanced Control Slider and adds corresponding values to stats and controls
     * @param stats Extended Statistics Map
     * @param controls Advanced Controls List
     * @param name Name for the control property
     * @param minValue minimum value for the slider
     * @param maxValue maximum value for the slider
     * @param currentValue current value of the slider
     */
    private static void createSlider(Map<String, String> stats, List<AdvancedControllableProperty> controls,String name,float minValue,float maxValue,float currentValue){
        AdvancedControllableProperty.Slider slider = new AdvancedControllableProperty.Slider();
        slider.setLabelStart("0%");
        slider.setLabelEnd("100%");
        slider.setRangeStart(minValue);
        slider.setRangeEnd(maxValue);
        stats.put(name,currentValue+"");
        controls.add(new AdvancedControllableProperty(name,new Date(),slider,currentValue));
    }
}
