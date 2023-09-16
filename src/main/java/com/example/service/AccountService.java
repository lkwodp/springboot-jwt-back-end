package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.Account;
import com.example.entity.vo.request.EmailRegisterVo;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * @author blbyd_li
 * @data 2023/9/10
 * @apiNote
 */
public interface AccountService extends IService<Account> , UserDetailsService {
    Account findAccountByUsernameOrEmail(String conditions);

    String registerEmailVerifyCode(String type,String email,String ip);

    String registerEmailAccount(EmailRegisterVo emailRegisterVo);
}
