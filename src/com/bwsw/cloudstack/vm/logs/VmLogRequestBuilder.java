package com.bwsw.cloudstack.vm.logs;

import org.elasticsearch.action.search.SearchRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface VmLogRequestBuilder {

    String DATE_FIELD = "@timestamp";
    String LOG_FILE_FIELD = "source";
    String DATA_FIELD = "message";

    SearchRequest getLogSearchRequest(String vmUuid, int page, int pageSize, Object[] searchAfter, LocalDateTime start, LocalDateTime end, List<String> keywords, String logFile);
}
