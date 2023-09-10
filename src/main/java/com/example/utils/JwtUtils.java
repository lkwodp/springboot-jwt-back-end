package com.example.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author blbyd_li
 * @data 2023/9/8
 * @apiNote
 */
@Component
public class JwtUtils {

    @Value("${spring.security.jwt.key}")
    String key; //签名
    @Value("${spring.security.jwt.expire}")
    int expire; //jwt过期时间


    @Resource
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 创建jwt
     * @param userDetails 用户
     * @param id 用户ID
     * @param username 用户名
     * @return jwt
     */
    public String creatJwt(UserDetails userDetails,
                           int id,
                           String username){
        //加密算法
        Algorithm algorithm = Algorithm.HMAC256(key);
        //获取过期时间
        Date expire = this.expireTime();
        //构建jwt
        return JWT.create()
                .withJWTId(UUID.randomUUID().toString()) //给令牌添加一个ID
                .withClaim("id",id)  //设置ID
                .withClaim("name",username)  //设置用户名
                .withClaim("authorities",
                        userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                //设置用户角色
                .withExpiresAt(expire)  //设置过期时间
                .withIssuedAt(new Date())  //设置构建时间
                .sign(algorithm);  //构建签名
    }

    /**
     * 获取过期时间
     * @return 时间
     */
    public Date expireTime(){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, expire * 24);
        return calendar.getTime();
    }

    /**
     * 让JWT令牌失效
     * @param headerToken 请求头token信息
     * @return 结果
     */

    public boolean invalidateJwt(String headerToken){
        String token = this.convertToken(headerToken);
        if(token == null) return false;
        Algorithm algorithm = Algorithm.HMAC256(key);
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();
        try {
            DecodedJWT jwt = jwtVerifier.verify(token);
            String jwtId = jwt.getId();
            return this.deleteToken(jwtId,jwt.getExpiresAt());
        }catch (JWTVerificationException e){
            return false;
        }
    }

    /**
     * 删除token
     * @param uuid jwt的ID
     * @param time jwt令牌剩余有效时间
     * @return 是否删除成功
     */
    private boolean deleteToken(String uuid,Date time){
        if (this.isInvalidToken(uuid))
            return false;
        Date nowTime = new Date();
        long expire = Math.max(time.getTime() - nowTime.getTime(),0);
        stringRedisTemplate.opsForValue().set(Const.JWT_BLACK_LIST + uuid,"",expire, TimeUnit.MICROSECONDS);
        return true;
    }

    /**
     * 判断jwt令牌是否已经过期
     * @param uuid jwt令牌ID
     * @return 是否过期
     */
    private boolean isInvalidToken(String uuid){
       return Boolean.TRUE.equals(stringRedisTemplate.hasKey(Const.JWT_BLACK_LIST + uuid));
    }

    /**
     * 检验jwt
     * @param headerToken token信息
     * @return 结果
     */
    public DecodedJWT resolveJwt(String headerToken){
        String token = this.convertToken(headerToken);
        if(token == null) return null;
        Algorithm algorithm = Algorithm.HMAC256(key);
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();
        try {
            DecodedJWT verify = jwtVerifier.verify(token);
            if (this.isInvalidToken(verify.getId()))
                return null;
            Date expiresAt = verify.getExpiresAt();
            return new Date().after(expiresAt) ? null : verify;
        }catch (JWTVerificationException e){
            return null;
        }


    }

    /**
     * 验证jwt工具
     * @param headerToken token信息
     * @return 结果
     */
    private String convertToken(String headerToken){
        if (headerToken == null || !headerToken.startsWith("Bearer "))
            return null;
        return headerToken.substring(7);
    }


    /**
     * 根据jwt信息构建用户
     * @param jwt token信息
     * @return 用户
     */
    public UserDetails toUser(DecodedJWT jwt){
        Map<String, Claim> claims = jwt.getClaims();
        return User
                .withUsername(claims.get("name").asString())
                .password("*******")
                .authorities(claims.get("authorities").asArray(String.class))
                .build();
    }

    /**
     * 根据jwt信息获取用户ID
     * @param jwt token信息
     * @return 用户ID
     */
    public Integer toId(DecodedJWT jwt){
        Map<String, Claim> claims = jwt.getClaims();
        return claims.get("id").asInt();
    }
}
