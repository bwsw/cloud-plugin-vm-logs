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

package com.bwsw.cloudstack.response;

import com.bwsw.cloudstack.vm.logs.VmLogRequestBuilder;
import com.cloud.serializer.Param;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;

public class VmLogFileResponse extends BaseResponse {

    @Param(description = "the log file")
    @JsonAlias(VmLogRequestBuilder.LOG_FILE_FIELD)
    @JsonProperty("file")
    @SerializedName("file")
    private final String file;

    public VmLogFileResponse(String file) {
        super("vmlogfiles");
        this.file = file;
    }

    public String getFile() {
        return file;
    }
}
