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
import com.bwsw.cloudstack.vm.logs.response.AggregateResponse;
import com.bwsw.cloudstack.vm.logs.response.ScrollableListResponse;
import com.bwsw.cloudstack.vm.logs.response.VmLogFileResponse;
import com.bwsw.cloudstack.vm.logs.response.VmLogResponse;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.collect.ImmutableList;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
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

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private VMInstanceDao _vmInstanceDao;

    @Mock
    private RestHighLevelClient _restHighLevelClient;

    @Mock
    private VmLogRequestBuilder _vmLogRequestBuilder;

    @Mock
    private VmLogFetcher _vmLogFetcher;

    @Mock
    private SearchScrollRequest _searchScrollRequest;

    @Mock
    private VMInstanceVO _vmInstanceVO;

    @InjectMocks
    private VmLogManagerImpl _vmLogManager = new VmLogManagerImpl();

    private ScrollableListResponse<VmLogResponse> _emptyVmLogResponse = new ScrollableListResponse<>(0, null, SCROLL_ID);

    private SearchRequest _searchRequest = new SearchRequest();

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
        when(_vmLogFetcher.fetch(_restHighLevelClient, _searchRequest, VmLogResponse.class)).thenThrow(new IOException());

        _vmLogManager.listVmLogs(VM_ID, null, null, null, null, null, PAGE, PAGE_SIZE, null);
    }

    @Test
    public void testListVmLogsDefaultValues() throws IOException {
        setVmExpectations();
        when(_vmLogRequestBuilder.getLogSearchRequest(UUID, 1, VmLogManager.VmLogDefaultPageSize.value(), null, null, null, null, null, null)).thenReturn(_searchRequest);
        when(_vmLogFetcher.fetch(_restHighLevelClient, _searchRequest, VmLogResponse.class)).thenReturn(_emptyVmLogResponse);

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
        when(_vmLogFetcher.fetch(_restHighLevelClient, _searchRequest, VmLogResponse.class)).thenReturn(_emptyVmLogResponse);

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
        when(_vmLogFetcher.scroll(_restHighLevelClient, _searchScrollRequest, VmLogResponse.class)).thenThrow(new IOException());

        _vmLogManager.scrollVmLogs(SCROLL_ID, TIMEOUT);
    }

    @Test
    public void testScrollVmLogs() throws IOException {
        when(_vmLogRequestBuilder.getScrollRequest(SCROLL_ID, TIMEOUT)).thenReturn(_searchScrollRequest);
        when(_vmLogFetcher.scroll(_restHighLevelClient, _searchScrollRequest, VmLogResponse.class)).thenReturn(_emptyVmLogResponse);

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
        when(_vmLogFetcher.fetchLogFiles(_restHighLevelClient, _searchRequest)).thenThrow(new IOException());

        _vmLogManager.listVmLogFiles(VM_ID, null, null, START_INDEX, PAGE_SIZE_ONE);
    }

    @Test
    public void testListVmLogFilesEmptyResults() throws IOException {
        setVmExpectations();
        when(_vmLogRequestBuilder.getLogFileSearchRequest(UUID, (int)PAGE_SIZE_ONE, null, null, null)).thenReturn(_searchRequest);
        when(_vmLogFetcher.fetchLogFiles(_restHighLevelClient, _searchRequest)).thenReturn(new AggregateResponse<>(null, 0, null));

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
        when(_vmLogFetcher.fetchLogFiles(_restHighLevelClient, _searchRequest)).thenReturn(new AggregateResponse<>(firstPageResults, count, searchAfter))
                .thenReturn(new AggregateResponse<>(secondPageResults, count, null));

        ListResponse<VmLogFileResponse> result = _vmLogManager.listVmLogFiles(VM_ID, DATE_TIME, end, PAGE_SIZE_ONE, PAGE_SIZE_ONE);

        assertNotNull(result);
        assertNotNull(result.getCount());
        assertEquals(count, result.getCount().intValue());
        assertEquals(secondPageResults, result.getResponses());
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
