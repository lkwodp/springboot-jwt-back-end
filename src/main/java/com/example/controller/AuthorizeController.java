package com.example.controller;

import com.example.entity.rest.RestBean;
import com.example.entity.vo.request.EmailRegisterVo;
import com.example.service.AccountService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/ask-code")
    public RestBean<Void> askVerifyCode(@RequestParam @Email String email,
                                        @RequestParam @Pattern(regexp = "(register|reset)") String type,
                                        HttpServletRequest request){
       /* String message = accountService.registerEmailVerifyCode(type,email,request.getRemoteAddr());
         log.info(type + ":" + email + ":" + request.getRemoteAddr());
        return message == null ? RestBean.success() : RestBean.failure(400,message);*/
        return this.messageHandle(()->accountService.registerEmailVerifyCode(type,email,request.getRemoteAddr()));
    }

    @PostMapping("/register")
    public RestBean<Void> registrationEmail(@RequestBody EmailRegisterVo vo){
       return this.messageHandle(()-> accountService.registerEmailAccount(vo));
    }

    private RestBean<Void>  messageHandle(Supplier<String> action){
        String message = action.get();
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }
}
