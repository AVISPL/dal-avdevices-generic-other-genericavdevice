package com.insightsystems.dal.sony;

import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class SonyBraviaCommunicatorTest {
    SonyBraviaCommunicator lcd;

    @Before
    public void setup() throws Exception {
        lcd = new SonyBraviaCommunicator();
        lcd.setHost("192.168.254.171");
        lcd.setPort(80);
        lcd.setProtocol("http");
        lcd.setPassword("1234");
        lcd.init();
    }

    @Test
    public void validateAllStatisticsDeviceStandby() throws Exception {
        Map<String,String> stats = ((ExtendedStatistics)lcd.getMultipleStatistics().get(0)).getStatistics();

        Assert.assertNotNull(stats.get("Controls#Input"));
        Assert.assertNotNull(stats.get("Controls#Power"));
        Assert.assertNotNull(stats.get("Controls#PowerSaveMode"));
        Assert.assertNotNull(stats.get("Controls#Reboot"));
        Assert.assertNotNull(stats.get("Controls#TerminateApps"));
        Assert.assertNotNull(stats.get("Device#DateTime"));
        Assert.assertNotNull(stats.get("Device#InterfaceVersion"));
        Assert.assertNotNull(stats.get("Device#LedIndicatorMode"));
        Assert.assertNotNull(stats.get("Device#LedIndicatorState"));
        Assert.assertNotNull(stats.get("Device#ProductName"));
        Assert.assertNotNull(stats.get("Device#SerialNumber"));
        Assert.assertNotNull(stats.get("Network#DnsPrimary"));
//        Assert.assertNotNull(stats.get("Network#DnsSecondary"));
        Assert.assertNotNull(stats.get("Network#Gateway"));
        Assert.assertNotNull(stats.get("Network#InterfaceName"));
        Assert.assertNotNull(stats.get("Network#Ipv4Address"));
        Assert.assertNotNull(stats.get("Network#Ipv6Address"));
        Assert.assertNotNull(stats.get("Network#Netmask"));
        Assert.assertNotNull(stats.get("SignalPresence#Hdmi1"));
        Assert.assertNotNull(stats.get("SignalPresence#Hdmi2"));
//        Assert.assertNotNull(stats.get("SignalPresence#Hdmi3/arc"));
//        Assert.assertNotNull(stats.get("SignalPresence#Hdmi4"));
//        Assert.assertNotNull(stats.get("SignalPresence#Screenmirroring"));
//        Assert.assertNotNull(stats.get("SignalPresence#Video1"));
//        Assert.assertNotNull(stats.get("SignalPresence#Video2/component"));
    }

    @Test
    public void validateAllStatisticsDeviceNonStandby() throws Exception {
        Map<String,String> stats = ((ExtendedStatistics)lcd.getMultipleStatistics().get(0)).getStatistics();
        Assert.assertNotNull(stats.get("Controls#Input"));
        Assert.assertNotNull(stats.get("Controls#Power"));
        Assert.assertNotNull(stats.get("Controls#PowerSaveMode"));
        Assert.assertNotNull(stats.get("Controls#Reboot"));
        Assert.assertNotNull(stats.get("Controls#TerminateApps"));
        Assert.assertNotNull(stats.get("Device#DateTime"));
        Assert.assertNotNull(stats.get("Device#InterfaceVersion"));
        Assert.assertNotNull(stats.get("Device#LedIndicatorMode"));
        Assert.assertNotNull(stats.get("Device#LedIndicatorState"));
        Assert.assertNotNull(stats.get("Device#ProductName"));
        Assert.assertNotNull(stats.get("Device#serialNumber"));
        Assert.assertNotNull(stats.get("Network#DnsPrimary"));
        Assert.assertNotNull(stats.get("Network#DnsSecondary"));
        Assert.assertNotNull(stats.get("Network#Gateway"));
        Assert.assertNotNull(stats.get("Network#InterfaceName"));
        Assert.assertNotNull(stats.get("Network#Ipv4 Address"));
        Assert.assertNotNull(stats.get("Network#Ipv6 Address"));
        Assert.assertNotNull(stats.get("Network#Netmask"));
        Assert.assertNotNull(stats.get("Signal Presence#Hdmi1"));
        Assert.assertNotNull(stats.get("Signal Presence#Hdmi2"));
        Assert.assertNotNull(stats.get("Signal Presence#Hdmi3/arc"));
        Assert.assertNotNull(stats.get("Signal Presence#Hdmi4"));
        Assert.assertNotNull(stats.get("Signal Presence#Screenmirroring"));
        Assert.assertNotNull(stats.get("Signal Presence#Video1"));
        Assert.assertNotNull(stats.get("Signal Presence#Video2/component"));
    }

    @Test
    public void testPower() throws Exception {
        ControllableProperty property = new ControllableProperty();
        property.setProperty("Controls#Power");
        property.setValue(1);
        lcd.controlProperty(property);
        Map<String,String> stats = ((ExtendedStatistics)lcd.getMultipleStatistics().get(0)).getStatistics();
        Assert.assertEquals("1", stats.get("Controls#Power"));
    }
    @Test
    public void validateDeviceInfoStatistics() {

    }

    @Test
    public void validateNetworkStatistics() {

    }

    @Test
    public void validateInputStatistics() {

    }

    @Test
    public void validateVolumeStatistics() {

    }

    @Test
    public void validatePowerStatistics() {

    }

    @Test
    public void validateApplicationStatistics() {

    }

    @Test
    public void validateLEDStatistics() {

    }

    @Test
    public void validateAudioStatistics() {

    }

    @Test
    public void validateApplicationStatusStatistics() {

    }

    @Test
    public void validateContentStatistics() {

    }

    @Test
    public void validateDateTimeStatistics() {

    }

    @Test
    public void validatePowerSaveModesStatistics() {

    }

    @Test
    public void validateStatelessControlsStatistics() {

    }

}
