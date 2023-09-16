package com.example.entity.vo.request;

import jakarta.validation.constraints.Email;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * @author blbyd_li
 * @data 2023/9/16
 * @apiNote
 */
@Data
public class EmailResetVo {
    @Email
    String email;
    @Length(min = 6,max = 6)
    String code;
    @Length(min = 6,max = 20)
    String password;
}
