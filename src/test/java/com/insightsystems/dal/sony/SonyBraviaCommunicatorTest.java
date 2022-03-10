package com.insightsystems.dal.sony;

import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static com.insightsystems.dal.sony.data.Constant.SPEAKER_SUBWOOFER_FREQ;
import static com.insightsystems.dal.sony.data.Constant.SPEAKER_SUBWOOFER_LEVEL;

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
        Assert.assertNotNull(stats.get("DeviceControls#Input"));
        Assert.assertNotNull(stats.get("DeviceControls#Power"));
        Assert.assertNotNull(stats.get("DeviceControls#PowerSaveMode"));
        Assert.assertNotNull(stats.get("DeviceControls#Reboot"));
        Assert.assertNotNull(stats.get("DeviceControls#TerminateApps"));
        Assert.assertNotNull(stats.get("DeviceInfo#DateTime"));
        Assert.assertNotNull(stats.get("DeviceInfo#InterfaceVersion"));
        Assert.assertNotNull(stats.get("DeviceInfo#LedIndicatorMode"));
        Assert.assertNotNull(stats.get("DeviceInfo#LedIndicatorState"));
        Assert.assertNotNull(stats.get("DeviceInfo#ProductName"));
        Assert.assertNotNull(stats.get("DeviceInfo#SerialNumber"));
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

    @Test
    public void testVolume() throws Exception {
        ControllableProperty property = new ControllableProperty();
        property.setProperty("AudioControls#SpeakerVolume");
        property.setValue(20.1f);
        lcd.controlProperty(property);
        Map<String,String> stats = ((ExtendedStatistics)lcd.getMultipleStatistics().get(0)).getStatistics();
        Assert.assertEquals("20.0", stats.get("AudioControls#SpeakerVolume"));
    }

    @Test
    public void testSubwooferFreq() throws Exception {
        ControllableProperty property = new ControllableProperty();
        property.setProperty(SPEAKER_SUBWOOFER_FREQ);
        property.setValue(15.1f);
        lcd.controlProperty(property);
        Map<String,String> stats = ((ExtendedStatistics)lcd.getMultipleStatistics().get(0)).getStatistics();
        Assert.assertEquals("15", stats.get(SPEAKER_SUBWOOFER_FREQ));
    }

    @Test
    public void testSubwooferLevel() throws Exception {
        ControllableProperty property = new ControllableProperty();
        property.setProperty(SPEAKER_SUBWOOFER_LEVEL);
        property.setValue(10.1f);
        lcd.getMultipleStatistics();
        lcd.controlProperty(property);
        Map<String,String> stats = ((ExtendedStatistics)lcd.getMultipleStatistics().get(0)).getStatistics();
        Assert.assertEquals("10", stats.get(SPEAKER_SUBWOOFER_LEVEL));
    }

    @Test
    public void testSpeakerMute() throws Exception {
        ControllableProperty property = new ControllableProperty();
        property.setProperty("AudioControls#SpeakerMute");
        property.setValue(0);
        lcd.getMultipleStatistics();
        lcd.controlProperty(property);
        Map<String,String> stats = ((ExtendedStatistics)lcd.getMultipleStatistics().get(0)).getStatistics();
        Assert.assertEquals("0", stats.get("AudioControls#SpeakerMute"));
    }
}
