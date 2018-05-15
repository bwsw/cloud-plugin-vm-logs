package com.bwsw.cloudstack.vm.logs;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchScrollRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface VmLogRequestBuilder {

    String DATE_FIELD = "@timestamp";
    String LOG_FILE_FIELD = "source";
    String DATA_FIELD = "message";

    SearchRequest getLogSearchRequest(String vmUuid, int pageSize, LocalDateTime start, LocalDateTime end, List<String> keywords, String logFile);

    SearchScrollRequest getSearchScrollRequest(String scrollId);
}
