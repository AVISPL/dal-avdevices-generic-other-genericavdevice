/*
 * Copyright (c) 2025 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.device.snmp.v3;

import org.snmp4j.security.SecurityLevel;

import java.util.Arrays;
import java.util.Optional;

/**
 * Security level wrapper, so we can allow customer to provide security name as a part of the adapter configuration, in
 * text value - AUTH_PRIV, AUTH_NOPRIV or NOAUTH_NOPRIV
 *
 * @author Maksym.Rossiitsev/Symphony team
 * @since 2.0.0
 * */
public enum LocalSecurityLevel {
    AUTH_PRIV(SecurityLevel.AUTH_PRIV),
    AUTH_NOPRIV(SecurityLevel.AUTH_NOPRIV),
    NOAUTH_NOPRIV(SecurityLevel.NOAUTH_NOPRIV);

    private int level;

    LocalSecurityLevel(int level) {
        this.level = level;
    }

    /**
     * Filter security level by provided name
     *
     * @param levelName to lookup security name by
     * @return int security level
     * */
    public static int findLevelByName(String levelName) {
        Optional<LocalSecurityLevel> securityLevel = Arrays.stream(values()).filter(sl -> sl.name().equals(levelName)).findFirst();
        if (securityLevel.isPresent()) {
            return securityLevel.get().level;
        }
        throw new IllegalArgumentException("Unable to find SNMPv3 security level with name " + levelName);
    }
}
