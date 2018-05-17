package com.bwsw.cloudstack.vm.logs;

import com.bwsw.cloudstack.response.ScrollableListResponse;
import org.apache.cloudstack.api.ResponseObject;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;

public interface VmLogFetcher {

    <T extends ResponseObject> ScrollableListResponse<T> fetch(RestHighLevelClient client, SearchRequest request, Class<T> elementClass, boolean scroll) throws IOException;
}
