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

import com.bwsw.cloudstack.api.GetVmLogsCmd;
import com.bwsw.cloudstack.api.ListVmLogFilesCmd;
import com.bwsw.cloudstack.api.ScrollVmLogsCmd;
import com.bwsw.cloudstack.response.AggregateResponse;
import com.bwsw.cloudstack.response.ScrollableListResponse;
import com.bwsw.cloudstack.response.VmLogFileResponse;
import com.bwsw.cloudstack.response.VmLogResponse;
import com.bwsw.cloudstack.vm.logs.util.HttpUtils;
import com.bwsw.cloustrack.vm.logs.entity.EntityConstants;
import com.bwsw.cloustrack.vm.logs.entity.SortField;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.collect.ImmutableMap;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VmLogManagerImpl extends ComponentLifecycleBase implements VmLogManager, Configurable {

    private static final Logger s_logger = Logger.getLogger(VmLogManagerImpl.class);
    private static final Map<String, String> s_logFields = ImmutableMap
            .of(EntityConstants.TIMESTAMP, VmLogRequestBuilder.DATE_FIELD, EntityConstants.FILE, VmLogRequestBuilder.LOG_FILE_SORT_FIELD, EntityConstants.LOG,
                    VmLogRequestBuilder.DATA_SORT_FIELD);

    @Inject
    private VMInstanceDao _vmInstanceDao;

    @Inject
    private VmLogRequestBuilder _vmLogRequestBuilder;

    @Inject
    private VmLogFetcher _vmLogFetcher;

    private RestHighLevelClient _restHighLevelClient;

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> commands = new ArrayList<>();
        commands.add(GetVmLogsCmd.class);
        commands.add(ScrollVmLogsCmd.class);
        commands.add(ListVmLogFilesCmd.class);
        return commands;
    }

    @Override
    public ScrollableListResponse<VmLogResponse> listVmLogs(Long id, LocalDateTime start, LocalDateTime end, List<String> keywords, String logFile, List<String> sortFields,
            Integer page, Integer pageSize, Integer scroll) {
        if (pageSize == null) {
            pageSize = VmLogDefaultPageSize.value();
        }
        if (pageSize == null || pageSize < 1) {
            throw new InvalidParameterValueException("Invalid page size");
        }
        if (page == null) {
            page = 1;
        } else if (page < 1) {
            throw new InvalidParameterValueException("Invalid page");
        }
        if (start != null && end != null && end.isBefore(start)) {
            throw new InvalidParameterValueException("Invalid start/end dates");
        }
        if (scroll != null && scroll < 0) {
            throw new InvalidParameterValueException("Invalid scroll");
        }
        List<SortField> sorting = null;
        if (sortFields != null && !sortFields.isEmpty()) {
            sorting = sortFields.stream().map(s -> {
                SortField.SortOrder order = SortField.SortOrder.ASC;
                if (s != null && s.startsWith(SortField.SortOrder.DESC.getPrefix())) {
                    s = s.substring(SortField.SortOrder.DESC.getPrefix().length());
                    order = SortField.SortOrder.DESC;
                }
                String field = s_logFields.get(s);
                if (field == null) {
                    throw new InvalidParameterValueException("Invalid sort field");
                }
                return new SortField(field, order);
            }).distinct().collect(Collectors.toList());
            Map<String, List<SortField>> sortByFields = sorting.stream().collect(Collectors.groupingBy(SortField::getField));
            if (!sortByFields.entrySet().stream().allMatch(kv -> kv.getValue().size() == 1)) {
                throw new InvalidParameterValueException("Invalid sort");
            }
        }

        VMInstanceVO vmInstanceVO = _vmInstanceDao.findById(id);
        if (vmInstanceVO == null) {
            throw new InvalidParameterValueException("Unable to find a virtual machine with specified id");
        }
        SearchRequest searchRequest = _vmLogRequestBuilder.getLogSearchRequest(vmInstanceVO.getUuid(), page, pageSize, scroll, start, end, keywords, logFile, sorting);
        try {
            return _vmLogFetcher.fetch(_restHighLevelClient, searchRequest, VmLogResponse.class);
        } catch (Exception e) {
            s_logger.error("Unable to retrieve VM logs", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to retrieve VM logs");
        }
    }

    @Override
    public ScrollableListResponse<VmLogResponse> scrollVmLogs(String scrollId, Integer timeout) {
        if (scrollId == null || scrollId.isEmpty()) {
            throw new InvalidParameterValueException("Invalid scroll id");
        }
        if (timeout == null || timeout < 0) {
            throw new InvalidParameterValueException("Invalid timeout");
        }
        SearchScrollRequest request = _vmLogRequestBuilder.getScrollRequest(scrollId, timeout);
        try {
            return _vmLogFetcher.scroll(_restHighLevelClient, request, VmLogResponse.class);
        } catch (Exception e) {
            s_logger.error("Unable to retrieve VM logs", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to retrieve VM logs");
        }
    }

    @Override
    public ListResponse<VmLogFileResponse> listVmLogFiles(Long id, LocalDateTime start, LocalDateTime end, Long startIndex, Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            throw new InvalidParameterValueException("Invalid page size");
        }
        if (startIndex == null) {
            startIndex = 0L;
        } else if (startIndex < 0) {
            throw new InvalidParameterValueException("Invalid start index");
        }
        if (start != null && end != null && end.isBefore(start)) {
            throw new InvalidParameterValueException("Invalid start/end dates");
        }
        VMInstanceVO vmInstanceVO = _vmInstanceDao.findById(id);
        if (vmInstanceVO == null) {
            throw new InvalidParameterValueException("Unable to find a virtual machine with specified id");
        }
        SearchRequest searchRequest = _vmLogRequestBuilder.getLogFileSearchRequest(vmInstanceVO.getUuid(), pageSize.intValue(), null, start, end);
        try {
            AggregateResponse<VmLogFileResponse> response = _vmLogFetcher.fetchLogFiles(_restHighLevelClient, searchRequest);
            if (startIndex < response.getCount()) {
                long lastIndex = pageSize - 1;
                while (startIndex > lastIndex && response.getSearchAfter() != null) {
                    searchRequest = _vmLogRequestBuilder.getLogFileSearchRequest(vmInstanceVO.getUuid(), pageSize.intValue(), response.getSearchAfter(), start, end);
                    response = _vmLogFetcher.fetchLogFiles(_restHighLevelClient, searchRequest);
                    lastIndex += pageSize;
                }
                if (startIndex <= lastIndex) {
                    ListResponse<VmLogFileResponse> listResponse = new ListResponse<>();
                    listResponse.setResponses(response.getItems(), response.getCount());
                    return listResponse;
                }
            }
            // more than available results are requested
            ListResponse<VmLogFileResponse> listResponse = new ListResponse<>();
            listResponse.setResponses(Collections.emptyList(), response.getCount());
            return listResponse;
        } catch (Exception e) {
            s_logger.error("Unable to retrieve VM log files", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to retrieve VM log files");
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        try {
            RestClientBuilder restClientBuilder = RestClient.builder(HttpUtils.getHttpHosts(VmLogElasticsearchList.value()).toArray(new HttpHost[] {}));
            String username = VmLogElasticsearchUsername.value();
            if (!Strings.isNullOrEmpty(username)) {
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, VmLogElasticsearchPassword.value()));
                restClientBuilder = restClientBuilder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
            }
            _restHighLevelClient = new RestHighLevelClient(restClientBuilder);
        } catch (IllegalArgumentException e) {
            s_logger.error("Failed to create ElasticSearch client", e);
            return false;
        }
        return true;
    }

    @Override
    public String getConfigComponentName() {
        return VmLogManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {VmLogElasticsearchList, VmLogElasticsearchUsername, VmLogElasticsearchPassword, VmLogDefaultPageSize};
    }

}
