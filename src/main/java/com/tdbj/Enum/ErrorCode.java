package com.tdbj.Enum;

public enum ErrorCode {
    NO_AUTH_ERROR(40101, "无权限"),;
    private int code;
    private String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
