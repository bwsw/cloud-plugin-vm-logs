package com.bwsw.cloudstack.response;

import com.bwsw.cloudstack.vm.logs.VmLogRequestBuilder;
import com.cloud.serializer.Param;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;

public class VmLogResponse extends BaseResponse {

    @Param(description = "the log event timestamp")
    @JsonAlias(VmLogRequestBuilder.DATE_FIELD)
    @JsonProperty("timestamp")
    @SerializedName("timestamp")
    private String timestamp;

    @Param(description = "the log file")
    @JsonAlias(VmLogRequestBuilder.LOG_FILE_FIELD)
    @JsonProperty("file")
    @SerializedName("file")
    private String file;

    @Param(description = "the log data")
    @JsonAlias(VmLogRequestBuilder.DATA_FIELD)
    @JsonProperty("log")
    @SerializedName("log")
    private String log;

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }
}
