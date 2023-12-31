package com.example.filter;

import com.example.utils.Const;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 解决跨域配置
 * @author blbyd_li
 * @data 2023/9/12
 * @apiNote
 */
@Component
@Order(Const.ORDER_CORS)//将优先机设置成比Security更高，即是比-100更小
public class CorsFilter extends HttpFilter {
    /*
    过滤器
     */
    @Override
    protected void doFilter(HttpServletRequest request,
                            HttpServletResponse response,
                            FilterChain chain) throws IOException, ServletException {
        this.addCorsHeader(request,response);
        //放行
        chain.doFilter(request,response);
    }

    /**
     * 设置响应response的响应头
     * @param request 请求
     * @param response 响应
     */
    private void addCorsHeader(HttpServletRequest request, HttpServletResponse response){
        response.addHeader("Access-Control-Allow-Origin",request.getHeader("Origin"));
        response.addHeader("Access-Control-Allow-Methods","GET, POST, PUT, DELETE, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers","Authorization,Content-Type");
    }
}
