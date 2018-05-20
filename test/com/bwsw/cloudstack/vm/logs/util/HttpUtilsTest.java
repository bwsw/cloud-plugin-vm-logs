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

import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.http.HttpHost;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@RunWith(DataProviderRunner.class)
public class HttpUtilsTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @DataProvider
    public static Object[][] validHosts() {
        return new Object[][] {{null, Collections.emptySet()}, {"", Collections.emptySet()}, {"some.com", Sets.newHashSet(new HttpHost("some.com", -1, "http"))},
                {"http://some.com:9200", Sets.newHashSet(new HttpHost("some.com", 9200, "http"))},
                {"https://some.com:9201", Sets.newHashSet(new HttpHost("some.com", 9201, "https"))},
                {"http://one.com:9200,https://two.com:9201", Sets.newHashSet(new HttpHost("one.com", 9200, "http"), new HttpHost("two.com", 9201, "https"))}};
    }

    @DataProvider
    public static Object[][] invalidHosts() {
        return new Object[][] {{" blank.com"}, {"http://some://"}, {"http://some:port"}, {",http://some.com"}};
    }

    @Test
    @UseDataProvider("validHosts")
    public void testGetHttpHostsValidData(String hostString, Set<HttpHost> hosts) {
        assertEquals(hosts, HttpUtils.getHttpHosts(hostString));
    }

    @Test
    @UseDataProvider("invalidHosts")
    public void testGetHttpHostsInvalidData(String hostString) {
        expectedException.expect(IllegalArgumentException.class);
        HttpUtils.getHttpHosts(hostString);
    }

}
