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
import com.cloud.exception.ConcurrentOperationException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;

import javax.inject.Inject;

@APICommand(name = ScrollVmLogsCmd.API_NAME, description = "Retrieves next batch of VM logs", responseObject = VmLogListResponse.class, requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true, responseView = ResponseObject.ResponseView.Full,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ScrollVmLogsCmd extends BaseCmd {

    public static final String API_NAME = "scrollVmLogs";

    @Parameter(name = "scrollid", type = CommandType.STRING, required = true, description = "scroll id")
    private String scrollId;

    @Parameter(name = "timeout", type = CommandType.INTEGER, required = true, description = "timeout in ms for subsequent scroll requests")
    private Integer timeout;

    @Inject
    private VmLogManager _vmLogManager;

    public String getScrollId() {
        return scrollId;
    }

    public Integer getTimeout() {
        return timeout;
    }

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        ScrollableListResponse<VmLogResponse> listResponse = _vmLogManager.scrollVmLogs(getScrollId(), getTimeout());
        // recreate the response for serialization to exclude generic type lists
        VmLogListResponse response = new VmLogListResponse(listResponse.getCount(), listResponse.getItems(), listResponse.getScrollId());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return API_NAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getAccountId();
    }
}
