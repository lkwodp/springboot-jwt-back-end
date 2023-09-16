package com.example.utils;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 限流工具类
 * @author blbyd_li
 * @data 2023/9/13
 * @apiNote
 */
@Component
public class FlowUtils {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 判断是否在发送冷却器
     * @param key 在redis存放关键字段标志
     * @param blockTime 过期时间
     * @return 结果--> false:在冷冷却期  true:不在冷却期
     */
    public boolean limitOnceCheck(String key,int blockTime){
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))){
            //如果已经存在key,即是已经不在有效期,在冷却时间中
            return false;
        }else {
            //在有效期
            //不在冷却时间让其进入冷却时间
            stringRedisTemplate.opsForValue().set(key," ",blockTime, TimeUnit.SECONDS);
            return true;
        }
    }
}
