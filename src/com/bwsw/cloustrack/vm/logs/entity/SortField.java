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

package com.bwsw.cloustrack.vm.logs.entity;

import com.google.common.base.Strings;

public class SortField {

    public enum SortOrder {
        ASC(""), DESC("-");

        private final String prefix;

        SortOrder(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return Strings.nullToEmpty(prefix);
        }
    }

    private final String field;
    private final SortOrder order;

    public SortField(String field, SortOrder order) {
        this.field = field;
        this.order = order;
    }

    public String getField() {
        return field;
    }

    public SortOrder getOrder() {
        return order;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((field == null) ? 0 : field.hashCode());
        result = prime * result + ((order == null) ? 0 : order.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        SortField other = (SortField)obj;
        if (field == null) {
            if (other.field != null) {
                return false;
            }
        } else {
            if (!field.equals(other.field)) {
                return false;
            }
        }
        return order == other.order;
    }
}
