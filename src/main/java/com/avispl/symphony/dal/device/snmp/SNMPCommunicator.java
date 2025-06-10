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

import javax.security.auth.login.FailedLoginException;
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
     * Authentication protocol for SNMPv3
     * */
    private String authenticationProtocol = "AuthSHA";
    /**
     * Privacy protocol for SNMPv3
     * */
    private String privacyProtocol = "PrivAES128";
    /**
     * SNMP Version, 2c by default
     * */
    private String version = "2";
    /**
     *
     * */
    private String login;
    /**
     *
     * */
    private String password;
    /**
     * Instance of SNMPv3 client
     * */
    Snmp snmpv3;
    /**
     * Snmpv3 transport reference
     * */
    TransportMapping<UdpAddress> snmpv3Transport;
    /**
     * UserTarget storage for snmpv3 configuration
     * */
    UserTarget<UdpAddress> snmpv3target;

    /**
     * Retrieves {@link #login}
     *
     * @return value of {@link #login}
     */
    public String getLogin() {
        return login;
    }

    /**
     * Sets {@link #login} value
     *
     * @param login new value of {@link #login}
     */
    public void setLogin(String login) {
        this.login = login;
    }

    /**
     * Retrieves {@link #password}
     *
     * @return value of {@link #password}
     */
    public String getPassword() {
        return String.format("%s|%s", authPassword, privatePassword);
    }

    /**
     * Sets {@link #password} value
     *
     * @param password new value of {@link #password}
     */
    public void setPassword(String password) {
        if (password == null) {
            return;
        }
        String[] passwords = password.split("\\|");
        this.authPassword = passwords[0];
        if (passwords.length == 2) {
            this.privatePassword = passwords[1];
        } else if (passwords.length > 2) {
            throw new IllegalArgumentException("Password value is corrupted. Please make sure to only include SNMPv3 auth and private password, separated by a | character.");
        }
    }

    /**
     * Retrieves {@link #authenticationProtocol}
     *
     * @return value of {@link #authenticationProtocol}
     */
    public String getAuthenticationProtocol() {
        return authenticationProtocol;
    }

    /**
     * Sets {@link #authenticationProtocol} value
     *
     * @param authenticationProtocol new value of {@link #authenticationProtocol}
     */
    public void setAuthenticationProtocol(String authenticationProtocol) {
        this.authenticationProtocol = authenticationProtocol;
    }

    /**
     * Retrieves {@link #privacyProtocol}
     *
     * @return value of {@link #privacyProtocol}
     */
    public String getPrivacyProtocol() {
        return privacyProtocol;
    }

    /**
     * Sets {@link #privacyProtocol} value
     *
     * @param privacyProtocol new value of {@link #privacyProtocol}
     */
    public void setPrivacyProtocol(String privacyProtocol) {
        this.privacyProtocol = privacyProtocol;
    }

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
            if (snmpv3Transport != null && snmpv3Transport.isListening()) {
                snmpv3Transport.close();
                snmpv3Transport = null;
            }
            if (snmpv3 != null) {
                snmpv3.close();
                snmpv3 = null;
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
        boolean snmpv3 = Objects.equals(version, "3");
        if (snmpv3 && StringUtils.isNotNullOrEmpty(authenticationProtocol)) {
            statistics.put("AdapterMetadata#AuthenticationProtocol", authenticationProtocol);
        }
        if (snmpv3 && StringUtils.isNotNullOrEmpty(securityLevel)) {
            statistics.put("AdapterMetadata#SecurityLevel", securityLevel);
        }
        if (snmpv3 && StringUtils.isNotNullOrEmpty(privacyProtocol)) {
            statistics.put("AdapterMetadata#PrivacyProtocol", privacyProtocol);
        }

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

        return result;
    }

    /**
     * Initialize v3 SNMP client
     *
     * @throws IOException if SNMP isn't initialized properly
     * @since 2.0.0
     * */
    private void initSNMPv3() throws IOException, FailedLoginException {
        if (StringUtils.isNullOrEmpty(login) && ("AUTH_PRIV".equals(securityLevel) || "AUTH_NOPRIV".equals(securityLevel))) {
            throw new FailedLoginException("Invalid login: please check snmp version, security name or security level configured.");
        }
        if (StringUtils.isNullOrEmpty(authPassword) && ("AUTH_PRIV".equals(securityLevel) || "AUTH_NOPRIV".equals(securityLevel))) {
            throw new FailedLoginException("Invalid authPassword: please check snmp version, auth password or security level configured.");
        }
        if (StringUtils.isNullOrEmpty(privatePassword) && ("AUTH_PRIV".equals(securityLevel) || "NOAUTH_PRIV".equals(securityLevel))) {
            throw new FailedLoginException("Invalid privatePassword: please check snmp version, private password or security level configured.");
        }
        if (StringUtils.isNullOrEmpty(securityLevel)) {
            throw new FailedLoginException("Invalid securityLevel: please check snmp version or security level configured [AUTH_PRIV, NOAUTH_PRIV, NOAUTH_NOPRIV].");
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

        if (snmpv3Transport == null || !snmpv3Transport.isListening()) {
            snmpv3Transport = new DefaultUdpTransportMapping();
            snmpv3Transport.listen();

            snmpv3 = new Snmp(snmpv3Transport);
        }
        if (snmpv3 == null) {
            snmpv3 = new Snmp(snmpv3Transport);
        }
        snmpv3.getMessageDispatcher().addMessageProcessingModel(new MPv3(usm));

        OctetString loginOctet = null;
        OctetString authPasswordOctet = null;
        OctetString privPasswordOctet = null;

        if (StringUtils.isNotNullOrEmpty(login)) {
            loginOctet = new OctetString(login);
        }
        if (StringUtils.isNotNullOrEmpty(authPassword)) {
            authPasswordOctet = new OctetString(authPassword);
        }
        if (StringUtils.isNotNullOrEmpty(privatePassword)) {
            privPasswordOctet = new OctetString(privatePassword);
        }
        OID authenticationProtocol = retrieveAuthenticationProtocol();
        OID privacyProtocol = retrievePrivacyProtocol();

        UsmUser user = new UsmUser(loginOctet,
                authPasswordOctet == null ? null : authenticationProtocol,  authPasswordOctet,
                privPasswordOctet == null ? null : privacyProtocol, privPasswordOctet);
        usm.addUser(loginOctet, null, user);

        UdpAddress agentAddr = new UdpAddress(getHost() + "/" + getSnmpPort());
        byte[] agentEID = snmpv3.discoverAuthoritativeEngineID(agentAddr, 1500);
        if (agentEID == null) {
            throw new IllegalStateException("SNMPv3 EngineID discovery failed. Please check target hostname or SNMP service status.");
        }

        snmpv3target = new UserTarget<>();
        snmpv3target.setAddress(agentAddr);
        snmpv3target.setVersion(SnmpConstants.version3);
        snmpv3target.setSecurityName(new OctetString(login));
        snmpv3target.setSecurityLevel(LocalSecurityLevel.findLevelByName(securityLevel));
        snmpv3target.setAuthoritativeEngineID(agentEID);
        snmpv3target.setRetries(2);
        snmpv3target.setTimeout(2000);
    }

    /**
     * Retrieve authentication protocol based on {@link #authenticationProtocol} variable
     * AuthSHA is used by default - if the {@link #authenticationProtocol} is not supported or not provided
     *
     * @return OID of the selected privacy protocol
     * */
    private OID retrieveAuthenticationProtocol() {
        switch (this.authenticationProtocol) {
            case "AuthSHA":
                return AuthSHA.ID;
            case "AuthMD5":
                return AuthMD5.ID;
            case "AuthHMAC384SHA512":
                return AuthHMAC384SHA512.ID;
            case "AuthHMAC128SHA224":
                return AuthHMAC128SHA224.ID;
            case "AuthHMAC256SHA384":
                return AuthHMAC256SHA384.ID;
            case "AuthHMAC192SHA256":
                return AuthHMAC192SHA256.ID;
            default:
                logger.warn(String.format("Cannot set authentication protocol to %s, switching to AuthSHA.", this.authenticationProtocol));
                return AuthSHA.ID;
        }
    }

    /**
     * Retrieve privacy protocol based on {@link #privacyProtocol} variable
     * PrivAES128 is used by default - if the {@link #privacyProtocol} is not supported or not provided
     *
     * @return OID of the selected privacy protocol
     * */
    private OID retrievePrivacyProtocol() {
        switch (this.privacyProtocol) {
            case "PrivAES128":
                return PrivAES128.ID;
            case "PrivAES192":
                return PrivAES192.ID;
            case "PrivAES256":
                return PrivAES256.ID;
            case "PrivDES":
                return PrivDES.ID;
            case "Priv3DES":
                return Priv3DES.ID;
            default:
                logger.warn(String.format("Cannot set privacy protocol to %s, switching to PrivAES128.", this.privacyProtocol));
                return PrivAES128.ID;
        }
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
        if (ev.getResponse() != null && ev.getResponse().toString().startsWith("REPORT")) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Unable to retrieve oid %s value: REPORT received: %s", oid, ev.getResponse().toString()));
            }
            response = "N/A";
        } else if (ev.getResponse() == null) {
            response = "Request timed out";
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
