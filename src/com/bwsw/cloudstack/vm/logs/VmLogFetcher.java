package com.bwsw.cloudstack.vm.logs;

import com.bwsw.cloudstack.response.ListResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;

public interface VmLogFetcher {

    <T> ListResponse<T> fetch(RestHighLevelClient client, SearchRequest request, long page, Class<T> elementClass) throws IOException;
}
