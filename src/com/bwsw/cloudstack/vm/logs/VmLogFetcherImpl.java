package com.bwsw.cloudstack.vm.logs;

import com.bwsw.cloudstack.response.ListResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VmLogFetcherImpl implements VmLogFetcher {

    @Inject
    private VmLogRequestBuilder _vmLogRequestBuilder;

    private final ObjectMapper _objectMapper = new ObjectMapper();

    @Override
    public <T> ListResponse<T> fetch(RestHighLevelClient client, SearchRequest request, long page, Class<T> elementClass) throws IOException {
        SearchResponse response = client.search(request);
        if (response.status() != RestStatus.OK || response.getHits() == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to retrieve VM logs");
        }
        if (page > 0) {
            int currentPage = 1;
            do {
                currentPage++;
                response = client.searchScroll(_vmLogRequestBuilder.getSearchScrollRequest(response.getScrollId()));
            } while (currentPage < page && response.getHits().getHits().length != 0);
        }
        return new ListResponse<>(response.getHits().getTotalHits(), parseResults(response, elementClass));
    }

    private <T> List<T> parseResults(SearchResponse response, Class<T> elementClass) throws IOException {
        List<T> results = new ArrayList<>();
        for (SearchHit searchHit : response.getHits()) {
            results.add(_objectMapper.readValue(searchHit.getSourceAsString(), elementClass));
        }
        return results;
    }

}
