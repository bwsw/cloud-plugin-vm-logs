package com.bwsw.cloudstack.vm.logs;

import com.bwsw.cloudstack.api.ListVmLogsCmd;
import com.bwsw.cloudstack.response.ListResponse;
import com.bwsw.cloudstack.response.VmLogResponse;
import com.bwsw.cloudstack.vm.logs.util.HttpUtils;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.http.HttpHost;
import org.apache.log4j.Logger;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class VmLogManagerImpl extends ComponentLifecycleBase implements VmLogManager, Configurable {

    private static final Logger s_logger = Logger.getLogger(VmLogManagerImpl.class);

    @Inject
    private VMInstanceDao _vmInstanceDao;

    private RestHighLevelClient _restHighLevelClient;

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> commands = new ArrayList<>();
        commands.add(ListVmLogsCmd.class);
        return commands;
    }

    @Override
    public ListResponse<VmLogResponse> listVmLogs(Long id, Date start, Date end, List<String> keywords, String logFile) {
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

    @Override
    public boolean configure(String name, Map<String, Object> params) {
        try {
            _restHighLevelClient = new RestHighLevelClient(RestClient.builder(HttpUtils.getHttpHosts(VmLogElasticSearchList.value()).toArray(new HttpHost[] {})));
        } catch (IllegalArgumentException e) {
            s_logger.error("Failed to create ElasticSearch client", e);
            return false;
        }
        return true;
    }

    @Override
    public String getConfigComponentName() {
        return VmLogManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {VmLogElasticSearchList};
    }

}
