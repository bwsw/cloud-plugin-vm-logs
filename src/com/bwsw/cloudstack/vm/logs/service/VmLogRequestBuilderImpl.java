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

import com.bwsw.cloudstack.vm.logs.entity.EntityConstants;
import com.bwsw.cloudstack.vm.logs.entity.SortField;
import com.bwsw.cloudstack.vm.logs.entity.Token;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.composite.TermsValuesSourceBuilder;
import org.elasticsearch.search.aggregations.metrics.cardinality.CardinalityAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VmLogRequestBuilderImpl implements VmLogRequestBuilder {

    private static final String INDEX_PREFIX = "vmlog-";
    private static final String INDEX_SUFFIX = "-*";
    private static final String INDEX_PATTERN = "vmlog-*-*";
    private static final String[] FIELDS = new String[] {LOG_FILE_FIELD, DATA_FIELD, DATE_FIELD};
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final String LOG_FILE_KEYWORD_FIELD = LOG_FILE_FIELD + ".keyword";
    private static final String[] WILDCARD_CHARS = new String[] {"*", "?", "\\"};
    private static final String[] WILDCARD_ESCAPED_CHARS = new String[] {"\\*", "\\?", "\\\\"};

    private final ObjectMapper _objectMapper = new ObjectMapper();

    @Override
    public SearchRequest getLogSearchRequest(String vmUuid, int page, int pageSize, Integer timeout, LocalDateTime start, LocalDateTime end, List<String> keywords, String logFile,
            List<SortField> sortFields) {
        SearchRequest request = new SearchRequest(getIndex(vmUuid));

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.fetchSource(FIELDS, null);
        sourceBuilder.size(pageSize);

        if (timeout != null) {
            request.scroll(TimeValue.timeValueMillis(timeout));
        } else {
            sourceBuilder.from((page - 1) * pageSize);
        }

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        if (start != null || end != null) {
            RangeQueryBuilder dateFilter = QueryBuilders.rangeQuery(DATE_FIELD);
            if (start != null) {
                dateFilter.gte(format(start));
            }
            if (end != null) {
                dateFilter.lt(format(end));
            }
            queryBuilder.filter(dateFilter);
        }
        if (!Strings.isNullOrEmpty(logFile)) {
            queryBuilder.filter(QueryBuilders.termQuery(LOG_FILE_KEYWORD_FIELD, logFile));
        }
        if (keywords != null && !keywords.isEmpty()) {
            keywords.forEach(e -> queryBuilder
                    .must(QueryBuilders.wildcardQuery(DATA_SEARCH_FIELD, StringUtils.wrap(StringUtils.replaceEach(e, WILDCARD_CHARS, WILDCARD_ESCAPED_CHARS), '*'))));
        }
        if (queryBuilder.hasClauses()) {
            sourceBuilder.query(queryBuilder);
        }
        if (sortFields != null && !sortFields.isEmpty()) {
            for (SortField sortField : sortFields) {
                sourceBuilder.sort(sortField.getField(), convert(sortField.getOrder()));
            }
        }
        request.source(sourceBuilder);
        return request;
    }

    @Override
    public SearchScrollRequest getScrollRequest(String scrollId, int scrollTimeout) {
        SearchScrollRequest request = new SearchScrollRequest(scrollId);
        request.scroll(TimeValue.timeValueMillis(scrollTimeout));
        return request;
    }

    @Override
    public SearchRequest getLogFileSearchRequest(String vmUuid, int pageSize, Map<String, Object> aggregateAfter, LocalDateTime start, LocalDateTime end) {
        SearchRequest request = new SearchRequest(getIndex(vmUuid));

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(0);
        sourceBuilder.trackTotalHits(false);

        if (start != null || end != null) {
            RangeQueryBuilder dateFilter = QueryBuilders.rangeQuery(DATE_FIELD);
            if (start != null) {
                dateFilter.gte(format(start));
            }
            if (end != null) {
                dateFilter.lt(format(end));
            }
            sourceBuilder.query(dateFilter);
        }
        CompositeAggregationBuilder termBuilder = new CompositeAggregationBuilder(LOG_FILE_AGGREGATION,
                Collections.singletonList(new TermsValuesSourceBuilder(LOG_FILE_FIELD).field(LOG_FILE_KEYWORD_FIELD).order(SortOrder.ASC))).size(pageSize);
        if (aggregateAfter != null) {
            termBuilder.aggregateAfter(aggregateAfter);
        }
        sourceBuilder.aggregation(termBuilder);

        CardinalityAggregationBuilder countBuilder = AggregationBuilders.cardinality(LOG_FILE_COUNT_AGGREGATION).field(LOG_FILE_KEYWORD_FIELD);
        sourceBuilder.aggregation(countBuilder);

        request.source(sourceBuilder);
        return request;
    }

    @Override
    public IndexRequest getCreateTokenRequest(Token token) throws JsonProcessingException {
        IndexRequest request = new IndexRequest(REGISTRY_INDEX, REGISTRY_TYPE, token.getToken());
        request.opType(DocWriteRequest.OpType.CREATE);
        request.source(_objectMapper.writeValueAsString(token), XContentType.JSON);
        return request;
    }

    @Override
    public GetRequest getGetTokenRequest(String token) {
        return new GetRequest(REGISTRY_INDEX, REGISTRY_TYPE, token);
    }

    @Override
    public UpdateRequest getInvalidateTokenRequest(String token, LocalDateTime validTo) {
        Map<String, Object> fields = new HashMap<>();
        fields.put(EntityConstants.VALID_TO, validTo);

        UpdateRequest request = new UpdateRequest(REGISTRY_INDEX, REGISTRY_TYPE, token);
        request.doc(fields);
        return request;
    }

    @Override
    public Request getLogIndicesStatsRequest() {
        return new Request("GET", INDEX_PATTERN + "/_stats/store", Collections.emptyMap(), null);
    }

    private String getIndex(String vmUuid) {
        return INDEX_PREFIX + vmUuid + INDEX_SUFFIX;
    }

    private String format(LocalDateTime dateTime) {
        return DATE_TIME_FORMATTER.format(dateTime);
    }

    private SortOrder convert(SortField.SortOrder sortOrder) {
        switch (sortOrder) {
        case ASC:
            return SortOrder.ASC;
        case DESC:
            return SortOrder.DESC;
        default:
            throw new IllegalArgumentException("Unknown sort order " + sortOrder);
        }
    }
}
