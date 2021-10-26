package com.insightsystems.dal.sony;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class Lcd_Test {
    Lcd lcd;


    @Before
    public void setup() throws Exception {
        lcd = new Lcd();
        lcd.setHost("10.196.64.102");
        lcd.setPort(80);
        lcd.setProtocol("http");
        lcd.setPassword("s0nylcd");
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
        Assert.assertNotNull(stats.get("Device#dateTime"));
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
}
