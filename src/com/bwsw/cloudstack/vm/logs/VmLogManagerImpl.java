package com.bwsw.cloudstack.vm.logs;

import com.bwsw.cloudstack.api.GetVmLogsCmd;
import com.bwsw.cloudstack.response.ScrollableListResponse;
import com.bwsw.cloudstack.response.VmLogResponse;
import com.bwsw.cloudstack.vm.logs.util.HttpUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.http.HttpHost;
import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VmLogManagerImpl extends ComponentLifecycleBase implements VmLogManager, Configurable {

    private static final Logger s_logger = Logger.getLogger(VmLogManagerImpl.class);

    @Inject
    private VMInstanceDao _vmInstanceDao;

    @Inject
    private VmLogRequestBuilder _vmLogRequestBuilder;

    @Inject
    private VmLogFetcher _vmLogFetcher;

    private RestHighLevelClient _restHighLevelClient;

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> commands = new ArrayList<>();
        commands.add(GetVmLogsCmd.class);
        return commands;
    }

    @Override
    public ScrollableListResponse<VmLogResponse> listVmLogs(Long id, LocalDateTime start, LocalDateTime end, List<String> keywords, String logFile, Integer page, Integer pageSize,
            Object[] searchAfter) {
        if (pageSize == null) {
            pageSize = VmLogDefaultPageSize.value();
        }
        if (pageSize == null || pageSize < 1) {
            throw new InvalidParameterValueException("Invalid page size");
        }
        if (page == null) {
            page = 1;
        } else if (page < 1) {
            throw new InvalidParameterValueException("Invalid page");
        }
        if (start != null && end != null && end.isBefore(start)) {
            throw new InvalidParameterValueException("Invalid start/end dates");
        }

        VMInstanceVO vmInstanceVO = _vmInstanceDao.findById(id);
        if (vmInstanceVO == null) {
            throw new InvalidParameterValueException("Unable to find a virtual machine with specified id");
        }
        SearchRequest searchRequest = _vmLogRequestBuilder.getLogSearchRequest(vmInstanceVO.getUuid(), page, pageSize, searchAfter, start, end, keywords, logFile);
        try {
            return _vmLogFetcher.fetch(_restHighLevelClient, searchRequest, VmLogResponse.class, true);
        } catch (Exception e) {
            s_logger.error("Unable to retrieve VM logs", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to retrieve VM logs");
        }
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
        return new ConfigKey<?>[] {VmLogElasticSearchList, VmLogDefaultPageSize};
    }

}
