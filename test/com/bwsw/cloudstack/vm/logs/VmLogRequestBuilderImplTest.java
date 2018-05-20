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

package com.bwsw.cloudstack.vm.logs;

import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(DataProviderRunner.class)
public class VmLogRequestBuilderImplTest {

    @DataProvider
    public static Object[][] filters() {
        return new Object[][] {{LocalDateTime.of(2018, 5, 1, 10, 0, 0), null, null, null, "start-date-vm-log-query.json"},
                {null, LocalDateTime.of(2018, 5, 31, 12, 0, 0), null, null, "end-date-vm-log-query.json"},
                {null, null, ImmutableList.of("one", "two"), null, "keywords-vm-log-query.json"}, {null, null, null, "/var/log/app.log", "logfile-vm-log-query.json"},
                {LocalDateTime.of(2018, 5, 1, 0, 0, 0), LocalDateTime.of(2018, 5, 31, 23, 59, 59), ImmutableList.of("search_keyword"), "/var/log/app.log",
                        "complex-vm-log-query.json"}};
    }

    private static final String UUID = "uuid";
    private static final int PAGE_SIZE = 15;
    private static final int PAGE = 1;
    private static final String[] FIELDS = new String[] {"source", "message", "@timestamp"};
    private static final String[] EXCLUDED_FIELDS = {};

    private VmLogRequestBuilderImpl vmLogQueryBuilder = new VmLogRequestBuilderImpl();

    @Test
    public void testGetSearchQueryBasicRequest() {
        SearchRequest searchRequest = vmLogQueryBuilder.getLogSearchRequest(UUID, PAGE, PAGE_SIZE, null, null, null, null, null);

        checkCommonSettings(searchRequest, PAGE_SIZE);
        assertEquals(PAGE, searchRequest.source().from());
        assertNull(searchRequest.source().query());
    }

    @Test
    @UseDataProvider("filters")
    public void testGetSearchQueryFilters(LocalDateTime start, LocalDateTime end, List<String> keywords, String logFile, String resultFile) throws IOException {
        SearchRequest searchRequest = vmLogQueryBuilder.getLogSearchRequest(UUID, PAGE, PAGE_SIZE, null, start, end, keywords, logFile);

        checkCommonSettings(searchRequest, PAGE_SIZE);

        checkQuery(searchRequest, IOUtils.resourceToString(resultFile, Charset.defaultCharset(), this.getClass().getClassLoader()));
    }

    private void checkCommonSettings(SearchRequest searchRequest, int pageSize) {
        assertNotNull(searchRequest);

        SearchSourceBuilder searchSourceBuilder = searchRequest.source();
        assertNotNull(searchSourceBuilder);
        assertEquals(pageSize, searchSourceBuilder.size());

        FetchSourceContext fetchSourceContext = searchSourceBuilder.fetchSource();
        assertNotNull(fetchSourceContext);
        assertArrayEquals(FIELDS, fetchSourceContext.includes());
        assertArrayEquals(EXCLUDED_FIELDS, fetchSourceContext.excludes());
    }

    private void checkQuery(SearchRequest searchRequest, String expectedQuery) throws IOException {
        QueryBuilder queryBuilder = searchRequest.source().query();
        assertNotNull(queryBuilder);

        String query = queryBuilder.toXContent(XContentFactory.jsonBuilder(), ToXContent.EMPTY_PARAMS).string();
        assertEquals(expectedQuery.trim(), query);
    }
}
