package com.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.Account;
import com.example.entity.vo.request.EmailRegisterVo;
import com.example.mapper.AccountMapper;
import com.example.service.AccountService;
import com.example.utils.Const;
import com.example.utils.FlowUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author blbyd_li
 * @data 2023/9/10
 * @apiNote
 */
@Slf4j
@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {

    @Resource
    private AmqpTemplate amqpTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private FlowUtils flowUtils;
    @Resource
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = this.findAccountByUsernameOrEmail(username);
        if (account == null)
            throw new UsernameNotFoundException("用户名或者密码错误");
        return User
                .withUsername(username)
                .password(account.getPassword())
                .roles(account.getRole())
                .build();
    }

    @Override
    public Account findAccountByUsernameOrEmail(String conditions){
        return this.query()
                .eq("username",conditions).or()
                .eq("email",conditions)
                .one();
    }

    /**
     * 发送注册邮件验证码
     * @param type 邮件类型
     * @param email 目标邮箱
     * @param ip IP地址
     * @return null：成功发送
     */
    @Override
    public String registerEmailVerifyCode(String type, String email, String ip) {
        //加锁
        synchronized (ip.intern()){
            if (!this.verifyLimit(ip))
                return "请求过于频繁，请稍后再试";
            //生成6位数验证码
            Random random = new Random();
            int code =  random.nextInt(899999) + 100000;
            //数据
            Map<String,Object> data = Map.of("type",type,"email",email,"code",code);
            log.info(data.toString());
            //放入消息队列
            amqpTemplate.convertAndSend("mail",data);
            //存入Redis中
            stringRedisTemplate.opsForValue()
                    .set(Const.VERIFY_EMAIL_DATA + email,String.valueOf(code),3, TimeUnit.MINUTES);
            return null;
        }
    }
    /**
     * 限制60秒内不能再发验证码
     * @param ip IP地址
     * @return 是否通过检查
     */
    private boolean verifyLimit(String ip){
        String key = Const.VERIFY_EMAIL_LIMIT + ip;
        return flowUtils.limitOnceCheck(key,60);
    }

    @Override
    public String registerEmailAccount(EmailRegisterVo emailRegisterVo) {
        String email = emailRegisterVo.getEmail();
        String username = emailRegisterVo.getUsername();
        String key = Const.VERIFY_EMAIL_DATA + email;
        String code = stringRedisTemplate.opsForValue().get(key);
        if (code == null) return "请先获取验证码";
        if(!code.equals(emailRegisterVo.getCode())) return "验证码输入错误，请重新输入";
        if (this.existsAccountByEmail(email)) return "此电子邮件已被其他用户注册";
        if (this.existsAccountByUsername(username)) return "此用户名已存在，请更换用户名";
        String password = bCryptPasswordEncoder.encode(emailRegisterVo.getPassword());
        Account account = new Account(null,username,password,email,"USER",new Date());
        if (this.save(account)) {
            stringRedisTemplate.delete(key);
            return null;
        }else {
            return "内部错误请联系管理员";
        }
    }
    private boolean existsAccountByEmail(String email){
        return this.baseMapper.exists(Wrappers.<Account>query().eq("email",email));
    }
    private boolean existsAccountByUsername(String username){
        return this.baseMapper.exists(Wrappers.<Account>query().eq("username",username));
    }


}
