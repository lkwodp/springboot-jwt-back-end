package com.example.utils;

/**
 * 相关字段类
 * @author blbyd_li
 * @data 2023/9/10
 * @apiNote
 */
public class Const {
    //JWT令牌字段
    public static final String JWT_BLACK_LIST = "jwt:blacklist:";
    //邮件存放限制
    public static final String VERIFY_EMAIL_LIMIT = "verify:email:limit";
    //邮件数据
    public static final String VERIFY_EMAIL_DATA = "verify:email:data";
    //跨域cors拦截器优先级
    public static final int ORDER_CORS = -102;
    //限流拦截器优先级
    public static final int ORDER_LIMIT = -101;
    //请求限流计数器
    public static final String FLOW_LIMIT_COUNTER = "flow:counter";
    //IP封禁标志
    public static final String FLOW_LIMIT_BLOCK = "flow:block";
}
