/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.insightsystems.dal.sony.data.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * This serializer is made for serializing request parameters into a proper format, according to the
 * Sony Bravia LCD API documentation https://pro-bravia.sony.net/develop/integrate/ip-control/index.html
 *
 * @since 1.0
 * @author Maksym.Rossiytsev
 * */
public class MapToCoupleArraySerializer extends JsonSerializer<Multimap<String, Object>> {

    /**
     * {@inheritDoc}
     *
     * In order to represent json parameters in a format of ["key1":"value1", "key2": "value2"....]
     * using traditional map - this serializer is used.
     **/
    @Override
    public void serialize(Multimap<String, Object> value, JsonGenerator generator,
                          SerializerProvider serializers) throws IOException {
        generator.writeStartArray();

        if (!value.isEmpty()) {
            generator.writeStartObject();
            for (Map.Entry<?, ?> entry : value.entries()) {
                Object entryValue = entry.getValue();
                if (entryValue instanceof Map) {
                    generator.writeFieldName((String) entry.getKey());
                    generator.writeStartArray();
                    generator.writeStartObject();

                    Iterator iterator = ((Map<String, Object>)entryValue).entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry values = (Map.Entry)iterator.next();
                        generator.writeObjectField((String) values.getKey(), values.getValue());
                    }

                    generator.writeEndObject();
                    generator.writeEndArray();
                } else {
                    generator.writeObjectField((String) entry.getKey(), entry.getValue());
                }
            }
            generator.writeEndObject();
        }
        generator.writeEndArray();
    }
}
