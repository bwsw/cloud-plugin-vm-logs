package com.bwsw.cloudstack.vm.logs;

import com.bwsw.cloudstack.response.ListResponse;
import com.bwsw.cloudstack.response.VmLogResponse;
import com.cloud.utils.component.PluggableService;

import java.util.Date;
import java.util.List;

public interface VmLogService extends PluggableService {

    ListResponse<VmLogResponse> listVmLogs(Long id, Date start, Date end, List<String> keywords);

    void deleteVmLogs(String uuid);
}
