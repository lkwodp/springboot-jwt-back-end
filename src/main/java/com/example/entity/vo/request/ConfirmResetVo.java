package com.example.entity.vo.request;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * @author blbyd_li
 * @data 2023/9/16
 * @apiNote
 */
@Data
@AllArgsConstructor
public class ConfirmResetVo {
    @Email
    String email;
    @Length(min = 6,max = 6)
    String code;
}
