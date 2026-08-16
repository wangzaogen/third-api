package com.thirdapi.admin.dto;

public class PublishResponse {

    private String appId;
    private long version;
    private boolean success;

    public PublishResponse() {
    }

    public PublishResponse(String appId, long version, boolean success) {
        this.appId = appId;
        this.version = version;
        this.success = success;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
