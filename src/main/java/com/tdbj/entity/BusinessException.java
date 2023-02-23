package com.tdbj.entity;

import com.tdbj.Enum.ErrorCode;

public class BusinessException  extends RuntimeException{
    private final int code;

    public BusinessException(int code) {
        this.code = code;
    }

    public BusinessException(int code,String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode){
        this.code=errorCode.getCode();
    }

    public int getCode(){
        return code;
    }

}
