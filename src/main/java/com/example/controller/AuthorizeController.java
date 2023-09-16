package com.example.controller;

import com.example.entity.rest.RestBean;
import com.example.entity.vo.request.ConfirmResetVo;
import com.example.entity.vo.request.EmailRegisterVo;
import com.example.entity.vo.request.EmailResetVo;
import com.example.service.AccountService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author blbyd_li
 * @data 2023/9/13
 * @apiNote
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthorizeController {
    @Resource
    private AccountService accountService;

    /**
     * 根据邮箱地址和类型获取验证码接口
     * @param email 邮箱地址
     * @param type 请求类型 register|reset
     * @param request http请求
     * @return 响应结果
     */
    @GetMapping("/ask-code")
    public RestBean<Void> askVerifyCode(@RequestParam @Email String email,
                                        @RequestParam @Pattern(regexp = "(register|reset)") String type,
                                        HttpServletRequest request){
       /* String message = accountService.registerEmailVerifyCode(type,email,request.getRemoteAddr());
         log.info(type + ":" + email + ":" + request.getRemoteAddr());
        return message == null ? RestBean.success() : RestBean.failure(400,message);*/
        return this.messageHandle(()->accountService.registerEmailVerifyCode(type,email,request.getRemoteAddr()));
    }

    /**
     * 注册接口
     * @param vo 注册表单实体
     * @return 响应结果
     */
    @PostMapping("/register")
    public RestBean<Void> registrationEmail(@RequestBody EmailRegisterVo vo){
       return this.messageHandle(vo,accountService::registerEmailAccount);
    }

    /**
     * 验证验证码是否正确
     * @param vo 表单实体
     * @return 响应结果
     */
    @PostMapping("/rest-confirm")
    public RestBean<Void> restConfirm(@RequestBody @Valid ConfirmResetVo vo){
        return this.messageHandle(vo,accountService::resetConfirm);
    }

    /**
     * 重置密码
     * @param vo 表单实体
     * @return 响应结果
     */
    @PostMapping("/rest-password")
    public RestBean<Void> restPassword(@RequestBody @Valid EmailResetVo vo){
        return this.messageHandle(vo,accountService::resetEmailAccountPassword);
    }



    private RestBean<Void>  messageHandle(Supplier<String> action){
        String message = action.get();
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }
    private <T> RestBean<Void> messageHandle(T vo, Function<T,String> function){
        return messageHandle(()->function.apply(vo));
    }
}
