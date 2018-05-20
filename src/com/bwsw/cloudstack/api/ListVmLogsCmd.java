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

package com.bwsw.cloudstack.api;

import com.bwsw.cloudstack.response.ScrollableListResponse;
import com.bwsw.cloudstack.response.VmLogListResponse;
import com.bwsw.cloudstack.response.VmLogResponse;
import com.bwsw.cloudstack.vm.logs.VmLogManager;
import com.bwsw.cloudstack.vm.logs.util.ParameterUtils;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;
import com.google.common.base.Strings;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.UserVmResponse;

import javax.inject.Inject;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@APICommand(name = ListVmLogsCmd.API_NAME, description = "Gets VM logs", responseObject = ScrollableListResponse.class, requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        responseView = ResponseObject.ResponseView.Full, authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
        entityType = {VirtualMachine.class})
public class ListVmLogsCmd extends BaseCmd {

    public static final String API_NAME = "getVmLogs";
    private static final String SEARCH_AFTER_PARAM = "searchafter";

    @ACL(accessType = SecurityChecker.AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = UserVmResponse.class, required = true, description = "the ID of the virtual machine")
    private Long id;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.STRING, description = "the start date/time to search VM logs in UTC, yyyy-MM-dd'T'HH:mm:ss")
    private String startDate;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.STRING, description = "the end date/time to search VM logs in UTC, yyyy-MM-dd'T'HH:mm:ss")
    private String endDate;

    @Parameter(name = "keywords", type = CommandType.LIST, collectionType = CommandType.STRING, description = "keywords to search VM logs")
    private List<String> keywords;

    @Parameter(name = "logFile", type = CommandType.STRING, description = "the log file to search VM logs")
    private String logFile;

    @Parameter(name = ApiConstants.PAGE, type = CommandType.INTEGER)
    private Integer page;

    @Parameter(name = ApiConstants.PAGE_SIZE, type = CommandType.INTEGER)
    private Integer pageSize;

    @Parameter(name = SEARCH_AFTER_PARAM, type = CommandType.STRING, description = "the scroll id to retrieve next log page")
    private String searchAfter;

    @Inject
    private VmLogManager _vmLogManager;

    public Long getId() {
        return id;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public String getLogFile() {
        return logFile;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public String getSearchAfter() {
        return searchAfter;
    }

    @Override
    public long getEntityOwnerId() {
        UserVm vm = _responseGenerator.findUserVmById(getId());
        if (vm != null) {
            return vm.getAccountId();
        }

        // no account info given, parent this command to SYSTEM so ERROR events are tracked
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        ScrollableListResponse<VmLogResponse> listResponse = _vmLogManager
                .listVmLogs(getId(), parseDate(getStartDate(), ApiConstants.START_DATE), parseDate(getEndDate(), ApiConstants.END_DATE), getKeywords(), getLogFile(), getPage(),
                        getPageSize(), getSearchAfterValue());
        // recreate the response for serialization to exclude generic type lists
        VmLogListResponse response = new VmLogListResponse(listResponse.getCount(), listResponse.getItems(), listResponse.getSearchAfter());
        response.setResponseName(getCommandName());
        response.setObjectName("vmlogs");
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return API_NAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    private LocalDateTime parseDate(String date, String paramName) {
        if (Strings.isNullOrEmpty(date)) {
            return null;
        }
        try {
            return LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "\"" + paramName + "\" parameter is invalid");
        }
    }

    private Object[] getSearchAfterValue() {
        try {
            return ParameterUtils.readSearchAfter(getSearchAfter());
        } catch (IOException e) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "\"" + SEARCH_AFTER_PARAM + "\" parameter is invalid");
        }
    }
}
