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
import com.bwsw.cloudstack.vm.logs.response.AggregateResponse;
import com.bwsw.cloudstack.vm.logs.response.ScrollableListResponse;
import com.bwsw.cloudstack.vm.logs.response.VmLogFileResponse;
import com.bwsw.cloudstack.vm.logs.response.VmLogResponse;
import com.bwsw.cloudstack.vm.logs.security.TokenGenerator;
import com.bwsw.cloudstack.vm.logs.util.DateUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.collect.ImmutableList;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.hamcrest.CustomMatcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VmLogManagerImplTest {

    private static final long VM_ID = 1;
    private static final String UUID = "61d12f36-0201-4035-b6fc-c7f768f583f1";
    private static final int PAGE = 2;
    private static final int PAGE_SIZE = 10;
    private static final long START_INDEX = 0;
    private static final long PAGE_SIZE_ONE = 1;
    private static final int TIMEOUT = 60000;
    private static final String SCROLL_ID = "scrollId";
    private static final LocalDateTime DATE_TIME = LocalDateTime.now();
    private static final String TOKEN = "5zVatA2s0kTi1Wxafwcf-lUKNVMw1-Fq8u2tzIcaTOWuZ5_mgH_e0EnZxLEEF5kp-WAUCEUsyqhy1osYQoVfOA";
    private static final Token TOKEN_ENTITY = new Token(TOKEN, UUID, LocalDateTime.now());

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private VMInstanceDao _vmInstanceDao;

    @Mock
    private RestHighLevelClient _restHighLevelClient;

    @Mock
    private VmLogRequestBuilder _vmLogRequestBuilder;

    @Mock
    private VmLogExecutor _vmLogExecutor;

    @Mock
    private TokenGenerator _tokenGenerator;

    @Mock
    private AccountManager _accountManager;

    @Mock
    private SearchScrollRequest _searchScrollRequest;

    @Mock
    private IndexRequest _indexRequest;

    @Mock
    private GetRequest _getRequest;

    @Mock
    private UpdateRequest _updateRequest;

    @Mock
    private VMInstanceVO _vmInstanceVO;

    @Mock
    private User _callerUser;

    @Mock
    private Account _callerAccount;

    @InjectMocks
    private VmLogManagerImpl _vmLogManager = new VmLogManagerImpl();

    private ScrollableListResponse<VmLogResponse> _emptyVmLogResponse = new ScrollableListResponse<>(0, null, SCROLL_ID);

    private SearchRequest _searchRequest = new SearchRequest();

    @BeforeClass
    public static void beforeClass() {
        CallContext.unregisterAll();
    }

    @AfterClass
    public static void afterClass() {
        CallContext.unregisterAll();
    }

    @Test
    public void testListVmLogsInvalidPageSize() {
        setExceptionExpectation(InvalidParameterValueException.class, "page size");

        _vmLogManager.listVmLogs(VM_ID, null, null, null, null, null, PAGE, -1, null);
    }

    @Test
    public void testListVmLogsInvalidPage() {
        setExceptionExpectation(InvalidParameterValueException.class, "page");

        _vmLogManager.listVmLogs(VM_ID, null, null, null, null, null, -1, PAGE_SIZE, null);
    }

    @Test
    public void testListVmLogsInvalidDates() {
        setExceptionExpectation(InvalidParameterValueException.class, "start/end dates");

        _vmLogManager.listVmLogs(VM_ID, DATE_TIME, DATE_TIME.minusDays(1), null, null, null, PAGE, PAGE_SIZE, null);
    }

    @Test
    public void testListVmLogsInvalidScroll() {
        setExceptionExpectation(InvalidParameterValueException.class, "scroll");

        _vmLogManager.listVmLogs(VM_ID, null, null, null, null, null, PAGE, PAGE_SIZE, -1);
    }

    @Test
    public void testListVmLogsNonexistentSortField() {
        setExceptionExpectation(InvalidParameterValueException.class, "sort field");

        List<String> sortFields = ImmutableList.of("unknown");

        _vmLogManager.listVmLogs(VM_ID, null, null, null, null, sortFields, PAGE, PAGE_SIZE, null);
    }

    @Test
    public void testListVmLogsContradictorySorting() {
        setExceptionExpectation(InvalidParameterValueException.class, "sort");

        List<String> sortFields = ImmutableList.of(EntityConstants.FILE, EntityConstants.LOG, "-" + EntityConstants.FILE);

        _vmLogManager.listVmLogs(VM_ID, null, null, null, null, sortFields, PAGE, PAGE_SIZE, null);
    }

    @Test
    public void testListVmLogsNonexistentVM() {
        setExceptionExpectation(InvalidParameterValueException.class, "virtual machine");
        when(_vmInstanceDao.findById(VM_ID)).thenReturn(null);

        _vmLogManager.listVmLogs(VM_ID, null, null, null, null, null, PAGE, PAGE_SIZE, null);
    }

    @Test
    public void testListVmLogsRequestException() throws IOException {
        setExceptionExpectation(ServerApiException.class, "VM logs");
        setVmExpectations();
        when(_vmLogRequestBuilder.getLogSearchRequest(UUID, PAGE, PAGE_SIZE, null, null, null, null, null, null)).thenReturn(_searchRequest);
        when(_vmLogExecutor.fetch(_restHighLevelClient, _searchRequest, VmLogResponse.class)).thenThrow(new IOException());

        _vmLogManager.listVmLogs(VM_ID, null, null, null, null, null, PAGE, PAGE_SIZE, null);
    }

    @Test
    public void testListVmLogsDefaultValues() throws IOException {
        setVmExpectations();
        when(_vmLogRequestBuilder.getLogSearchRequest(UUID, 1, VmLogManager.VmLogDefaultPageSize.value(), null, null, null, null, null, null)).thenReturn(_searchRequest);
        when(_vmLogExecutor.fetch(_restHighLevelClient, _searchRequest, VmLogResponse.class)).thenReturn(_emptyVmLogResponse);

        ScrollableListResponse<VmLogResponse> result = _vmLogManager.listVmLogs(VM_ID, null, null, null, null, null, null, null, null);

        assertSame(_emptyVmLogResponse, result);
    }

    @Test
    public void testListVmLogs() throws IOException {
        LocalDateTime end = DATE_TIME.plusDays(1);
        String logFile = "app.log";
        List<String> keywords = ImmutableList.of("first", "second");
        List<String> sort = ImmutableList.of(EntityConstants.TIMESTAMP, EntityConstants.LOG, EntityConstants.FILE);
        List<SortField> sortFields = ImmutableList
                .of(new SortField(VmLogRequestBuilder.DATE_FIELD, SortField.SortOrder.ASC), new SortField(VmLogRequestBuilder.DATA_SORT_FIELD, SortField.SortOrder.ASC),
                        new SortField(VmLogRequestBuilder.LOG_FILE_SORT_FIELD, SortField.SortOrder.ASC));

        setVmExpectations();
        when(_vmLogRequestBuilder.getLogSearchRequest(UUID, PAGE, PAGE_SIZE, TIMEOUT, DATE_TIME, end, keywords, logFile, sortFields)).thenReturn(_searchRequest);
        when(_vmLogExecutor.fetch(_restHighLevelClient, _searchRequest, VmLogResponse.class)).thenReturn(_emptyVmLogResponse);

        ScrollableListResponse<VmLogResponse> result = _vmLogManager.listVmLogs(VM_ID, DATE_TIME, end, keywords, logFile, sort, PAGE, PAGE_SIZE, TIMEOUT);

        assertSame(_emptyVmLogResponse, result);
    }

    @Test
    public void testScrollVmLogsNullScrollId() {
        setExceptionExpectation(InvalidParameterValueException.class, "scroll id");

        _vmLogManager.scrollVmLogs(null, TIMEOUT);
    }

    @Test
    public void testScrollVmLogsEmptyScrollId() {
        setExceptionExpectation(InvalidParameterValueException.class, "scroll id");

        _vmLogManager.scrollVmLogs("", TIMEOUT);
    }

    @Test
    public void testScrollVmLogsNullTimeout() {
        setExceptionExpectation(InvalidParameterValueException.class, "timeout");

        _vmLogManager.scrollVmLogs(SCROLL_ID, null);
    }

    @Test
    public void testScrollVmLogsNegativeTimeout() {
        setExceptionExpectation(InvalidParameterValueException.class, "timeout");

        _vmLogManager.scrollVmLogs(SCROLL_ID, -1);
    }

    @Test
    public void testScrollVmLogsRequestException() throws IOException {
        setExceptionExpectation(ServerApiException.class, "VM logs");

        when(_vmLogRequestBuilder.getScrollRequest(SCROLL_ID, TIMEOUT)).thenReturn(_searchScrollRequest);
        when(_vmLogExecutor.scroll(_restHighLevelClient, _searchScrollRequest, VmLogResponse.class)).thenThrow(new IOException());

        _vmLogManager.scrollVmLogs(SCROLL_ID, TIMEOUT);
    }

    @Test
    public void testScrollVmLogs() throws IOException {
        when(_vmLogRequestBuilder.getScrollRequest(SCROLL_ID, TIMEOUT)).thenReturn(_searchScrollRequest);
        when(_vmLogExecutor.scroll(_restHighLevelClient, _searchScrollRequest, VmLogResponse.class)).thenReturn(_emptyVmLogResponse);

        ScrollableListResponse<VmLogResponse> result = _vmLogManager.scrollVmLogs(SCROLL_ID, TIMEOUT);

        assertSame(_emptyVmLogResponse, result);
    }

    @Test
    public void testListVmLogFilesNullPageSize() {
        setExceptionExpectation(InvalidParameterValueException.class, "page size");

        _vmLogManager.listVmLogFiles(VM_ID, null, null, null, null);
    }

    @Test
    public void testListVmLogFilesNegativePageSize() {
        setExceptionExpectation(InvalidParameterValueException.class, "page size");

        _vmLogManager.listVmLogFiles(VM_ID, null, null, null, -1L);
    }

    @Test
    public void testListVmLogFilesZeroPageSize() {
        setExceptionExpectation(InvalidParameterValueException.class, "page size");

        _vmLogManager.listVmLogFiles(VM_ID, null, null, null, 0L);
    }

    @Test
    public void testListVmLogFilesNegativeStartIndex() {
        setExceptionExpectation(InvalidParameterValueException.class, "start index");

        _vmLogManager.listVmLogFiles(VM_ID, null, null, -1L, PAGE_SIZE_ONE);
    }

    @Test
    public void testListVmLogFilesInvalidDates() {
        setExceptionExpectation(InvalidParameterValueException.class, "start/end dates");

        _vmLogManager.listVmLogFiles(VM_ID, DATE_TIME, DATE_TIME.minusDays(1), START_INDEX, PAGE_SIZE_ONE);
    }

    @Test
    public void testListVmLogFilesNonexistentVM() {
        setExceptionExpectation(InvalidParameterValueException.class, "virtual machine");
        when(_vmInstanceDao.findById(VM_ID)).thenReturn(null);

        _vmLogManager.listVmLogFiles(VM_ID, null, null, START_INDEX, PAGE_SIZE_ONE);
    }

    @Test
    public void testListVmLogFilesRequestException() throws IOException {
        setExceptionExpectation(ServerApiException.class, "VM log files");
        setVmExpectations();
        when(_vmLogRequestBuilder.getLogFileSearchRequest(UUID, (int)PAGE_SIZE_ONE, null, null, null)).thenReturn(_searchRequest);
        when(_vmLogExecutor.fetchLogFiles(_restHighLevelClient, _searchRequest)).thenThrow(new IOException());

        _vmLogManager.listVmLogFiles(VM_ID, null, null, START_INDEX, PAGE_SIZE_ONE);
    }

    @Test
    public void testListVmLogFilesEmptyResults() throws IOException {
        setVmExpectations();
        when(_vmLogRequestBuilder.getLogFileSearchRequest(UUID, (int)PAGE_SIZE_ONE, null, null, null)).thenReturn(_searchRequest);
        when(_vmLogExecutor.fetchLogFiles(_restHighLevelClient, _searchRequest)).thenReturn(new AggregateResponse<>(null, 0, null));

        ListResponse<VmLogFileResponse> result = _vmLogManager.listVmLogFiles(VM_ID, null, null, START_INDEX, PAGE_SIZE_ONE);

        assertNotNull(result);
        assertNotNull(result.getCount());
        assertEquals(0, result.getCount().intValue());
        assertNotNull(result.getResponses());
        assertTrue(result.getResponses().isEmpty());
    }

    @Test
    public void testListVmLogFiles() throws IOException {
        int count = (int)PAGE_SIZE_ONE * 2;
        LocalDateTime end = DATE_TIME.plusDays(1);
        List<VmLogFileResponse> firstPageResults = ImmutableList.of(new VmLogFileResponse("server.log"));
        Map<String, Object> searchAfter = firstPageResults.stream().collect(Collectors.toMap(s -> VmLogRequestBuilder.LOG_FILE_SORT_FIELD, Function.identity()));
        List<VmLogFileResponse> secondPageResults = ImmutableList.of(new VmLogFileResponse("warning.log"));

        setVmExpectations();
        when(_vmLogRequestBuilder.getLogFileSearchRequest(UUID, (int)PAGE_SIZE_ONE, null, DATE_TIME, end)).thenReturn(_searchRequest);
        when(_vmLogRequestBuilder.getLogFileSearchRequest(UUID, (int)PAGE_SIZE_ONE, searchAfter, DATE_TIME, end)).thenReturn(_searchRequest);
        when(_vmLogExecutor.fetchLogFiles(_restHighLevelClient, _searchRequest)).thenReturn(new AggregateResponse<>(firstPageResults, count, searchAfter))
                .thenReturn(new AggregateResponse<>(secondPageResults, count, null));

        ListResponse<VmLogFileResponse> result = _vmLogManager.listVmLogFiles(VM_ID, DATE_TIME, end, PAGE_SIZE_ONE, PAGE_SIZE_ONE);

        assertNotNull(result);
        assertNotNull(result.getCount());
        assertEquals(count, result.getCount().intValue());
        assertEquals(secondPageResults, result.getResponses());
    }

    @Test
    public void testCreateVmLogTokenNonexistentVM() {
        setExceptionExpectation(InvalidParameterValueException.class, "virtual machine");
        when(_vmInstanceDao.findById(VM_ID)).thenReturn(null);

        _vmLogManager.createToken(VM_ID);
    }

    @Test
    public void testCreateVmLogTokenRequestException() throws IOException {
        setExceptionExpectation(ServerApiException.class, "VM log token");
        setVmExpectations();
        when(_tokenGenerator.generate()).thenReturn(TOKEN);
        when(_vmLogRequestBuilder.getCreateTokenRequest(any(Token.class))).thenReturn(_indexRequest);
        doThrow(new IOException()).when(_vmLogExecutor).index(_restHighLevelClient, _indexRequest);

        _vmLogManager.createToken(VM_ID);
    }

    @Test
    public void testCreateVmLogToken() throws IOException {
        setVmExpectations();
        when(_tokenGenerator.generate()).thenReturn(TOKEN);
        when(_vmLogRequestBuilder.getCreateTokenRequest(argThat(new CustomMatcher<Token>("VM log token") {
            @Override
            public boolean matches(Object o) {
                if (!(o instanceof Token)) {
                    return false;
                }
                Token token = (Token)o;
                return TOKEN.equals(token.getToken()) && UUID.equals(token.getVmUuid()) && token.getValidFrom() != null
                        && Duration.between(token.getValidFrom(), DateUtils.getCurrentDateTime()).toMillis() < 1000 && token.getValidTo() == null;
            }
        }))).thenReturn(_indexRequest);
        doNothing().when(_vmLogExecutor).index(_restHighLevelClient, _indexRequest);

        String result = _vmLogManager.createToken(VM_ID);

        assertEquals(TOKEN, result);
    }

    @Test
    public void testInvalidateTokenNullToken() {
        setExceptionExpectation(InvalidParameterValueException.class, "token");

        _vmLogManager.invalidateToken(null);
    }

    @Test
    public void testInvalidateTokenEmptyToken() {
        setExceptionExpectation(InvalidParameterValueException.class, "token");

        _vmLogManager.invalidateToken("");
    }

    @Test
    public void testInvalidateTokenNonexistentToken() throws IOException {
        setExceptionExpectation(InvalidParameterValueException.class, "token");

        when(_vmLogRequestBuilder.getGetTokenRequest(TOKEN)).thenReturn(_getRequest);
        when(_vmLogExecutor.get(_restHighLevelClient, _getRequest, Token.class)).thenReturn(null);

        _vmLogManager.invalidateToken(TOKEN);
    }

    @Test
    public void testInvalidateTokenGetRequestException() throws IOException {
        setExceptionExpectation(ServerApiException.class, "VM log token");

        when(_vmLogRequestBuilder.getGetTokenRequest(TOKEN)).thenReturn(_getRequest);
        when(_vmLogExecutor.get(_restHighLevelClient, _getRequest, Token.class)).thenThrow(new IOException());

        _vmLogManager.invalidateToken(TOKEN);
    }

    @Test
    public void testInvalidateTokenInvalidToken() throws IOException {
        setExceptionExpectation(InvalidParameterValueException.class, "token");

        when(_vmLogRequestBuilder.getGetTokenRequest(TOKEN)).thenReturn(_getRequest);
        Token token = new Token(TOKEN, UUID, DateUtils.getCurrentDateTime());
        token.setValidTo(DateUtils.getCurrentDateTime());
        when(_vmLogExecutor.get(_restHighLevelClient, _getRequest, Token.class)).thenReturn(token);

        _vmLogManager.invalidateToken(TOKEN);
    }

    @Test
    public void testInvalidateTokenNonexistentVM() throws IOException {
        setExceptionExpectation(InvalidParameterValueException.class, "virtual machine");

        when(_vmLogRequestBuilder.getGetTokenRequest(TOKEN_ENTITY.getToken())).thenReturn(_getRequest);
        when(_vmLogExecutor.get(_restHighLevelClient, _getRequest, Token.class)).thenReturn(TOKEN_ENTITY);
        when(_vmInstanceDao.findByUuid(TOKEN_ENTITY.getVmUuid())).thenReturn(null);

        _vmLogManager.invalidateToken(TOKEN_ENTITY.getToken());
    }

    @Test
    public void testInvalidateTokenPermissionDenied() throws IOException {
        PermissionDeniedException exception = new PermissionDeniedException("VM");
        setExceptionExpectation(exception.getClass(), exception.getMessage());

        when(_vmLogRequestBuilder.getGetTokenRequest(TOKEN_ENTITY.getToken())).thenReturn(_getRequest);
        when(_vmLogExecutor.get(_restHighLevelClient, _getRequest, Token.class)).thenReturn(TOKEN_ENTITY);
        when(_vmInstanceDao.findByUuid(TOKEN_ENTITY.getVmUuid())).thenReturn(_vmInstanceVO);
        doThrow(exception).when(_accountManager).checkAccess(_callerAccount, SecurityChecker.AccessType.OperateEntry, false, _vmInstanceVO);

        CallContext.register(_callerUser, _callerAccount);

        _vmLogManager.invalidateToken(TOKEN_ENTITY.getToken());
    }

    @Test
    public void testInvalidateTokenUpdateRequestException() throws IOException {
        setExceptionExpectation(ServerApiException.class, "VM log token");

        when(_vmLogRequestBuilder.getGetTokenRequest(TOKEN_ENTITY.getToken())).thenReturn(_getRequest);
        when(_vmLogExecutor.get(_restHighLevelClient, _getRequest, Token.class)).thenReturn(TOKEN_ENTITY);
        when(_vmInstanceDao.findByUuid(TOKEN_ENTITY.getVmUuid())).thenReturn(_vmInstanceVO);
        doNothing().when(_accountManager).checkAccess(_callerAccount, SecurityChecker.AccessType.OperateEntry, false, _vmInstanceVO);
        when(_vmLogRequestBuilder.getInvalidateTokenRequest(eq(TOKEN_ENTITY.getToken()), any(LocalDateTime.class))).thenReturn(_updateRequest);
        doThrow(new IOException()).when(_vmLogExecutor).update(_restHighLevelClient, _updateRequest);

        CallContext.register(_callerUser, _callerAccount);

        _vmLogManager.invalidateToken(TOKEN_ENTITY.getToken());
    }

    @Test
    public void testInvalidateToken() throws IOException {
        when(_vmLogRequestBuilder.getGetTokenRequest(TOKEN_ENTITY.getToken())).thenReturn(_getRequest);
        when(_vmLogExecutor.get(_restHighLevelClient, _getRequest, Token.class)).thenReturn(TOKEN_ENTITY);
        when(_vmInstanceDao.findByUuid(TOKEN_ENTITY.getVmUuid())).thenReturn(_vmInstanceVO);
        doNothing().when(_accountManager).checkAccess(_callerAccount, SecurityChecker.AccessType.OperateEntry, false, _vmInstanceVO);
        when(_vmLogRequestBuilder.getInvalidateTokenRequest(eq(TOKEN_ENTITY.getToken()), any(LocalDateTime.class))).thenReturn(_updateRequest);
        doNothing().when(_vmLogExecutor).update(_restHighLevelClient, _updateRequest);

        CallContext.register(_callerUser, _callerAccount);

        boolean result = _vmLogManager.invalidateToken(TOKEN_ENTITY.getToken());

        assertTrue(result);
    }

    private void setExceptionExpectation(Class<? extends Exception> exceptionClass, String message) {
        expectedException.expect(exceptionClass);
        expectedException.expectMessage(message);
    }

    private void setVmExpectations() {
        when(_vmInstanceDao.findById(VM_ID)).thenReturn(_vmInstanceVO);
        when(_vmInstanceVO.getUuid()).thenReturn(UUID);
    }
}
