package com.bwsw.cloudstack.vm.logs;

import org.elasticsearch.action.search.SearchRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface VmLogQueryBuilder {

    SearchRequest getSearchQuery(String vmUuid, int pageSize, LocalDateTime start, LocalDateTime end, List<String> keywords, String logFile);
}
