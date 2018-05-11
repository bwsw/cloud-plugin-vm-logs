package com.bwsw.cloudstack.vm.logs;

import com.bwsw.cloudstack.api.ListVmLogsCmd;
import com.bwsw.cloudstack.response.ListResponse;
import com.bwsw.cloudstack.response.VmLogResponse;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class VmLogServiceImpl implements VmLogService {

    private static final Logger s_logger = Logger.getLogger(VmLogServiceImpl.class);

    @Inject
    private VMInstanceDao _vmInstanceDao;

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> commands = new ArrayList<>();
        commands.add(ListVmLogsCmd.class);
        return commands;
    }

    @Override
    public ListResponse<VmLogResponse> listVmLogs(Long id, Date start, Date end, List<String> keywords) {
        List<VmLogResponse> logs;

        VMInstanceVO vmInstanceVO = _vmInstanceDao.findById(id);
        if (vmInstanceVO != null) {
            // TODO: retrieve logs
            logs = Collections.emptyList();
        } else {
            logs = Collections.emptyList();
        }
        return new ListResponse<>(logs.size(), logs);
    }

    @Override
    public void deleteVmLogs(String uuid) {
        VMInstanceVO vmInstanceVO = _vmInstanceDao.findByUuidIncludingRemoved(uuid);
        if (vmInstanceVO != null) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Deleting logs for VM " + uuid);
                // TODO: delete logs
            }
        } else {
            s_logger.error("Can not delete logs for unknown VM " + uuid);
        }
    }
}
