package com.thirdapi.admin.service;

public class AppNotFoundException extends RuntimeException {

    public AppNotFoundException(String appId) {
        super("App not found or disabled: " + appId);
    }
}
