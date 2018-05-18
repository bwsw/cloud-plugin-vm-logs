package com.bwsw.cloudstack.response;

import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.ResponseObject;

import java.util.List;

public class ScrollableListResponse<T extends ResponseObject> extends BaseResponse {

    private final int count;
    private final String searchAfter;
    private List<T> items;

    public ScrollableListResponse(int count, List<T> items, String searchAfter) {
        this.count = count;
        this.items = items;
        this.searchAfter = searchAfter;
    }

    public int getCount() {
        return count;
    }

    public String getSearchAfter() {
        return searchAfter;
    }

    public List<T> getItems() {
        return items;
    }
}
