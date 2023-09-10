package com.example.entity.vo.response;

import lombok.Data;

import java.util.Date;

/**
 * @author blbyd_li
 * @data 2023/9/8
 * @apiNote
 */
@Data
public class AuthorizeVo {
    String username;
    String role;
    String token;
    Date expire;

}
