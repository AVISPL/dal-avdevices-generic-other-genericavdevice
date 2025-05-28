/*
 * Copyright (c) 2022-2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.device.snmp;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.dto.snmp.SnmpEntry;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.BaseDevice;
import com.avispl.symphony.dal.util.StringUtils;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.*;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import com.avispl.symphony.dal.device.snmp.v3.LocalSecurityLevel;

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
     * Snmpv3 username
     * */
    private String securityName;
    /**
     * Snmpv3 auth password
     * */
    private String authPassword;
    /**
     * Snmpv3 private password
     * */
    private String privatePassword;
    /**
     * {@link SecurityLevel#AUTH_PRIV}, {@link SecurityLevel#AUTH_NOPRIV} or {@link SecurityLevel#NOAUTH_NOPRIV} security level
     * for SNMPv3
     * */
    private String securityLevel;
    /**
     * SNMP Version, 2c by default
     * */
    private String version = "2";
    /**
     * Instance of SNMPv3 client
     * */
    Snmp snmpv3;
    /**
     * UserTarget storage for snmpv3 configuration
     * */
    UserTarget<UdpAddress> snmpv3target;

    /**
     * Retrieves {@link #securityLevel}
     *
     * @return value of {@link #securityLevel}
     */
    public String getSecurityLevel() {
        return securityLevel;
    }

    /**
     * Sets {@link #securityLevel} value
     *
     * @param securityLevel new value of {@link #securityLevel}
     */
    public void setSecurityLevel(String securityLevel) {
        this.securityLevel = securityLevel;
    }

    /**
     * Retrieves {@link #version}
     *
     * @return value of {@link #version}
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets {@link #version} value
     *
     * @param version new value of {@link #version}
     */
    public void setVersion(String version) {
        this.version = version;
    }

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

    /**
     * Retrieves {@link #privatePassword}
     *
     * @return value of {@link #privatePassword}
     */
    public String getPrivatePassword() {
        return privatePassword;
    }

    /**
     * Sets {@link #privatePassword} value
     *
     * @param privatePassword new value of {@link #privatePassword}
     */
    public void setPrivatePassword(String privatePassword) {
        this.privatePassword = privatePassword;
    }

    /**
     * Retrieves {@link #authPassword}
     *
     * @return value of {@link #authPassword}
     */
    public String getAuthPassword() {
        return authPassword;
    }

    /**
     * Sets {@link #authPassword} value
     *
     * @param authPassword new value of {@link #authPassword}
     */
    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }

    /**
     * Retrieves {@link #securityName}
     *
     * @return value of {@link #securityName}
     */
    public String getSecurityName() {
        return securityName;
    }

    /**
     * Sets {@link #securityName} value
     *
     * @param securityName new value of {@link #securityName}
     */
    public void setSecurityName(String securityName) {
        this.securityName = securityName;
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
    protected void internalDestroy() {
        try {
            snmpv3target = null;
            if (snmpv3 != null) {
                snmpv3.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Exception during SNMPv3 client termination", e);
        } finally {
            super.internalDestroy();
        }
    }

    @Override
    public List<Statistics> getMultipleStatistics() throws Exception {
        ExtendedStatistics extendedStatistics = new ExtendedStatistics();
        Map<String, String> statistics = fetchSNMPProperties();
        statistics.put("AdapterMetadata#AdapterVersion", adapterProperties.getProperty("adapter.version"));
        statistics.put("AdapterMetadata#AdapterBuildDate", adapterProperties.getProperty("adapter.build.date"));
        statistics.put("AdapterMetadata#AdapterUptime", normalizeUptime((System.currentTimeMillis() - adapterInitializationTimestamp) / 1000));
        statistics.put("AdapterMetadata#SNMPVersion", String.valueOf(version));

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
        if (version.equals("3")) {
            initSNMPv3();
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

            Collection<SnmpEntry> snmpEntries;
            if (version.equals("3")) {
                snmpEntries = querySnmpv3(oid);
            } else {
                snmpEntries = querySnmp(Collections.singletonList(oid));
                if (snmpEntries.isEmpty()) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("No variable bindings available, skipping.");
                    }
                    continue;
                }
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
        if (version.equals("3")) {
            snmpv3.close();
        }
        return result;
    }

    /**
     * Initialize v3 SNMP client
     *
     * @throws IOException if SNMP isn't initialized properly
     * @since 2.0.0
     * */
    private void initSNMPv3() throws IOException {
        if (StringUtils.isNullOrEmpty(securityName) && ("AUTH_PRIV".equals(securityLevel) || "AUTH_NOPRIV".equals(securityLevel))) {
            throw new IllegalArgumentException("Invalid securityName: please check snmp version, security name or security level configured.");
        }
        if (StringUtils.isNullOrEmpty(authPassword) && ("AUTH_PRIV".equals(securityLevel) || "AUTH_NOPRIV".equals(securityLevel))) {
            throw new IllegalArgumentException("Invalid authPassword: please check snmp version, auth password or security level configured.");
        }
        if (StringUtils.isNullOrEmpty(privatePassword) && ("AUTH_PRIV".equals(securityLevel) || "NOAUTH_PRIV".equals(securityLevel))) {
            throw new IllegalArgumentException("Invalid privatePassword: please check snmp version, private password or security level configured.");
        }
        if (StringUtils.isNullOrEmpty(securityLevel)) {
            throw new IllegalArgumentException("Invalid securityLevel: please check snmp version or security level configured [AUTH_PRIV, NOAUTH_PRIV, NOAUTH_NOPRIV].");
        }

        SecurityProtocols.getInstance().addAuthenticationProtocol( new AuthSHA());
        SecurityProtocols.getInstance().addAuthenticationProtocol( new AuthHMAC128SHA224());
        SecurityProtocols.getInstance().addAuthenticationProtocol( new AuthHMAC384SHA512());
        SecurityProtocols.getInstance().addAuthenticationProtocol( new AuthHMAC192SHA256());
        SecurityProtocols.getInstance().addAuthenticationProtocol( new AuthHMAC256SHA384());
        SecurityProtocols.getInstance().addAuthenticationProtocol( new AuthMD5());

        USM usm = new USM(SecurityProtocols.getInstance(),
                new OctetString(MPv3.createLocalEngineID()), 0);
        SecurityModels.getInstance().addSecurityModel(usm);

        TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
        snmpv3 = new Snmp(transport);
        snmpv3.getMessageDispatcher().addMessageProcessingModel(new MPv3(usm));
        transport.listen();

        UsmUser user = new UsmUser(new OctetString(securityName),
                AuthSHA.ID,  new OctetString(authPassword),
                PrivAES128.ID, new OctetString(privatePassword));
        usm.addUser(new OctetString(securityName), null, user);

        UdpAddress agentAddr = new UdpAddress(getHost() + "/" + getSnmpPort());
        byte[] agentEID = snmpv3.discoverAuthoritativeEngineID(agentAddr, 1500);
        if (agentEID == null) {
            throw new IllegalStateException("SNMPv3 EngineID discovery failed. Please check target hostname or SNMP service status.");
        }

        snmpv3target = new UserTarget<>();
        snmpv3target.setAddress(agentAddr);
        snmpv3target.setVersion(SnmpConstants.version3);
        snmpv3target.setSecurityName(new OctetString(securityName));
        snmpv3target.setSecurityLevel(LocalSecurityLevel.findLevelByName(securityLevel));
        snmpv3target.setAuthoritativeEngineID(agentEID);
        snmpv3target.setRetries(2);
        snmpv3target.setTimeout(2000);
    }

    /**
     * Make an SNMPv3 query and retrieve remote OID value
     *
     * @param oid to retreive a value for
     * @throws IOException if operation cannot be completed due to an IO issue
     * @since 2.0.0
     * */
    private Collection<SnmpEntry> querySnmpv3(String oid) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Retrieving SNMPv3 OID: " + oid);
        }
        ScopedPDU pdu = new ScopedPDU();
        pdu.setType(PDU.GET);
        pdu.add(new VariableBinding(new OID(oid)));

        ResponseEvent<?> ev = snmpv3.send(pdu, snmpv3target);
        String response;
        if (ev.getResponse() == null) {
            response = "Timeout";
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("OID %s retrieval timeout.", oid));
            }
        } else {
            VariableBinding vb = ev.getResponse().get(0);
            response = vb.getVariable().toString();
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("OID %s retrieved successfully with value %s", oid, response));
            }
        }
        SnmpEntry entry = new SnmpEntry();
        entry.setOid(oid);
        entry.setValue(response);
        return Collections.singletonList(entry);
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
