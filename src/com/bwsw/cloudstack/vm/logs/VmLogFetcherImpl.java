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

import com.bwsw.cloudstack.response.AggregateResponse;
import com.bwsw.cloudstack.response.ScrollableListResponse;
import com.bwsw.cloudstack.response.VmLogFileResponse;
import com.bwsw.cloudstack.vm.logs.util.ParameterUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.cloudstack.api.ResponseObject;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
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
import java.util.List;

public class VmLogFetcherImpl implements VmLogFetcher {

    private final ObjectMapper _objectMapper = new ObjectMapper();

    @Override
    public <T extends ResponseObject> ScrollableListResponse<T> fetch(RestHighLevelClient client, SearchRequest request, Class<T> elementClass, boolean scroll) throws IOException {
        SearchResponse response = client.search(request);
        if (response.status() != RestStatus.OK || response.getHits() == null) {
            throw new CloudRuntimeException("Failed to retrieve VM logs");
        }
        return new ScrollableListResponse<>((int)response.getHits().getTotalHits(), parseResults(response, elementClass), getScrollId(response, scroll));
    }

    public AggregateResponse<VmLogFileResponse> fetchLogFiles(RestHighLevelClient client, SearchRequest request) throws IOException {
        SearchResponse response = client.search(request);
        if (response.status() != RestStatus.OK || response.getAggregations() == null) {
            throw new CloudRuntimeException("Failed to retrieve VM log files");
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

    private <T> List<T> parseResults(SearchResponse response, Class<T> elementClass) throws IOException {
        List<T> results = new ArrayList<>();
        for (SearchHit searchHit : response.getHits()) {
            results.add(_objectMapper.readValue(searchHit.getSourceAsString(), elementClass));
        }
        return results;
    }

    private String getScrollId(SearchResponse response, boolean scroll) throws JsonProcessingException {
        if (!scroll) {
            return null;
        }
        int hits = response.getHits().getHits().length;
        if (hits == 0) {
            return null;
        }
        return ParameterUtils.convertToJson(response.getHits().getHits()[hits - 1].getSortValues());
    }

}
