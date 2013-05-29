package com.kalixia.rawsag.codecs.jaxrs.converters;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class UUIDConverter implements Converter<UUID> {

    @Override
    public List<Class<UUID>> acceptClasses() {
        return Arrays.asList(UUID.class);
    }

    @Override
    public String toString(UUID uuid) {
        return uuid.toString();
    }

    @Override
    public UUID fromString(String value) {
        return UUID.fromString(value);
    }

}
