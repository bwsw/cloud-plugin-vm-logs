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

package com.bwsw.cloudstack.vm.logs.api;

import com.bwsw.cloudstack.vm.logs.response.ScrollableListResponse;
import com.bwsw.cloudstack.vm.logs.response.VmLogListResponse;
import com.bwsw.cloudstack.vm.logs.response.VmLogResponse;
import com.bwsw.cloudstack.vm.logs.service.VmLogManager;
import com.bwsw.cloudstack.vm.logs.util.ParameterUtils;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.UserVmResponse;

import javax.inject.Inject;
import java.util.List;

@APICommand(name = GetVmLogsCmd.API_NAME, description = "Gets VM logs", responseObject = VmLogListResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = true,
        responseView = ResponseObject.ResponseView.Full, authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
        entityType = {VirtualMachine.class})
public class GetVmLogsCmd extends BaseCmd {

    public static final String API_NAME = "getVmLogs";

    @ACL(accessType = SecurityChecker.AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = UserVmResponse.class, required = true, description = "the ID of the virtual machine")
    private Long id;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.STRING, description = "the start date/time to search VM logs in UTC, yyyy-MM-dd'T'HH:mm:ss")
    private String startDate;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.STRING, description = "the end date/time to search VM logs in UTC, yyyy-MM-dd'T'HH:mm:ss")
    private String endDate;

    @Parameter(name = "keywords", type = CommandType.LIST, collectionType = CommandType.STRING, description = "keywords to search VM logs")
    private List<String> keywords;

    @Parameter(name = "logfile", type = CommandType.STRING, description = "the log file to search VM logs")
    private String logFile;

    @Parameter(name = "sort", type = CommandType.LIST, collectionType = CommandType.STRING, description = "response fields to sort VM logs (the prefix - for descending order)")
    private List<String> sortFields;

    @Parameter(name = ApiConstants.PAGE, type = CommandType.INTEGER)
    private Integer page;

    @Parameter(name = ApiConstants.PAGE_SIZE, type = CommandType.INTEGER)
    private Integer pageSize;

    @Parameter(name = "scroll", type = CommandType.INTEGER, description = "timeout in ms for subsequent scroll requests")
    private Integer scroll;

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

    public List<String> getSortFields() {
        return sortFields;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public Integer getScroll() {
        return scroll;
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
                .listVmLogs(getId(), ParameterUtils.parseDate(getStartDate(), ApiConstants.START_DATE), ParameterUtils.parseDate(getEndDate(), ApiConstants.END_DATE),
                        getKeywords(), getLogFile(), getSortFields(), getPage(), getPageSize(), getScroll());
        // recreate the response for serialization to exclude generic type lists
        VmLogListResponse response = new VmLogListResponse(listResponse.getCount(), listResponse.getItems(), listResponse.getScrollId());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return API_NAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }
}
