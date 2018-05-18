// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.bwsw.cloudstack.vm.logs.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.cloudstack.api.ServerApiException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(DataProviderRunner.class)
public class ParameterUtilsTest {

    private final static String PARAM = "param";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @DataProvider
    public static Object[][] jsonObjects() {
        return new Object[][] {{null, null}, {new Object[0], "[]"}, {new Object[] {12345, "text"}, "[12345,\"text\"]"}};
    }

    @DataProvider
    public static Object[][] validDates() {
        return new Object[][] {{null, null}, {"2018-05-01T15:01:02", LocalDateTime.of(2018, 5, 1, 15, 1, 2)}};
    }

    @DataProvider
    public static Object[][] invalidDates() {
        return new Object[][] {{""}, {"01-05-2018 15:01:02"}};
    }

    @Test
    @UseDataProvider("jsonObjects")
    public void testConvertToJson(Object[] objects, String json) throws JsonProcessingException {
        assertEquals(json, ParameterUtils.convertToJson(objects));
    }

    @Test
    @UseDataProvider("jsonObjects")
    public void testParseFromJson(Object[] objects, String json) throws IOException {
        assertArrayEquals(objects, ParameterUtils.parseFromJson(json));
    }

    @Test
    public void testParseFromJsonInvalidJson() throws IOException {
        expectedException.expect(JsonMappingException.class);
        ParameterUtils.parseFromJson("{\"a\"}");
    }

    @Test
    @UseDataProvider("validDates")
    public void testParseDate(String value, LocalDateTime dateTime) {
        assertEquals(dateTime, ParameterUtils.parseDate(value, PARAM));
    }

    @Test
    @UseDataProvider("invalidDates")
    public void testParseDateInvalidDates(String value) {
        expectedException.expect(ServerApiException.class);
        expectedException.expectMessage("\"" + PARAM + "\" parameter is invalid");
        ParameterUtils.parseDate(value, PARAM);
    }

}
