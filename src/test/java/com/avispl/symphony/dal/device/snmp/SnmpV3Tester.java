package com.avispl.symphony.dal.device.snmp;/*
 * Minimal SNMPv3 GET example with proper engine-ID discovery and
 * boots/time handling.  Requires SNMP4J 3.x.
 */
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.*;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SnmpV3Tester {

    // ── Credentials & target ───────────────────────────────────────────────────
    private static final String USER       = "_";
    private static final String AUTH_PWD   = "_";
    private static final String PRIV_PWD   = "_";
    private static final String TARGET_IP  = "_/161";   // <-- change
    private static final OID    SYS_DESCR  = new OID("1.3.6.1.2.1.1.1.0");
    private static final OID    ENGINE_TIME=
            new OID("1.3.6.1.6.3.10.2.1.3.0");  // snmpEngineTime.0

    public static void main(String[] args) throws Exception {

        // 1. ONE USM for everything (discovery + real traffic)
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
        Snmp snmp = new Snmp(transport);
        snmp.getMessageDispatcher().addMessageProcessingModel(new MPv3(usm));
        transport.listen();

        // 2. Register the user (engineID = null → any engine)
        UsmUser user = new UsmUser(new OctetString(USER),
                AuthSHA.ID,  new OctetString(AUTH_PWD),
                PrivAES128.ID, new OctetString(PRIV_PWD));
        usm.addUser(new OctetString(USER), null, user);

        // 3. Discover the agent’s authoritative engine-ID
        UdpAddress agentAddr = new UdpAddress(TARGET_IP);
        byte[] agentEID = snmp.discoverAuthoritativeEngineID(agentAddr, 1500);
        if (agentEID == null) {
            throw new IllegalStateException("Engine-ID discovery failed");
        }

        // 4. Build the v3 target (with the engine-ID we just learned)
        UserTarget<UdpAddress> target = new UserTarget<>();
        target.setAddress(agentAddr);
        target.setVersion(SnmpConstants.version3);
        target.setSecurityName(new OctetString(USER));
        target.setSecurityLevel(SecurityLevel.AUTH_PRIV);
        target.setAuthoritativeEngineID(agentEID);
        target.setRetries(2);
        target.setTimeout(2000);

        // 5. Fire actual GETs — first one succeeds immediately (no REPORT)
        getAndPrint(snmp, target, SYS_DESCR);
        getAndPrint(snmp, target, ENGINE_TIME);

        snmp.close();      // tidy-up
        transport.close();
    }

    private static void getAndPrint(Snmp snmp, Target<?> target, OID oid)
            throws Exception {

        ScopedPDU pdu = new ScopedPDU();
        pdu.setType(PDU.GET);
        pdu.add(new VariableBinding(oid));

        ResponseEvent<?> ev = snmp.send(pdu, target);
        if (ev.getResponse() == null) {
            System.err.println("Timeout asking for " + oid);
        } else {
            VariableBinding vb = ev.getResponse().get(0);
            System.out.println(vb.getOid() + " = " + vb.getVariable());
        }
    }
}
