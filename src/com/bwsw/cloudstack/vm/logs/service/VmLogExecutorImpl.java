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

import com.bwsw.cloudstack.vm.logs.entity.ResponseEntity;
import com.bwsw.cloudstack.vm.logs.response.AggregateResponse;
import com.bwsw.cloudstack.vm.logs.response.ScrollableListResponse;
import com.bwsw.cloudstack.vm.logs.response.VmLogFileResponse;
import com.cloud.utils.exception.CloudRuntimeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeAggregation;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.cardinality.Cardinality;
import org.elasticsearch.search.aggregations.metrics.cardinality.CardinalityAggregationBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VmLogExecutorImpl implements VmLogExecutor {

    private final ObjectMapper _objectMapper = new ObjectMapper();

    @Override
    public <T extends ResponseEntity> ScrollableListResponse<T> fetch(RestHighLevelClient client, SearchRequest request, Class<T> elementClass) throws IOException {
        return parseSearch(client.search(request), elementClass);
    }

    @Override
    public <T extends ResponseEntity> ScrollableListResponse<T> scroll(RestHighLevelClient client, SearchScrollRequest request, Class<T> elementClass) throws IOException {
        return parseSearch(client.searchScroll(request), elementClass);
    }

    public AggregateResponse<VmLogFileResponse> fetchLogFiles(RestHighLevelClient client, SearchRequest request) throws IOException {
        SearchResponse response = client.search(request);
        if (response.status() != RestStatus.OK) {
            throw new CloudRuntimeException("Failed to retrieve VM log files");
        }
        if (response.getAggregations() == null) {
            return new AggregateResponse<>(Collections.emptyList(), 0, null);
        }
        Aggregation valueAggregation = response.getAggregations().get(VmLogRequestBuilder.LOG_FILE_AGGREGATION);
        if (valueAggregation == null || !CompositeAggregationBuilder.NAME.equals(valueAggregation.getType())) {
            throw new CloudRuntimeException("Invalid aggregation " + VmLogRequestBuilder.LOG_FILE_AGGREGATION);
        }

        Aggregation countAggregation = response.getAggregations().get(VmLogRequestBuilder.LOG_FILE_COUNT_AGGREGATION);
        if (countAggregation == null || !CardinalityAggregationBuilder.NAME.equals(countAggregation.getType())) {
            throw new CloudRuntimeException("Invalid aggregation " + VmLogRequestBuilder.LOG_FILE_COUNT_AGGREGATION);
        }

        CompositeAggregation compositeAggregation = (CompositeAggregation)valueAggregation;
        List<VmLogFileResponse> responses = new ArrayList<>();
        List<? extends CompositeAggregation.Bucket> buckets = compositeAggregation.getBuckets();
        if (buckets != null && !buckets.isEmpty()) {
            for (CompositeAggregation.Bucket bucket : buckets) {
                if (bucket.getKey() != null) {
                    String file = (String)bucket.getKey().get(VmLogRequestBuilder.LOG_FILE_FIELD);
                    if (file == null) {
                        throw new IOException("No data for log file");
                    }
                    responses.add(new VmLogFileResponse(file));
                }
            }
        }

        return new AggregateResponse<>(responses, (int)((Cardinality)countAggregation).getValue(), compositeAggregation.afterKey());
    }

    @Override
    public void index(RestHighLevelClient client, IndexRequest request) throws IOException {
        IndexResponse response = client.index(request);
        if (response.status() != RestStatus.CREATED) {
            throw new CloudRuntimeException("Failed to execute create operation");
        }
    }

    @Override
    public <T> T get(RestHighLevelClient client, GetRequest request, Class<T> entityClass) throws IOException {
        GetResponse response = client.get(request);
        if (response.isExists() && !response.isSourceEmpty()) {
            return _objectMapper.readValue(response.getSourceAsString(), entityClass);
        }
        return null;
    }

    @Override
    public void update(RestHighLevelClient client, UpdateRequest request) throws IOException {
        UpdateResponse response = client.update(request);
        if (response.status() != RestStatus.OK) {
            throw new CloudRuntimeException("Failed to execute update operation");
        }
    }

    private <T extends ResponseEntity> List<T> parseResults(SearchResponse response, Class<T> elementClass) throws IOException {
        List<T> results = new ArrayList<>();
        for (SearchHit searchHit : response.getHits()) {
            T element = _objectMapper.readValue(searchHit.getSourceAsString(), elementClass);
            element.setId(searchHit.getId());
            results.add(element);
        }
        return results;
    }

    private <T extends ResponseEntity> ScrollableListResponse<T> parseSearch(SearchResponse response, Class<T> elementClass) throws IOException {
        if (response.status() != RestStatus.OK || response.getHits() == null) {
            throw new CloudRuntimeException("Failed to retrieve VM logs");
        }
        return new ScrollableListResponse<>((int)response.getHits().getTotalHits(), parseResults(response, elementClass), response.getScrollId());
    }
}
