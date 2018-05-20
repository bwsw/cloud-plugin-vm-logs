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

import org.elasticsearch.action.search.SearchRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface VmLogRequestBuilder {

    String DATE_FIELD = "@timestamp";
    String LOG_FILE_FIELD = "source";
    String DATA_FIELD = "message";

    SearchRequest getLogSearchRequest(String vmUuid, int page, int pageSize, Object[] searchAfter, LocalDateTime start, LocalDateTime end, List<String> keywords, String logFile);
}
