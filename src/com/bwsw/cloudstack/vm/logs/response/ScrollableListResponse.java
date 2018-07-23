// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.bwsw.cloudstack.vm.logs.response;

import java.util.List;

public class ScrollableListResponse<T> {

    private final int count;
    private final String scrollId;
    private List<T> items;

    public ScrollableListResponse(int count, List<T> items, String scrollId) {
        this.count = count;
        this.items = items;
        this.scrollId = scrollId;
    }

    public int getCount() {
        return count;
    }

    public String getScrollId() {
        return scrollId;
    }

    public List<T> getItems() {
        return items;
    }
}
