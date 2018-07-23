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
import java.util.Map;

public class AggregateResponse<T> {

    private final List<T> items;
    private final int count;
    private final Map<String, Object> searchAfter;

    public AggregateResponse(List<T> items, int count, Map<String, Object> searchAfter) {
        this.items = items;
        this.count = count;
        this.searchAfter = searchAfter;
    }

    public List<T> getItems() {
        return items;
    }

    public int getCount() {
        return count;
    }

    public Map<String, Object> getSearchAfter() {
        return searchAfter;
    }
}
