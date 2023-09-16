package com.example.listener;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 邮件监听器
 * @author blbyd_li
 * @data 2023/9/13
 * @apiNote
 */
@Slf4j
@Component
@RabbitListener(queues = "mail")
public class MailQueueListener {
    @Resource
    JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    String username;

    /**
     * 监听器，发送邮件
     * @param data 存放的数据
     */
    @RabbitHandler
    public void sendMailMessage(Map<String,Object> data){
        String email = (String) data.get("email");
        Integer code = (Integer) data.get("code");
        String type = (String) data.get("type");
        log.info(type + ":" + code + ":" + email);
        SimpleMailMessage message = switch (type){
            case "register" -> createMessage("欢迎注册网站",
                        "您的注册验证码为："+code + ",有效时间为3分钟，为了保障您的账号安全，请勿向他人泄露您的验证码。",email);
            case "reset" -> createMessage("你的重置密码重置邮件",
                    "您好，您正在进行重置密码操作，验证码为："+code+",有效时间3分钟，如非本人操作，请勿略",email);
            default -> null;
        };
        if (message == null) return;
        javaMailSender.send(message);
    }

    /**
     * 发送邮件
     * @param title 邮件标题
     * @param content 内容
     * @param email 收件人
     * @return 邮件
     */
    private SimpleMailMessage createMessage(String title,String content,String email){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject(title);
        message.setText(content);
        message.setTo(email);
        message.setFrom(username);
        return message;
    }
}
