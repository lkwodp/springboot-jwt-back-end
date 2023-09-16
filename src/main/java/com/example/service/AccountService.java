package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.Account;
import com.example.entity.vo.request.ConfirmResetVo;
import com.example.entity.vo.request.EmailRegisterVo;
import com.example.entity.vo.request.EmailResetVo;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * AccountService
 * @author blbyd_li
 * @data 2023/9/10
 * @apiNote
 */
public interface AccountService extends IService<Account> , UserDetailsService {
    /**
     * 根据用户名或者邮箱地址查找用户
     * @param conditions 用户名或者邮箱地址
     * @return 用户
     */
    Account findAccountByUsernameOrEmail(String conditions);
    /**
     * 发送注册邮件验证码
     * @param type 邮件类型
     * @param email 目标邮箱
     * @param ip IP地址
     * @return null：成功发送 else：对应的错误信息
     */
    String registerEmailVerifyCode(String type,String email,String ip);
    /**
     * 注册用户，根据提交的对象实体注册用户
     * @param emailRegisterVo 对象实体vo
     * @return null：注册用户成功  else：具体的失败原因
     */
    String registerEmailAccount(EmailRegisterVo emailRegisterVo);
    /**
     * 根据请求实体的数据对象vo，判断验证码是否正确
     * @param vo 数据对象vo
     * @return null：验证码正确  else：具体错误原因
     */
    String resetConfirm(ConfirmResetVo vo);
    /**
     * 根据请求实体对象，进行重置密码
     * @param vo 对象实体vo
     * @return null：正确重置密码 else：具体错误原因
     */
    String resetEmailAccountPassword(EmailResetVo vo);
}
