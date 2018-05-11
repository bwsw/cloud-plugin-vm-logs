package com.bwsw.cloudstack.vm.logs;

import com.bwsw.cloudstack.response.ListResponse;
import com.bwsw.cloudstack.response.VmLogResponse;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.util.Date;
import java.util.List;

public interface VmLogManager extends PluggableService {

    ConfigKey<String> VmLogElasticSearchList = new ConfigKey<>("Advanced", String.class, "vm.log.elasticsearch.list", null,
            "Comma separated list of ElasticSearch HTTP hosts; e.g. http://localhost,http://localhost:9201", false);

    ListResponse<VmLogResponse> listVmLogs(Long id, Date start, Date end, List<String> keywords);

    void deleteVmLogs(String uuid);
}
