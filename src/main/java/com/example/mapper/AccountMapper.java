package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.dto.Account;
import org.apache.ibatis.annotations.Mapper;

/**
 * AccountMapper
 * @author blbyd_li
 * @data 2023/9/10
 * @apiNote
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {

}
