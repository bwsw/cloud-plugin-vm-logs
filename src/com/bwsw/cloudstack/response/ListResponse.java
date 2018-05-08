package com.bwsw.cloudstack.response;

import java.util.List;

public class ListResponse<T> {

    private final int count;
    private final List<T> items;

    public ListResponse(int count, List<T> items) {
        this.count = count;
        this.items = items;
    }

    public int getCount() {
        return count;
    }

    public List<T> getItems() {
        return items;
    }
}
