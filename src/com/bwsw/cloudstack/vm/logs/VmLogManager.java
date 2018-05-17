package com.bwsw.cloudstack.vm.logs;

import com.bwsw.cloudstack.response.ScrollableListResponse;
import com.bwsw.cloudstack.response.VmLogResponse;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.time.LocalDateTime;
import java.util.List;

public interface VmLogManager extends PluggableService {

    ConfigKey<String> VmLogElasticSearchList = new ConfigKey<>("Advanced", String.class, "vm.log.elasticsearch.list", null,
            "Comma separated list of ElasticSearch HTTP hosts; e.g. http://localhost,http://localhost:9201", false);

    ConfigKey<Integer> VmLogDefaultPageSize = new ConfigKey<>("Advanced", Integer.class, "vm.log.page.size.default", "100", "Default page size for VM log listing", true);

    ConfigKey<Integer> VmLogMaxPageSize = new ConfigKey<>("Advanced", Integer.class, "vm.log.page.size.max", "1000", "Maximum page size for VM log listing", true);

    ScrollableListResponse<VmLogResponse> listVmLogs(Long id, LocalDateTime start, LocalDateTime end, List<String> keywords, String logFile, Integer page, Integer pageSize,
            Object[] searchAfter);

    void deleteVmLogs(String uuid);
}
