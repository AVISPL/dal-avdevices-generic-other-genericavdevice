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
    public void validateAllStatistics() throws Exception {
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
        Assert.assertNotNull(stats.get("Network#Gateway"));
        Assert.assertNotNull(stats.get("Network#InterfaceName"));
        Assert.assertNotNull(stats.get("Network#Ipv4Address"));
        Assert.assertNotNull(stats.get("Network#Ipv6Address"));
        Assert.assertNotNull(stats.get("Network#Netmask"));
        Assert.assertNotNull(stats.get("SignalPresence#Hdmi1"));
        Assert.assertNotNull(stats.get("SignalPresence#Hdmi2"));
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
}
