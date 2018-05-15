package com.bwsw.cloudstack.response;

import com.bwsw.cloudstack.vm.logs.VmLogRequestBuilder;
import com.cloud.serializer.Param;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.cloudstack.api.BaseResponse;

public class VmLogResponse extends BaseResponse {

    @Param(description = "the log event timestamp")
    @JsonAlias(VmLogRequestBuilder.DATE_FIELD)
    @JsonProperty("timestamp")
    private String timestamp;

    @Param(description = "the log file")
    @JsonAlias(VmLogRequestBuilder.LOG_FILE_FIELD)
    @JsonProperty("file")
    private String logFile;

    @Param(description = "the log data")
    @JsonAlias(VmLogRequestBuilder.DATA_FIELD)
    @JsonProperty("log")
    private String log;

    public VmLogResponse() {
        super("vmlogs");
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getLogFile() {
        return logFile;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }
}
