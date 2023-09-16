package com.example.controller.exception;

import com.example.entity.rest.RestBean;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 处理validateException异常，返回标准数据
 * @author blbyd_li
 * @data 2023/9/15
 * @apiNote
 */
@Slf4j
@RestControllerAdvice
public class ValidationController {
    @ExceptionHandler(ValidationException.class)
    public RestBean<Void> validateException(ValidationException e){
        log.warn("Resolve[{} : {}]",e.getClass().getName(),e.getMessage());
        return RestBean.failure(400,"请求参数有误");
    }
}
