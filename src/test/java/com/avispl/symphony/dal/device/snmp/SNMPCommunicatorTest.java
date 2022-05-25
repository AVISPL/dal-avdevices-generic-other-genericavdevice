/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.device.snmp;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class SNMPCommunicatorTest {

    SNMPCommunicator snmpCommunicator = new SNMPCommunicator();

    @BeforeEach
    public void setUpBefore() throws Exception {
        snmpCommunicator.setSnmpPort(161);
        snmpCommunicator.setSnmpCommunity("public");
    }

    @Test
    public void testSnmpPropertiesWin10() throws Exception {
        snmpCommunicator.setHost("127.0.0.1");
        snmpCommunicator.setSnmpProperties(".1.3.6.1.2.1.1.1.0:Hardware|.1.3.6.1.2.1.1.4.0:Name|.1.3.6.1.2.1.1.5.0:DeviceName|.1.3.6.1.2.1.1.6.0:Location|.1.3.6.1.2.1.2.2.1.2.20:Network");
        snmpCommunicator.init();
        List<Statistics> statisticsList = snmpCommunicator.getMultipleStatistics();
        ExtendedStatistics statistics = (ExtendedStatistics) statisticsList.get(0);
        Map<String, String> statisticsMap = statistics.getStatistics();
        Assertions.assertNotNull(statisticsMap.get("AdapterMetadata#AdapterBuildDate"));
        Assertions.assertNotNull(statisticsMap.get("AdapterMetadata#AdapterUptime"));
        Assertions.assertNotNull(statisticsMap.get("AdapterMetadata#AdapterVersion"));

        Assertions.assertEquals("Max", statisticsMap.get("Name"));
        Assertions.assertEquals("Hardware: Intel64 Family 6 Model 166 Stepping 0 AT/AT COMPATIBLE - Software: Windows Version 6.3 (Build 19044 Multiprocessor Free)", statisticsMap.get("Hardware"));
        Assertions.assertEquals("DESKTOP-32LE6G6", statisticsMap.get("DeviceName"));
        Assertions.assertEquals("Ukraine", statisticsMap.get("Location"));
        Assertions.assertEquals("Realtek PCIe GbE Family Controller-WFP Native MAC Layer LightWeight Filter-0000", statisticsMap.get("Network"));
        Thread.sleep(30000);
        statisticsList = snmpCommunicator.getMultipleStatistics();
        statistics = (ExtendedStatistics) statisticsList.get(0);
        statisticsMap = statistics.getStatistics();
        Assertions.assertNotNull(statisticsMap.get("AdapterMetadata#AdapterBuildDate"));
        Assertions.assertNotNull(statisticsMap.get("AdapterMetadata#AdapterUptime"));
        Assertions.assertNotNull(statisticsMap.get("AdapterMetadata#AdapterVersion"));

        Assertions.assertEquals("Max", statisticsMap.get("Name"));
        Assertions.assertEquals("Hardware: Intel64 Family 6 Model 166 Stepping 0 AT/AT COMPATIBLE - Software: Windows Version 6.3 (Build 19044 Multiprocessor Free)", statisticsMap.get("Hardware"));
        Assertions.assertEquals("DESKTOP-32LE6G6", statisticsMap.get("DeviceName"));
        Assertions.assertEquals("Ukraine", statisticsMap.get("Location"));
        Assertions.assertEquals("Realtek PCIe GbE Family Controller-WFP Native MAC Layer LightWeight Filter-0000", statisticsMap.get("Network"));
        Thread.sleep(30000);
        statisticsList = snmpCommunicator.getMultipleStatistics();
        statistics = (ExtendedStatistics) statisticsList.get(0);
        statisticsMap = statistics.getStatistics();
        Assertions.assertNotNull(statisticsMap.get("AdapterMetadata#AdapterBuildDate"));
        Assertions.assertNotNull(statisticsMap.get("AdapterMetadata#AdapterUptime"));
        Assertions.assertNotNull(statisticsMap.get("AdapterMetadata#AdapterVersion"));

        Assertions.assertEquals("Max", statisticsMap.get("Name"));
        Assertions.assertEquals("Hardware: Intel64 Family 6 Model 166 Stepping 0 AT/AT COMPATIBLE - Software: Windows Version 6.3 (Build 19044 Multiprocessor Free)", statisticsMap.get("Hardware"));
        Assertions.assertEquals("DESKTOP-32LE6G6", statisticsMap.get("DeviceName"));
        Assertions.assertEquals("Ukraine", statisticsMap.get("Location"));
        Assertions.assertEquals("Realtek PCIe GbE Family Controller-WFP Native MAC Layer LightWeight Filter-0000", statisticsMap.get("Network"));
        Thread.sleep(30000);
        statisticsList = snmpCommunicator.getMultipleStatistics();
        statistics = (ExtendedStatistics) statisticsList.get(0);
        statisticsMap = statistics.getStatistics();
        Assertions.assertNotNull(statisticsMap.get("AdapterMetadata#AdapterBuildDate"));
        Assertions.assertNotNull(statisticsMap.get("AdapterMetadata#AdapterUptime"));
        Assertions.assertNotNull(statisticsMap.get("AdapterMetadata#AdapterVersion"));

        Assertions.assertEquals("Max", statisticsMap.get("Name"));
        Assertions.assertEquals("Hardware: Intel64 Family 6 Model 166 Stepping 0 AT/AT COMPATIBLE - Software: Windows Version 6.3 (Build 19044 Multiprocessor Free)", statisticsMap.get("Hardware"));
        Assertions.assertEquals("DESKTOP-32LE6G6", statisticsMap.get("DeviceName"));
        Assertions.assertEquals("Ukraine", statisticsMap.get("Location"));
        Assertions.assertEquals("Realtek PCIe GbE Family Controller-WFP Native MAC Layer LightWeight Filter-0000", statisticsMap.get("Network"));
    }

    @Test
    public void testSnmpPropertiesRemote() throws Exception {
        snmpCommunicator.setHost("172.31.254.114");
        snmpCommunicator.setSnmpProperties(".1.3.6.1.2.1.1.1.0:SystemDescription|.1.3.6.1.2.1.1.2.0:SystemID|.1.3.6.1.2.1.1.3.0:SystemUptime|.1.3.6.1.2.1.1.5.0:SystemName|.1.3.6.1.2.1.1.7.0:SystemServices|.1.3.6.1.2.1.1.6.0:SystemLocation");
        snmpCommunicator.init();
        List<Statistics> statisticsList = snmpCommunicator.getMultipleStatistics();
        ExtendedStatistics statistics = (ExtendedStatistics) statisticsList.get(0);
        Map<String, String> statisticsMap = statistics.getStatistics();
        Assertions.assertNotNull(statisticsMap.get("AdapterMetadata#AdapterBuildDate"));
        Assertions.assertNotNull(statisticsMap.get("AdapterMetadata#AdapterUptime"));
        Assertions.assertNotNull(statisticsMap.get("AdapterMetadata#AdapterVersion"));

        Assertions.assertEquals("Cisco Codec\n" +
                "SoftW: ce9.10.0.50f5888d087\n" +
                "MCU: Cisco TelePresence SX80\n" +
                "Date: 2019-12-17\n" +
                "S/N: FTT182902SN", statisticsMap.get("SystemDescription"));
        Assertions.assertNotNull(statisticsMap.get("SystemUptime"));
        Assertions.assertEquals("1.3.6.1.4.1.5596.150.6.4.1", statisticsMap.get("SystemID"));
        Assertions.assertEquals("NH-SX80", statisticsMap.get("SystemName"));
        Assertions.assertEquals("72", statisticsMap.get("SystemServices"));
    }
}
