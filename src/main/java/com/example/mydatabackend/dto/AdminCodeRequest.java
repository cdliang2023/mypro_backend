package com.example.mydatabackend.dto;

public class AdminCodeRequest {

    private String adminName;
    private String code;

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}