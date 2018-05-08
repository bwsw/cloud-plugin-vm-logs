package com.bwsw.cloudstack.response;

import com.cloud.serializer.Param;
import org.apache.cloudstack.api.BaseResponse;

public class VmLogResponse extends BaseResponse {

    @Param(description = "the log data")
    private String log;

    public VmLogResponse() {
        super("vmlogs");
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }
}
