package com.bwsw.cloudstack.vm.logs.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class ParameterUtils {

    private final static ObjectMapper s_objectMapper = new ObjectMapper();

    public static String writeSearchAfter(Object[] sortValues) throws JsonProcessingException {
        if (sortValues == null) {
            return null;
        }
        return s_objectMapper.writeValueAsString(sortValues);
    }

    public static Object[] readSearchAfter(String searchAfter) throws IOException {
        if (searchAfter == null) {
            return null;
        }
        return s_objectMapper.readValue(searchAfter, Object[].class);
    }
}
