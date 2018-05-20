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

import com.bwsw.cloudstack.response.ScrollableListResponse;
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

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VmLogFetcherImpl implements VmLogFetcher {

    @Inject
    private VmLogRequestBuilder _vmLogRequestBuilder;

    private final ObjectMapper _objectMapper = new ObjectMapper();

    @Override
    public <T extends ResponseObject> ScrollableListResponse<T> fetch(RestHighLevelClient client, SearchRequest request, Class<T> elementClass, boolean scroll) throws IOException {
        SearchResponse response = client.search(request);
        if (response.status() != RestStatus.OK || response.getHits() == null) {
            throw new CloudRuntimeException("Failed to retrieve VM logs");
        }
        return new ScrollableListResponse<>((int)response.getHits().getTotalHits(), parseResults(response, elementClass), getScrollId(response, scroll));
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
