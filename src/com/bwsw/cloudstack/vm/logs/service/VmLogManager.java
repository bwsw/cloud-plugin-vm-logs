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

import com.bwsw.cloudstack.vm.logs.response.ScrollableListResponse;
import com.bwsw.cloudstack.vm.logs.response.VmLogFileResponse;
import com.bwsw.cloudstack.vm.logs.response.VmLogResponse;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.time.LocalDateTime;
import java.util.List;

public interface VmLogManager extends PluggableService {

    ConfigKey<String> VmLogElasticsearchList = new ConfigKey<>("Advanced", String.class, "vm.log.elasticsearch.list", null,
            "Comma separated list of ElasticSearch HTTP hosts; e.g. http://localhost,http://localhost:9201", false);

    ConfigKey<String> VmLogElasticsearchUsername = new ConfigKey<>("Advanced", String.class, "vm.log.elasticsearch.username", null, "Elasticsearch username for authentication",
            false);

    ConfigKey<String> VmLogElasticsearchPassword = new ConfigKey<>("Advanced", String.class, "vm.log.elasticsearch.password", null, "Elasticsearch password for authentication",
            false);

    ConfigKey<Integer> VmLogDefaultPageSize = new ConfigKey<>("Advanced", Integer.class, "vm.log.page.size.default", "100", "Default page size for VM log listing", true);

    ScrollableListResponse<VmLogResponse> listVmLogs(Long id, LocalDateTime start, LocalDateTime end, List<String> keywords, String logFile, List<String> sortFields, Integer page,
            Integer pageSize, Integer scroll);

    ScrollableListResponse<VmLogResponse> scrollVmLogs(String scrollId, Integer timeout);

    ListResponse<VmLogFileResponse> listVmLogFiles(Long id, LocalDateTime start, LocalDateTime end, Long startIndex, Long pageSize);
}
