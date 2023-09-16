package com.example.filter;

import com.example.entity.rest.RestBean;
import com.example.utils.Const;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 限流过滤器
 * @author blbyd_li
 * @data 2023/9/16
 * @apiNote
 */
@Component
@Order(Const.ORDER_LIMIT)
public class FlowLimitFilter extends HttpFilter {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /*
    过滤器
     */
    @Override
    protected void doFilter(HttpServletRequest request,
                            HttpServletResponse response,
                            FilterChain chain) throws IOException, ServletException {
        String address = request.getRemoteAddr();
        if (this.tryCount(address)){
            chain.doFilter(request,response);
        }else {
            this.writeBlockMessage(response);
        }
    }


    private void writeBlockMessage(HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
        httpServletResponse.setContentType("application/json;charset=utf-8");
        httpServletResponse.getWriter().write(RestBean.forbidden("操作过于频繁，请稍后再试").asJsonString());
    }

    /**
     * 根据IP地址来检查计数，如果在规定时间的请求数量过多，该项目是1秒钟限制10次请求
     * 则会采取限制功能
     * @param ip 请求的IP地址
     * @return 是否限流---> true:正常结果，可以继续访问； false：已被采取限流措施
     */
    private boolean tryCount(String ip){
        //加锁
        synchronized (ip.intern()){
            //该IP地址已经被限流---在Redis中已经存在该个key
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(this.generateLimitBlockKeyByIp(ip))))
                return false;
            return this.limitPeriodCheck(ip);
        }
    }

    /**
     * 根据IP地址来计数并且检查是否需要限流
     * @param ip 请求的IP地址
     * @return true：正常  false：被限流
     */
    private boolean limitPeriodCheck(String ip){
        //根据IP获取计数器Key
        String key = this.generateLimitCounterKeyByIp(ip);
        //检查该key是否已经存在Redis中
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            //已经存在
            //将该key对应的数量自增
            Long increment = Optional.ofNullable(stringRedisTemplate.opsForValue().increment(key)).orElse(0L);
            if (increment > 10){
                //如果计数结果已经超过10，已经达到限流的标准
                //则将该IP地址设置限流标志并存入Redis中
                stringRedisTemplate.opsForValue()
                        .set(this.generateLimitBlockKeyByIp(ip)," ",30,TimeUnit.SECONDS);
                return false;
            }
        }else {
            //不存在，则将key存入到Redis中
            //该key初始值为1，有效期为1秒
            stringRedisTemplate.opsForValue().set(key,"1",1, TimeUnit.SECONDS);
        }
        return true;
    }

    /**
     * 根据IP生成限流计数器的key
     * @param ip 一次请求的IP地址
     * @return 生成的key值
     */
    private String generateLimitCounterKeyByIp(String ip){
        return Const.FLOW_LIMIT_COUNTER + ip;
    }

    /**
     * 根据IP生成限流标志的key
     * @param ip ip 一次请求的IP地址
     * @return 生成的key值
     */
    private String generateLimitBlockKeyByIp(String ip){
        return Const.FLOW_LIMIT_BLOCK + ip;
    }
}
