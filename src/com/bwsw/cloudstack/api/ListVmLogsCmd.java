package com.bwsw.cloudstack.api;

import com.bwsw.cloudstack.response.VmLogResponse;
import com.bwsw.cloudstack.vm.logs.VmLogManager;
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
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@APICommand(name = ListVmLogsCmd.API_NAME, description = "Lists VM logs", responseObject = VmLogResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = true,
        responseView = ResponseObject.ResponseView.Full, authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
        entityType = {VirtualMachine.class})
public class ListVmLogsCmd extends BaseListCmd {

    public static final String API_NAME = "listVmLogs";

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
        com.bwsw.cloudstack.response.ListResponse<VmLogResponse> vmLogs = _vmLogManager
                .listVmLogs(getId(), parseDate(getStartDate(), ApiConstants.START_DATE), parseDate(getEndDate(), ApiConstants.END_DATE), getKeywords(), logFile, getPage(),
                        getPageSize());
        ListResponse<VmLogResponse> response = new ListResponse<>();
        response.setResponseName(getCommandName());
        response.setResponses(vmLogs.getItems(), (int)vmLogs.getCount());
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
}
