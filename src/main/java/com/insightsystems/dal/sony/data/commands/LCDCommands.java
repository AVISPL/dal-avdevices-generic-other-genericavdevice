/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.insightsystems.dal.sony.data.commands;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.insightsystems.dal.sony.data.SystemRequest;

import java.util.Iterator;
import java.util.List;

import static com.insightsystems.dal.sony.data.Constant.*;

/**
 * Set of prepared commands to perform on Sony Bravia device in order to
 * retrieve statistics or change device's settings
 *
 * @since 1.0
 * @author Maksym.Rossiytsev
 * */
public class LCDCommands {
    public static final SystemRequest interfaceInformation = new SystemRequest("1.0", GET_INTERFACE_INFORMATION, 33);
    public static final SystemRequest getSystemInformation = new SystemRequest("1.0", GET_SYSTEM_INFORMATION, 33);
    public static final SystemRequest getCurrentExternalInputsStatus = new SystemRequest("1.1", GET_CURRENT_EXTERNAL_INPUTS_STATUS, 105);
    public static final SystemRequest getPlayingContentInfo = new SystemRequest("1.0", GET_PLAYING_CONTENT_INFO, 103);
    public static final SystemRequest getVolumeInformation = new SystemRequest("1.0", GET_VOLUME_INFORMATION, 33);
    public static final SystemRequest getPowerStatus = new SystemRequest("1.0", GET_POWER_STATUS, 50);
    public static final SystemRequest getApplicationList = new SystemRequest("1.0", GET_APPLICATION_LIST, 60);
    public static final SystemRequest getNetworkSettings = new SystemRequest("1.0", GET_NETWORK_SETTINGS, 2)
            .withParams(ImmutableMap.of("netif", ""));
    public static final SystemRequest getLEDIndicatorStatus = new SystemRequest("1.0", GET_LED_INDICATOR_STATUS, 45);
    public static final SystemRequest getCurrentTime = new SystemRequest("1.1", GET_CURRENT_TIME, 51);
    public static final SystemRequest getPowerSavingMode = new SystemRequest("1.0", GET_POWER_SAVING_MODE, 51);
    public static final SystemRequest requestReboot = new SystemRequest("1.0", REQUEST_REBOOT, 10);
    public static final SystemRequest terminateApps = new SystemRequest("1.0", TERMINATE_APPS, 55);
    public static final SystemRequest getApplicationStatusList = new SystemRequest("1.0", GET_APPLICATION_STATUS_LIST, 55);
    public static final SystemRequest getSoundSettings = new SystemRequest("1.1", GET_SOUND_SETTINGS, 73)
            .withParams(ImmutableMap.of("target", ""));
    public static final SystemRequest setSoundSettings = new SystemRequest("1.1", SET_SOUND_SETTINGS, 5);
    public static final SystemRequest getSpeakerSettings = new SystemRequest("1.0", GET_SPEAKER_SETTINGS, 67)
            .withParams(ImmutableMap.of("target", ""));
    public static final SystemRequest setSpeakerSettings = new SystemRequest("1.0", SET_SPEAKER_SETTINGS, 62);
    public static final SystemRequest getContentCount = new SystemRequest("1.1", GET_CONTENT_COUNT, 11);
    public static final SystemRequest getContentList = new SystemRequest("1.5", GET_CONTENT_LIST, 88);
    public static final SystemRequest setLEDIndicatorStatus = new SystemRequest("1.1", SET_LED_INDICATOR_STATUS, 53);
    public static final SystemRequest setActiveApp = new SystemRequest("1.0", SET_ACTIVE_APP, 601);
    public static final SystemRequest setPowerStatus = new SystemRequest("1.0", SET_POWER_STATUS, 55);
    public static final SystemRequest setPlayContent = new SystemRequest("1.0", SET_PLAY_CONTENT, 101);
    public static final SystemRequest setPowerSavingMode = new SystemRequest("1.0", SET_POWER_SAVING_MODE, 52);
    public static final SystemRequest setAudioMute = new SystemRequest("1.0", SET_AUDIO_MUTE, 601);
    public static final SystemRequest setAudioVolume = new SystemRequest("1.0", SET_AUDIO_VOLUME, 601);

    /**
     * Generate a multimap of values based on 2 lists -> keys and values.
     * The lists must be the same length, otherwise an exception will be thrown
     *
     * @param keys list of values to be used as keys of multimap
     * @param values list of values to be used as values of multimap
     * @return {@link Multimap} storing all collected values, provided by params
     */
    public static Multimap<String, Object> generateRequestParameters(List<String> keys, List<Object> values) {
        if (keys.size() != values.size()) {
            throw new IllegalArgumentException("Keys list size does not match values list size.");
        }
        ListMultimap<String, Object> parameters = ArrayListMultimap.create();
        Iterator<String> keysIterator = keys.iterator();
        Iterator<Object> valuesIterator = values.iterator();

        while (keysIterator.hasNext() && valuesIterator.hasNext()) {
            parameters.put(keysIterator.next(), valuesIterator.next());
        }
        return parameters;
    }
}
