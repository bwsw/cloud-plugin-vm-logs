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

import com.bwsw.cloudstack.vm.logs.api.CreateVmLogTokenCmd;
import com.bwsw.cloudstack.vm.logs.api.GetVmLogsCmd;
import com.bwsw.cloudstack.vm.logs.api.InvalidateVmLogTokenCmd;
import com.bwsw.cloudstack.vm.logs.api.ListVmLogFilesCmd;
import com.bwsw.cloudstack.vm.logs.api.ScrollVmLogsCmd;
import com.bwsw.cloudstack.vm.logs.entity.EntityConstants;
import com.bwsw.cloudstack.vm.logs.entity.SortField;
import com.bwsw.cloudstack.vm.logs.entity.Token;
import com.bwsw.cloudstack.vm.logs.response.AggregateResponse;
import com.bwsw.cloudstack.vm.logs.response.ScrollableListResponse;
import com.bwsw.cloudstack.vm.logs.response.VmLogFileResponse;
import com.bwsw.cloudstack.vm.logs.response.VmLogResponse;
import com.bwsw.cloudstack.vm.logs.security.TokenGenerator;
import com.bwsw.cloudstack.vm.logs.util.DateUtils;
import com.bwsw.cloudstack.vm.logs.util.HttpUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.rest.RestStatus;

import javax.inject.Inject;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VmLogManagerImpl extends ComponentLifecycleBase implements VmLogManager, Configurable {

    private static final Logger s_logger = Logger.getLogger(VmLogManagerImpl.class);
    private static final Map<String, String> s_logFields = ImmutableMap
            .of(EntityConstants.TIMESTAMP, VmLogRequestBuilder.DATE_FIELD, EntityConstants.FILE, VmLogRequestBuilder.LOG_FILE_SORT_FIELD, EntityConstants.LOG,
                    VmLogRequestBuilder.DATA_SORT_FIELD);
    private static final Pattern s_indexPattern = Pattern.compile("vmlog-(.+)-[0-9]{4}-[0-9]{2}-[0-9]{2}");

    @Inject
    private VMInstanceDao _vmInstanceDao;

    @Inject
    private VmLogRequestBuilder _vmLogRequestBuilder;

    @Inject
    private VmLogExecutor _vmLogExecutor;

    @Inject
    private TokenGenerator _tokenGenerator;

    @Inject
    private AccountManager _accountManager;

    private RestHighLevelClient _restHighLevelClient;

    private ObjectMapper _objectMapper = new ObjectMapper();

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> commands = new ArrayList<>();
        commands.add(GetVmLogsCmd.class);
        commands.add(ScrollVmLogsCmd.class);
        commands.add(ListVmLogFilesCmd.class);
        commands.add(CreateVmLogTokenCmd.class);
        commands.add(InvalidateVmLogTokenCmd.class);
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
            return _vmLogExecutor.fetch(_restHighLevelClient, searchRequest, VmLogResponse.class);
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
            return _vmLogExecutor.scroll(_restHighLevelClient, request, VmLogResponse.class);
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
            AggregateResponse<VmLogFileResponse> response = _vmLogExecutor.fetchLogFiles(_restHighLevelClient, searchRequest);
            if (startIndex < response.getCount()) {
                long lastIndex = pageSize - 1;
                while (startIndex > lastIndex && response.getSearchAfter() != null) {
                    searchRequest = _vmLogRequestBuilder.getLogFileSearchRequest(vmInstanceVO.getUuid(), pageSize.intValue(), response.getSearchAfter(), start, end);
                    response = _vmLogExecutor.fetchLogFiles(_restHighLevelClient, searchRequest);
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
    public String createToken(Long id) {
        VMInstanceVO vmInstanceVO = _vmInstanceDao.findById(id);
        if (vmInstanceVO == null) {
            throw new InvalidParameterValueException("Unable to find a virtual machine with specified id");
        }
        Token token = new Token(_tokenGenerator.generate(), vmInstanceVO.getUuid(), DateUtils.getCurrentDateTime());
        try {
            IndexRequest request = _vmLogRequestBuilder.getCreateTokenRequest(token);
            _vmLogExecutor.index(_restHighLevelClient, request);
            return token.getToken();
        } catch (IOException e) {
            s_logger.error("Unable to create VM log token", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create VM log token");
        }
    }

    @Override
    public boolean invalidateToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new InvalidParameterValueException("Invalid token");
        }
        GetRequest getRequest = _vmLogRequestBuilder.getGetTokenRequest(token);
        try {
            Token tokenResult = _vmLogExecutor.get(_restHighLevelClient, getRequest, Token.class);
            if (tokenResult == null) {
                throw new InvalidParameterValueException("The token does not exist");
            }
            if (tokenResult.getValidTo() != null) {
                throw new InvalidParameterValueException("Invalid token");
            }
            VMInstanceVO vmInstanceVO = null;
            if (tokenResult.getVmUuid() != null) {
                vmInstanceVO = _vmInstanceDao.findByUuid(tokenResult.getVmUuid());
            }
            if (vmInstanceVO == null) {
                throw new InvalidParameterValueException("Unable to find a virtual machine for the token");
            }
            _accountManager.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.OperateEntry, false, vmInstanceVO);
            UpdateRequest invalidateRequest = _vmLogRequestBuilder.getInvalidateTokenRequest(token, DateUtils.getCurrentDateTime());
            _vmLogExecutor.update(_restHighLevelClient, invalidateRequest);
            return true;
        } catch (IOException e) {
            s_logger.error("Unable to invalidate VM log token", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to invalidate VM log token");
        }
    }

    @Override
    public Map<String, Double> getVmLogStats() {
        Request request = _vmLogRequestBuilder.getLogIndicesStatsRequest();
        try {
            Response response = _vmLogExecutor.execute(_restHighLevelClient, request);
            if (response.getStatusLine().getStatusCode() != RestStatus.OK.getStatus()) {
                throw new CloudRuntimeException("Unexpected status for VM log index stats " + response.getStatusLine().getStatusCode());
            }
            JsonNode result = _objectMapper.readTree(EntityUtils.toString(response.getEntity()));
            if (result != null) {
                JsonNode data = result.path("indices");
                if (data.isMissingNode()) {
                    throw getInvalidStatsException();
                }
                Map<String, Double> stats = new HashMap<>();
                Iterator<Map.Entry<String, JsonNode>> indices = data.fields();
                while (indices.hasNext()) {
                    Map.Entry<String, JsonNode> index = indices.next();
                    Matcher vmUuidMatcher = s_indexPattern.matcher(index.getKey());
                    if (vmUuidMatcher.matches() && !index.getValue().isNull()) {
                        String vmUuid = vmUuidMatcher.group(1);
                        JsonNode indexSize = index.getValue().path("total").path("store").path("size_in_bytes");
                        if (!indexSize.isMissingNode()) {
                            stats.merge(vmUuid, indexSize.doubleValue(), (total, current) -> total + current);
                        } else {
                            throw getInvalidStatsException();
                        }
                    }
                }
                // size in MB
                stats.replaceAll((k, v) -> v / (1024 * 1024));
                return stats;
            } else {
                throw getInvalidStatsException();
            }
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to retrieve VM log index stats", e);
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
        return new ConfigKey<?>[] {VmLogElasticsearchList, VmLogElasticsearchUsername, VmLogElasticsearchPassword, VmLogDefaultPageSize, VmLogUsageTimeout};
    }

    private CloudRuntimeException getInvalidStatsException() {
        return new CloudRuntimeException("Invalid VM log index stats response");
    }

}
