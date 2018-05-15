package com.bwsw.cloudstack.response;

import java.util.List;

public class ListResponse<T> {

    private final long count;
    private final List<T> items;

    public ListResponse(long count, List<T> items) {
        this.count = count;
        this.items = items;
    }

    public long getCount() {
        return count;
    }

    public List<T> getItems() {
        return items;
    }
}
