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

import com.bwsw.cloudstack.vm.logs.response.InvalidationResponse;
import com.bwsw.cloudstack.vm.logs.service.VmLogManager;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;

import javax.inject.Inject;

@APICommand(name = InvalidateVmLogTokenCmd.API_NAME, description = "Invalidates VM log token", responseObject = InvalidationResponse.class, requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true, responseView = ResponseObject.ResponseView.Full,
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class InvalidateVmLogTokenCmd extends BaseCmd {

    public static final String API_NAME = "invalidateVmLogToken";

    @Parameter(name = "token", type = CommandType.STRING, required = true, description = "token to be invalidated")
    private String token;

    @Inject
    private VmLogManager _vmLogManager;

    public String getToken() {
        return token;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {
        boolean result = _vmLogManager.invalidateToken(getToken());
        InvalidationResponse response = new InvalidationResponse();
        response.setSuccess(result);
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
