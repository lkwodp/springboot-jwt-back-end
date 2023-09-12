package com.example.config;

import com.example.entity.dto.Account;
import com.example.entity.rest.RestBean;
import com.example.entity.vo.response.AuthorizeVo;
import com.example.filter.JwtAuthorizeFilter;
import com.example.service.AccountService;
import com.example.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author blbyd_li
 * @data 2023/9/8
 * @apiNote
 */
@Configuration
public class SecurityConfiguration {

    @Resource
    private JwtUtils jwtUtils;
    @Resource
    private JwtAuthorizeFilter jwtAuthorizeFilter;
    @Resource
    private AccountService accountService;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                //对于所有的链接
                .authorizeHttpRequests(conf -> conf
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                //登录
                .formLogin(conf->conf
                        .loginProcessingUrl("/api/auth/login")
                        .failureHandler(this::onAuthenticationFailure)
                        .successHandler(this::onAuthenticationSuccess)
                )
                //登出
                .logout(conf->conf
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(this::onLogoutSuccess)
                )
                //有效时间
                .exceptionHandling(conf->conf
                        .authenticationEntryPoint(this::handleProcess)
                        .accessDeniedHandler(this::handle)
                )
                .csrf(AbstractHttpConfigurer::disable)
                //关闭session
                .sessionManagement(conf->conf
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                //拦截器
                .addFilterBefore(jwtAuthorizeFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    //登录成功但是角色权限不足
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(RestBean.forbidden(accessDeniedException.getMessage()).asJsonString());

    }


    //没有登录导致权限不足
    public void handleProcess(HttpServletRequest request,
                              HttpServletResponse response,
                              AuthenticationException exception) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(RestBean.unauthorized(exception.getMessage()).asJsonString());
    }

    //登录成功
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        response.setContentType("application/json;charset=utf-8");

       User user = (User) authentication.getPrincipal();
        Account account = accountService.findAccountByUsernameOrEmail(user.getUsername());
        String token = jwtUtils.creatJwt(user,account.getId(),account.getUsername());

        //方案三
        AuthorizeVo vo = account.asViewObject(AuthorizeVo.class,v->{
            v.setExpire(jwtUtils.expireTime());
            v.setToken(token);
        });

       /*方案二
        AuthorizeVo vo = new AuthorizeVo();
        BeanUtils.copyProperties(account,vo);  OR  AuthorizeVo vo = account.asViewObject(AuthorizeVo.class);
        vo.setExpire(jwtUtils.expireTime());
        vo.setToken(token);*/


       /* 方案一
        AuthorizeVo vo = new AuthorizeVo();
        vo.setExpire(jwtUtils.expireTime());
        vo.setRole(account.getRole());
        vo.setToken(token);
        vo.setUsername(account.getUsername());*/

        response.getWriter().write(RestBean.success(vo).asJsonString());
    }
    //登录失败
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(RestBean.unauthorized(exception.getMessage()).asJsonString());
    }

    //退出登录成功
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        PrintWriter writer = response.getWriter();
        String authorization = request.getHeader("Authorization");
        if(jwtUtils.invalidateJwt(authorization)){
            writer.write(RestBean.success().asJsonString());
        }else {
            writer.write(RestBean.failure(400,"退出登录失败").asJsonString());
        }
    }
}
