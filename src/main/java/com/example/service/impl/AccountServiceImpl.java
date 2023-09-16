package com.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.Account;
import com.example.entity.vo.request.ConfirmResetVo;
import com.example.entity.vo.request.EmailRegisterVo;
import com.example.entity.vo.request.EmailResetVo;
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

    /**
     * 根据用户名或者邮箱地址查找用户
     * @param conditions 用户名或者邮箱地址
     * @return 用户
     */
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
     * @return null：成功发送 else：对应的错误信息
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
                    .set(this.obtainKeyByEmail(email),String.valueOf(code),3, TimeUnit.MINUTES);
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

    /**
     * 注册用户，根据提交的对象实体注册用户
     * @param emailRegisterVo 对象实体vo
     * @return null：注册用户成功  else：具体的失败原因
     */
    @Override
    public String registerEmailAccount(EmailRegisterVo emailRegisterVo) {
        //获取对应的属性值
        String email = emailRegisterVo.getEmail();
        String username = emailRegisterVo.getUsername();
        //获取相关的key值
        String key = this.obtainKeyByEmail(email);
        //获取Redis中存放的验证码
        String code = stringRedisTemplate.opsForValue().get(key);
        //逐步进行验证
        if (code == null) return "请先获取验证码";
        if(!code.equals(emailRegisterVo.getCode())) return "验证码输入错误，请重新输入";
        if (this.existsAccountByEmail(email)) return "此电子邮件已被其他用户注册";
        if (this.existsAccountByUsername(username)) return "此用户名已存在，请更换用户名";
        //密码加密
        String password = bCryptPasswordEncoder.encode(emailRegisterVo.getPassword());
        //构建对象
        Account account = new Account(null,username,password,email,"USER",new Date());
        //加入数据库
        if (this.save(account)) {
            stringRedisTemplate.delete(key);
            return null;
        }else {
            return "内部错误请联系管理员";
        }
    }

    /**
     * 根据邮箱地址email，判断是否存在该邮箱地址的用户
     * @param email 邮箱地址
     * @return true：已存在用户 false：不存在用户
     */
    private boolean existsAccountByEmail(String email){
        return this.baseMapper.exists(Wrappers.<Account>query().eq("email",email));
    }

    /**
     * 根据用户名判断是否已经存在用户
     * @param username 用户名
     * @return true：存在用户 false：不存在用户
     */
    private boolean existsAccountByUsername(String username){
        return this.baseMapper.exists(Wrappers.<Account>query().eq("username",username));
    }

    /**
     * 根据请求实体的数据对象vo，判断验证码是否正确
     * @param vo 数据对象vo
     * @return null：验证码正确  else：具体错误原因
     */
    @Override
    public String resetConfirm(ConfirmResetVo vo) {
        String email = vo.getEmail();
        String key = this.obtainKeyByEmail(email);
        String code = stringRedisTemplate.opsForValue().get(key);
        if (code == null) return "请先获取验证码";
        if (!code.equals(vo.getCode())) return "验证码输入错误，请重新输入";
        return null;
    }

    /**
     * 根据请求实体对象，进行重置密码
     * @param vo 对象实体vo
     * @return null：正确重置密码 else：具体错误原因
     */
    @Override
    public String resetEmailAccountPassword(EmailResetVo vo) {
        String email = vo.getEmail();
        String verify = this.resetConfirm(new ConfirmResetVo(email,vo.getCode()));
        if (verify != null) return verify;
        String password = bCryptPasswordEncoder.encode(vo.getPassword());
        boolean update = this.update().eq("email",email).set("password",password).update();
        if (update){
            String key = this.obtainKeyByEmail(email);
            stringRedisTemplate.delete(key);
            return null;
        }else {
            return "修改密码失败，请再次尝试";
        }

    }

    /**
     * 根据邮箱地址email获取对应的Redis数据key
     * @param email 邮箱地址
     * @return 获取到Redis数据库的key值
     */
    private String obtainKeyByEmail(String email){
        return Const.VERIFY_EMAIL_DATA + email;
    }


}
