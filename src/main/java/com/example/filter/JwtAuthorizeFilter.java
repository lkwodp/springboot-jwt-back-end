package com.example.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT令牌过滤器
 * @author blbyd_li
 * @data 2023/9/8
 * @apiNote
 */
@Slf4j
@Component
public class JwtAuthorizeFilter extends OncePerRequestFilter {

    @Resource
    private JwtUtils jwtUtils;
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        //log.info(authorization);
        DecodedJWT jwt = jwtUtils.resolveJwt(authorization);
        //log.info(String.valueOf(jwt!=null));
        if(jwt != null){
            UserDetails userDetails = jwtUtils.toUser(jwt);
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            request.setAttribute("id",jwtUtils.toId(jwt));
        }
        filterChain.doFilter(request,response);
    }
}
