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

package com.bwsw.cloudstack.vm.logs.service;

import com.bwsw.cloudstack.vm.logs.entity.SortField;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregatorFactories;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(DataProviderRunner.class)
public class VmLogRequestBuilderImplTest {

    @DataProvider
    public static Object[][] filters() {
        return new Object[][] {{LocalDateTime.of(2018, 5, 1, 10, 0, 0), null, null, null, "start-date-vm-log-query.json"},
                {null, LocalDateTime.of(2018, 5, 31, 12, 0, 0), null, null, "end-date-vm-log-query.json"},
                {null, null, ImmutableList.of("one * ? \\", "two"), null, "keywords-vm-log-query.json"}, {null, null, null, "/var/log/app.log", "logfile-vm-log-query.json"},
                {LocalDateTime.of(2018, 5, 1, 0, 0, 0), LocalDateTime.of(2018, 5, 31, 23, 59, 59), ImmutableList.of("search_keyword"), "/var/log/app.log",
                        "complex-vm-log-query.json"}};
    }

    @DataProvider
    public static Object[][] logFileFilters() {
        return new Object[][] {{LocalDateTime.of(2018, 5, 1, 10, 0, 0), null, "start-date-vm-log-file-query.json"},
                {null, LocalDateTime.of(2018, 5, 31, 12, 0, 0), "end-date-vm-log-file-query.json"},
                {LocalDateTime.of(2018, 5, 1, 0, 0, 0), LocalDateTime.of(2018, 5, 31, 23, 59, 59), "complex-vm-log-file-query.json"}};
    }

    private static final String UUID = "uuid";
    private static final int PAGE_SIZE = 15;
    private static final int PAGE = 1;
    private static final int TIMEOUT = 60000;
    private static final String[] FIELDS = {VmLogRequestBuilder.LOG_FILE_FIELD, VmLogRequestBuilder.DATA_FIELD, VmLogRequestBuilder.DATE_FIELD};
    private static final String[] EXCLUDED_FIELDS = {};
    private static final Map<String, Object> AGGREGATE_AFTER = ImmutableMap.of("source", "file.log");
    private static final ObjectMapper s_objectMapper = new ObjectMapper();

    private VmLogRequestBuilderImpl _vmLogQueryBuilder = new VmLogRequestBuilderImpl();

    @Test
    public void testGetLogSearchRequestBasicRequest() {
        SearchRequest searchRequest = _vmLogQueryBuilder.getLogSearchRequest(UUID, PAGE, PAGE_SIZE, null, null, null, null, null, null);

        checkCommonSearchQuerySettings(searchRequest, PAGE_SIZE);
        assertEquals((PAGE - 1) * PAGE_SIZE, searchRequest.source().from());
        assertNull(searchRequest.source().query());
    }

    @Test
    public void testGetLogSearchRequestScroll() {
        SearchRequest searchRequest = _vmLogQueryBuilder.getLogSearchRequest(UUID, PAGE, PAGE_SIZE, TIMEOUT, null, null, null, null, null);

        checkCommonSearchQuerySettings(searchRequest, PAGE_SIZE);
        assertEquals(-1, searchRequest.source().from());
        assertNotNull(searchRequest.scroll());
        assertEquals(TimeValue.timeValueMillis(TIMEOUT), searchRequest.scroll().keepAlive());
        assertNull(searchRequest.source().query());
    }

    @Test
    @UseDataProvider("filters")
    public void testGetLogSearchRequestFilters(LocalDateTime start, LocalDateTime end, List<String> keywords, String logFile, String resultFile) throws IOException {
        SearchRequest searchRequest = _vmLogQueryBuilder.getLogSearchRequest(UUID, PAGE, PAGE_SIZE, null, start, end, keywords, logFile, null);

        checkCommonSearchQuerySettings(searchRequest, PAGE_SIZE);

        checkQuery(searchRequest, IOUtils.resourceToString(resultFile, Charset.defaultCharset(), this.getClass().getClassLoader()));
    }

    @Test
    public void testGetLogSearchRequestSorting() {
        List<SortField> sortFields = new ArrayList<>();
        sortFields.add(new SortField(VmLogRequestBuilder.LOG_FILE_FIELD, SortField.SortOrder.ASC));
        sortFields.add(new SortField(VmLogRequestBuilder.DATE_FIELD, SortField.SortOrder.DESC));

        SearchRequest searchRequest = _vmLogQueryBuilder.getLogSearchRequest(UUID, PAGE, PAGE_SIZE, TIMEOUT, null, null, null, null, sortFields);

        checkCommonSearchQuerySettings(searchRequest, PAGE_SIZE);
        List<SortBuilder<?>> sortBuilders = searchRequest.source().sorts();
        assertNotNull(sortBuilders);
        assertEquals(sortFields.size(), sortBuilders.size());
        for (int i = 0; i < sortBuilders.size(); i++) {
            SortBuilder<?> sortBuilder = sortBuilders.get(i);
            assertTrue(sortBuilder instanceof FieldSortBuilder);
            SortField sortField = sortFields.get(i);
            assertEquals(sortField.getField(), ((FieldSortBuilder)sortBuilder).getFieldName());
            switch (sortBuilder.order()) {
            case DESC:
                assertEquals(SortField.SortOrder.DESC, sortField.getOrder());
                break;
            case ASC:
                assertEquals(SortField.SortOrder.ASC, sortField.getOrder());
                break;
            }
        }
    }

    @Test
    public void testGetScrollRequest() {
        SearchScrollRequest request = _vmLogQueryBuilder.getScrollRequest(UUID, TIMEOUT);

        assertNotNull(request);
        assertEquals(UUID, request.scrollId());
        assertNotNull(request.scroll());
        assertEquals(TimeValue.timeValueMillis(TIMEOUT), request.scroll().keepAlive());
    }

    @Test
    public void testGetLogFileSearchRequest() throws IOException {
        SearchRequest searchRequest = _vmLogQueryBuilder.getLogFileSearchRequest(UUID, PAGE_SIZE, null, null, null);

        checkCommonLogFileQuerySettings(searchRequest, PAGE_SIZE, null);
    }

    @Test
    @UseDataProvider("logFileFilters")
    public void testGetLogFileSearchRequestFilters(LocalDateTime start, LocalDateTime end, String resultFile) throws IOException {
        SearchRequest searchRequest = _vmLogQueryBuilder.getLogFileSearchRequest(UUID, PAGE_SIZE, null, start, end);

        checkCommonLogFileQuerySettings(searchRequest, PAGE_SIZE, null);

        checkQuery(searchRequest, IOUtils.resourceToString(resultFile, Charset.defaultCharset(), this.getClass().getClassLoader()));
    }

    @Test
    public void testGetLogFileSearchRequestAggregateAfter() throws IOException {
        SearchRequest searchRequest = _vmLogQueryBuilder.getLogFileSearchRequest(UUID, PAGE_SIZE, AGGREGATE_AFTER, null, null);

        checkCommonLogFileQuerySettings(searchRequest, PAGE_SIZE, AGGREGATE_AFTER);
    }

    private void checkCommonSearchQuerySettings(SearchRequest searchRequest, int pageSize) {
        assertNotNull(searchRequest);

        assertArrayEquals(new String[] {"vmlog-" + UUID + "-*"}, searchRequest.indices());
        SearchSourceBuilder searchSourceBuilder = searchRequest.source();
        assertNotNull(searchSourceBuilder);
        assertEquals(pageSize, searchSourceBuilder.size());

        FetchSourceContext fetchSourceContext = searchSourceBuilder.fetchSource();
        assertNotNull(fetchSourceContext);
        assertArrayEquals(FIELDS, fetchSourceContext.includes());
        assertArrayEquals(EXCLUDED_FIELDS, fetchSourceContext.excludes());
    }

    private void checkCommonLogFileQuerySettings(SearchRequest searchRequest, int pageSize, Map<String, Object> after) throws IOException {
        assertNotNull(searchRequest);

        assertArrayEquals(new String[] {"vmlog-" + UUID + "-*"}, searchRequest.indices());
        SearchSourceBuilder searchSourceBuilder = searchRequest.source();
        assertNotNull(searchSourceBuilder);
        assertEquals(0, searchSourceBuilder.size());
        assertFalse(searchSourceBuilder.trackTotalHits());

        AggregatorFactories.Builder aggregations = searchSourceBuilder.aggregations();

        List<AggregationBuilder> builders = aggregations.getAggregatorFactories();
        assertNotNull(builders);
        assertEquals(2, builders.size());
        for (AggregationBuilder builder : builders) {
            checkAggregation(builder, pageSize, after);
        }
    }

    private void checkQuery(SearchRequest searchRequest, String expectedQuery) throws IOException {
        QueryBuilder queryBuilder = searchRequest.source().query();
        assertNotNull(queryBuilder);

        String query = queryBuilder.toXContent(XContentFactory.jsonBuilder(), ToXContent.EMPTY_PARAMS).string();
        assertEquals(expectedQuery.trim(), query);
    }

    private void checkAggregation(AggregationBuilder aggregationBuilder, int pageSize, Map<String, Object> after) throws IOException {
        assertNotNull(aggregationBuilder);

        String expectedAggregation = IOUtils.resourceToString(aggregationBuilder.getName() + "-aggregation.json", Charset.defaultCharset(), this.getClass().getClassLoader());
        expectedAggregation = expectedAggregation.replaceFirst("%PAGE_SIZE%", String.valueOf(pageSize));
        String aggregationAfter = "";
        if (after != null) {
            aggregationAfter = ",\"after\":" + s_objectMapper.writeValueAsString(after);
        }
        expectedAggregation = expectedAggregation.replaceFirst("%AGGREGATE_AFTER%", aggregationAfter);
        assertEquals(expectedAggregation.trim(), aggregationBuilder.toString());
    }

}
