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
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchScrollRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface VmLogRequestBuilder {

    String DATE_FIELD = "@timestamp";
    String LOG_FILE_FIELD = "source";
    String LOG_FILE_SORT_FIELD = "source.keyword";
    String DATA_FIELD = "message";
    String DATA_SORT_FIELD = "message.keyword";
    String DATA_SEARCH_FIELD = "message.search";
    String LOG_FILE_AGGREGATION = "logfiles";
    String LOG_FILE_COUNT_AGGREGATION = "count_logfiles";

    SearchRequest getLogSearchRequest(String vmUuid, int page, int pageSize, Integer timeout, LocalDateTime start, LocalDateTime end, List<String> keywords, String logFile,
            List<SortField> sortFields);

    SearchScrollRequest getScrollRequest(String scrollId, int scrollTimeout);

    SearchRequest getLogFileSearchRequest(String vmUuid, int pageSize, Map<String, Object> aggregateAfter, LocalDateTime start, LocalDateTime end);
}
