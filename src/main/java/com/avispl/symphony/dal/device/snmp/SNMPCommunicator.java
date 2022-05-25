/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.device.snmp;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.dto.snmp.SnmpEntry;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.BaseDevice;
import com.avispl.symphony.dal.util.StringUtils;

import java.io.IOException;
import java.util.*;

/**
 * Generic SNMP Device Adapter
 * <p>
 * Main features:
 * - Provide SNMP device info as ExtendedProperties, based on {@link #snmpProperties}
 * - Provide Adapter Metadata based on Build information and runtime stats
 *
 * @author Maksym.Rossiytsev / Symphony Dev Team<br>
 * Created on May 24, 2022
 */
public class SNMPCommunicator extends BaseDevice implements Monitorable {
    private String snmpProperties;
    /**
     * Adapter metadata, collected from the version.properties
     */
    private Properties adapterProperties;
    /**
     * Device adapter instantiation timestamp.
     */
    private long adapterInitializationTimestamp;
    /**
     * Retrieves {@link #snmpProperties}
     *
     * @return value of {@link #snmpProperties}
     */
    public String getSnmpProperties() {
        return snmpProperties;
    }

    /**
     * Sets {@link #snmpProperties} value
     *
     * @param snmpProperties new value of {@link #snmpProperties}
     */
    public void setSnmpProperties(String snmpProperties) {
        this.snmpProperties = snmpProperties;
    }

    @Override
    protected void internalInit() throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Internal init is called.");
        }
        adapterInitializationTimestamp = System.currentTimeMillis();
        try {
            loadAdapterMetaData();
        } catch (IOException exc) {
            // Catching an error there because adapter should remain functional regardless of this issue.
            logger.error("Unable to load adapter metadata during internalInit stage.", exc);
        }
        super.internalInit();
    }

    @Override
    public List<Statistics> getMultipleStatistics() throws Exception {
        ExtendedStatistics extendedStatistics = new ExtendedStatistics();
        Map<String, String> statistics = fetchSNMPProperties();
        statistics.put("AdapterMetadata#AdapterVersion", adapterProperties.getProperty("adapter.version"));
        statistics.put("AdapterMetadata#AdapterBuildDate", adapterProperties.getProperty("adapter.build.date"));
        statistics.put("AdapterMetadata#AdapterUptime", normalizeUptime((System.currentTimeMillis() - adapterInitializationTimestamp) / 1000));

        extendedStatistics.setStatistics(statistics);
        return Collections.singletonList(extendedStatistics);
    }

    /**
     * Load adapter metadata - adapter.version, adapter.build.date and adapter.uptime, based on
     * the build data and {@link #adapterInitializationTimestamp}
     *
     * @throws IOException if unable to read "version.properties" file
     */
    private void loadAdapterMetaData() throws IOException {
        adapterProperties = new Properties();
        adapterProperties.load(getClass().getResourceAsStream("/version.properties"));
    }

    /**
     * Fetch SNMP properties based on settings provided in {@link #snmpProperties}
     * in a format of OID:PropertyName separated with a pipe character, e.g
     * .1.3.6.1.2.1.1.1.0:SystemDescription|.1.3.6.1.2.1.1.2.0:SystemID|.1.3.6.1.2.1.1.3.0:SystemUptime
     *
     * @return {@link Map} with values retrieved by OIDs with property names retrieved from {@link #snmpProperties}
     * @throws IOException if a critical error occurs while retrieving SNMP properties
     * */
    private Map<String, String> fetchSNMPProperties() throws Exception {
        Map<String, String> result = new TreeMap<>();
        if(StringUtils.isNullOrEmpty(snmpProperties)) {
            return result;
        }
        String[] SNMPPropertyPairs = snmpProperties.split("\\|");
        for (String entry : SNMPPropertyPairs) {
            String[] entries = entry.split(":");
            if (entries.length < 2) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Error: Corrupted SNMP property entry: " + entry);
                }
                continue;
            }
            String oid = entries[0];
            if (StringUtils.isNullOrEmpty(oid)) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Error: Corrupted SNMP OID entry: " + entry);
                }
                continue;
            }
            String propertyName = entries[1];
            if (StringUtils.isNullOrEmpty(propertyName)) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Error: Corrupted SNMP propertyName entry: " + entry);
                }
                continue;
            }
            Collection<SnmpEntry> snmpEntries = querySnmp(Collections.singletonList(oid));
            if (snmpEntries.isEmpty()) {
                if (logger.isWarnEnabled()) {
                    logger.warn("No variable bindings available, skipping.");
                }
                continue;
            }

            snmpEntries.forEach(snmpEntry -> {
                String responseOid = snmpEntry.getOid();
                if (!oid.endsWith(String.valueOf(responseOid))) {
                    if (logger.isWarnEnabled()) {
                        logger.warn(String.format("SNMP Entry does not match by OID. Expected: %s, Actual: %s", oid, responseOid));
                    }
                    return;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Found matching variable binding, adding to monitored statistics: " + snmpEntry);
                }
                String variableValue = snmpEntry.getValue();
                if (StringUtils.isNullOrEmpty(variableValue)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Variable value is empty, skipping.");
                    }
                    return;
                }
                result.put(propertyName, variableValue.trim());
            });
        }
        return result;
    }

    /**
     * Uptime is received in seconds, need to normalize it and make it human readable, like
     * 1 day(s) 5 hour(s) 12 minute(s) 55 minute(s)
     * Incoming parameter is may have a decimal point, so in order to safely process this - it's rounded first.
     * We don't need to add a segment of time if it's 0.
     *
     * @param uptimeSeconds value in seconds
     * @return string value of format 'x day(s) x hour(s) x minute(s) x minute(s)'
     */
    private String normalizeUptime(long uptimeSeconds) {
        StringBuilder normalizedUptime = new StringBuilder();

        long seconds = uptimeSeconds % 60;
        long minutes = uptimeSeconds % 3600 / 60;
        long hours = uptimeSeconds % 86400 / 3600;
        long days = uptimeSeconds / 86400;

        if (days > 0) {
            normalizedUptime.append(days).append(" day(s) ");
        }
        if (hours > 0) {
            normalizedUptime.append(hours).append(" hour(s) ");
        }
        if (minutes > 0) {
            normalizedUptime.append(minutes).append(" minute(s) ");
        }
        if (seconds > 0) {
            normalizedUptime.append(seconds).append(" second(s)");
        }
        return normalizedUptime.toString().trim();
    }
}
