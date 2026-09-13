package com.hedgefund.common;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class Json {

    private static final ObjectMapper SHARED = createShared();

    private Json() {}

    private static ObjectMapper createShared() {
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return om;
    }

    public static ObjectMapper shared() {
        return SHARED;
    }
}
