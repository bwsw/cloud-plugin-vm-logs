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
import com.bwsw.cloudstack.response.VmLogFileResponse;
import com.bwsw.cloudstack.response.VmLogResponse;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.time.LocalDateTime;
import java.util.List;

public interface VmLogManager extends PluggableService {

    ConfigKey<String> VmLogElasticSearchList = new ConfigKey<>("Advanced", String.class, "vm.log.elasticsearch.list", null,
            "Comma separated list of ElasticSearch HTTP hosts; e.g. http://localhost,http://localhost:9201", false);

    ConfigKey<Integer> VmLogDefaultPageSize = new ConfigKey<>("Advanced", Integer.class, "vm.log.page.size.default", "100", "Default page size for VM log listing", true);

    ScrollableListResponse<VmLogResponse> listVmLogs(Long id, LocalDateTime start, LocalDateTime end, List<String> keywords, String logFile, Integer page, Integer pageSize,
            Object[] searchAfter);

    ListResponse<VmLogFileResponse> listVmLogFiles(Long id, LocalDateTime start, LocalDateTime end, Long startIndex, Long pageSize);

    void deleteVmLogs(String uuid);
}
