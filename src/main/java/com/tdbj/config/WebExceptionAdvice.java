package com.tdbj.config;

import com.tdbj.Enum.ErrorCode;
import com.tdbj.dto.Result;
import com.tdbj.entity.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class WebExceptionAdvice {

    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        log.error(e.toString(), e);
        return Result.fail("服务器异常");
    }

    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e){
        log.info("BusinessException");
        return Result.fail(e.getMessage());
    }
}
