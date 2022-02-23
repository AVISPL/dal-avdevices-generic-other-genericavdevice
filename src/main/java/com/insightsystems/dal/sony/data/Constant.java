/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.insightsystems.dal.sony.data;

/**
 * Adapter constants. Includes property names, groups, rcp actions, json URIs
 *
 * @since 1.0
 * @author Maksym.Rossiytsev
 * */
public class Constant {
    // Property names
    public static final String MODEL_PROPERTY = "Model";
    public static final String PRODUCT_NAME_PROPERTY = "Device#ProductName";
    public static final String PRODUCT_CATEGORY_PROPERTY = "Device#ProductCategory";
    public static final String INTERFACE_VERSION_PROPERTY = "Device#InterfaceVersion";
    public static final String SERIAL_NUMBER_PROPERTY = "Device#SerialNumber";
    public static final String POWER_PROPERTY = "Controls#Power";
    public static final String LAUNCH_APPLICATION_PROPERTY = "Controls#LaunchApplication";
    public static final String INTERFACE_NAME_PROPERTY = "Network#InterfaceName";
    public static final String MAC_ADDRESS_PROPERTY = "Network#MacAddress";
    public static final String IPV4_ADDRESS_PROPERTY = "Network#Ipv4Address";
    public static final String IPV6_ADDRESS_PROPERTY = "Network#Ipv6Address";
    public static final String NETMASK_PROPERTY = "Network#Netmask";
    public static final String GATEWAY_PROPERTY = "Network#Gateway";
    public static final String DNS_PRIMARY_PROPERTY = "Network#DnsPrimary";
    public static final String DNS_SECONDARY_PROPERTY = "Network#DnsSecondary";
    public static final String LED_INDICATOR_MODE_PROPERTY = "Device#LedIndicatorMode";
    public static final String LED_INDICATOR_STATE_PROPERTY = "Device#LedIndicatorState";
    public static final String DATE_TIME_PROPERTY = "Device#DateTime";
    public static final String POWER_SAVE_MODE_PROPERTY = "Controls#PowerSaveMode";
    public static final String REBOOT_PROPERTY = "Controls#Reboot";
    public static final String TERMINATE_APPS_PROPERTY = "Controls#TerminateApps";
    public static final String INPUT_PROPERTY = "Controls#Input";
    public static final String SOUND_OUTPUT_TERMINAL = "SoundSettings#OutputTerminal";
    public static final String SPEAKER_TV_POSITION = "SpeakerSettings#TvPosition";
    public static final String SPEAKER_SUBWOOFER_LEVEL = "SpeakerSettings#SubwooferLevel";
    public static final String SPEAKER_SUBWOOFER_FREQ = "SpeakerSettings#SubwooferFreq";
    public static final String SPEAKER_SUBWOOFER_PHASE = "SpeakerSettings#SubwooferPhase";
    public static final String SPEAKER_SUBWOOFER_POWER = "SpeakerSettings#SubwooferPower";

    public static final String SIGNAL_PRESENCE_GROUP = "SignalPresence#";
    public static final String CONTROL_GROUP = "Control#";
    public static final String NETWORK_GROUP = "Network#";

    public static final String VOLUME = "Volume";
    public static final String MUTE = "Mute";

    // RCP Actions
    public static final String SET_AUDIO_VOLUME = "setAudioVolume";
    public static final String SET_AUDIO_MUTE = "setAudioMute";
    public static final String SET_LED_INDICATOR_STATUS = "setLEDIndicatorStatus";
    public static final String SET_ACTIVE_APP = "setActiveApp";
    public static final String SET_POWER_STATUS = "setPowerStatus";
    public static final String SET_PLAY_CONTENT = "setPlayContent";
    public static final String SET_POWER_SAVING_MODE = "setPowerSavingMode";
    public static final String GET_INTERFACE_INFORMATION = "getInterfaceInformation";
    public static final String GET_SYSTEM_INFORMATION = "getSystemInformation";
    public static final String GET_CURRENT_EXTERNAL_INPUTS_STATUS = "getCurrentExternalInputsStatus";
    public static final String GET_PLAYING_CONTENT_INFO = "getPlayingContentInfo";
    public static final String GET_VOLUME_INFORMATION = "getVolumeInformation";
    public static final String GET_POWER_STATUS = "getPowerStatus";
    public static final String GET_APPLICATION_LIST = "getApplicationList";
    public static final String GET_NETWORK_SETTINGS = "getNetworkSettings";
    public static final String GET_LED_INDICATOR_STATUS = "getLEDIndicatorStatus";
    public static final String GET_CURRENT_TIME = "getCurrentTime";
    public static final String GET_POWER_SAVING_MODE = "getPowerSavingMode";
    public static final String REQUEST_REBOOT = "requestReboot";
    public static final String TERMINATE_APPS = "terminateApps";
    public static final String GET_APPLICATION_STATUS_LIST = "getApplicationStatusList";
    public static final String GET_SOUND_SETTINGS = "getSoundSettings";
    public static final String SET_SOUND_SETTINGS = "setSoundSettings";
    public static final String GET_SPEAKER_SETTINGS = "getSpeakerSettings";
    public static final String SET_SPEAKER_SETTINGS = "setSpeakerSettings";
    public static final String GET_CONTENT_COUNT = "getContentCount";
    public static final String GET_CONTENT_LIST = "getContentList";

    // URIs
    public static final String SYSTEM_URI = "system";
    public static final String AV_CONTENT_URI = "avContent";
    public static final String AUDIO_URI = "audio";
    public static final String APP_CONTROL_URI = "appControl";

    // Json URIs
    public static final String DATE_TIME_URI = "/result/0/dateTime";
    public static final String MODE_0_URI = "/result/0/mode";
    public static final String MODE_URI = "/mode";
    public static final String STATUS_URI = "/status";
    public static final String STATUS_0_URI = "/result/0/status";
    public static final String RESULT_0_URI = "/result/0";
    public static final String RESULT_0_URI_URI = "/result/0/uri";
    public static final String MODEL_NAME_URI = "/modelName";
    public static final String PRODUCT_NAME_URI = "/productName";
    public static final String PRODUCT_CATEGORY_URI = "/productCategory";
    public static final String INTERFACE_VERSION_URI = "/interfaceVersion";
    public static final String SERIAL_URI = "/serial";
    public static final String TITLE_URI = "/title";
    public static final String TARGET_URI = "/target";
    public static final String MIN_VOLUME_URI = "/minVolume";
    public static final String MAX_VOLUME_URI = "/maxVolume";
    public static final String VOLUME_URI = "/volume";
    public static final String MUTE_URI = "/mute";
    public static final String URI_URI = "/uri";
    public static final String CONNECTION_URI = "/connection";
    public static final String NETIF_URI = "/netif";
    public static final String HW_ADDR_URI = "/hwAddr";
    public static final String IP_ADDR_V4_URI = "/ipAddrV4";
    public static final String IP_ADDR_V6_URI = "/ipAddrV6";
    public static final String NETMASK_URI = "/netmask";
    public static final String GATEWAY_URI = "/gateway";
    public static final String DNS_URI = "/dns";
}
