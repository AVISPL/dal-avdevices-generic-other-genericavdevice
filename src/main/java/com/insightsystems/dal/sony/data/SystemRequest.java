/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.insightsystems.dal.sony.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.insightsystems.dal.sony.data.serialization.MapToCoupleArraySerializer;

import java.util.AbstractMap;
import java.util.Map;


/**
 * Request object, that contains all the properties needed for performing
 * operations on Sony Bravia device.
 *
 * @since 1.0
 * @author Maksym.Rossiytsev
 * */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemRequest {
    private String version;
    private String method;
    private int id;
    @JsonSerialize(using = MapToCoupleArraySerializer.class)
    private Multimap<String, Object> params;

    public SystemRequest(String version, String method, int id) {
        this.version = version;
        this.method = method;
        this.id = id;
        this.params = ArrayListMultimap.create();
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
     * Retrieves {@link #method}
     *
     * @return value of {@link #method}
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets {@link #method} value
     *
     * @param method new value of {@link #method}
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Retrieves {@link #id}
     *
     * @return value of {@link #id}
     */
    public int getId() {
        return id;
    }

    /**
     * Sets {@link #id} value
     *
     * @param id new value of {@link #id}
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Retrieves {@link #params}
     *
     * @return value of {@link #params}
     */
    public Multimap<String, Object> getParams() {
        return params;
    }

    /**
     * Sets {@link #params} value
     *
     * @param params new value of {@link #params}
     */
    public void setParams(Multimap<String, Object> params) {
        this.params = params;
    }

    public SystemRequest withParams(AbstractMap.SimpleEntry<String, Object>... entries) {
        StringBuilder sb = new StringBuilder();
        params.clear();
        for (Map.Entry<String, Object> entry: entries) {
            params.put(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public SystemRequest withParams(Map<String, Object> entries) {
        params.clear();
        entries.entrySet().iterator().forEachRemaining(o -> params.put(o.getKey(), o.getValue()));
        return this;
    }

    public SystemRequest withParams(Multimap<String, Object> entries) {
        params.clear();
        params.putAll(entries);
        return this;
    }
}
