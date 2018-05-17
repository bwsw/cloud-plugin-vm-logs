package com.bwsw.cloudstack.response;

import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.ResponseObject;

import java.util.List;

public class ScrollableListResponse<T extends ResponseObject> extends BaseResponse {

    @SerializedName("count")
    private final int count;

    @SerializedName("searchafter")
    private final String searchAfter;

    @SerializedName("items")
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
