package com.bwsw.cloudstack.response;

import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;

import java.util.List;

public class VmLogListResponse extends BaseResponse {

    @SerializedName("count")
    private final int count;

    @SerializedName("searchafter")
    private final String searchAfter;

    @SerializedName("items")
    private List<VmLogResponse> items;

    public VmLogListResponse(int count, List<VmLogResponse> items, String searchAfter) {
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

    public List<VmLogResponse> getItems() {
        return items;
    }
}
