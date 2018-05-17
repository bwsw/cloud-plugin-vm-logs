package com.bwsw.cloudstack.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class VmLogListResponse extends ScrollableListResponse<VmLogResponse> {

    @SerializedName("items")
    private VmLogResponse[] itemArray;

    public VmLogListResponse(int count, List<VmLogResponse> items, String scrollId) {
        super(count, items, scrollId);
        this.itemArray = null;
    }

    public VmLogListResponse(int count, VmLogResponse[] items, String scrollId) {
        super(count, null, scrollId);
        this.itemArray = items;

    }

    public VmLogResponse[] getItemArray() {
        return itemArray;
    }
}
