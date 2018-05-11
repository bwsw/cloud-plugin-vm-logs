package com.bwsw.cloudstack.api;

import com.bwsw.cloudstack.response.VmLogResponse;
import com.bwsw.cloudstack.vm.logs.VmLogManager;
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
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

@APICommand(name = ListVmLogsCmd.API_NAME, description = "Lists VM logs", responseObject = VmLogResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = true,
        responseView = ResponseObject.ResponseView.Full, authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
        entityType = {VirtualMachine.class})
public class ListVmLogsCmd extends BaseListCmd {

    public static final String API_NAME = "listVmLogs";

    @ACL(accessType = SecurityChecker.AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = UserVmResponse.class, required = true, description = "the ID of the virtual machine")
    private Long id;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.TZDATE, description = "the start date to search VM logs")
    private Date startDate;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.TZDATE, description = "the end date to search VM logs")
    private Date endDate;

    @Parameter(name = "keywords", type = CommandType.LIST, collectionType = CommandType.STRING, description = "keywords to search VM logs")
    private List<String> keywords;

    @Inject
    private VmLogManager _vmLogManager;

    public Long getId() {
        return id;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public List<String> getKeywords() {
        return keywords;
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
        com.bwsw.cloudstack.response.ListResponse<VmLogResponse> vmLogs = _vmLogManager.listVmLogs(getId(), getStartDate(), getEndDate(), getKeywords());
        ListResponse<VmLogResponse> response = new ListResponse<>();
        response.setResponseName(getCommandName());
        response.setObjectName("logs");
        response.setResponses(vmLogs.getItems(), vmLogs.getCount());
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return API_NAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }
}
