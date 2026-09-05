package com.erp.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户实体。
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 登录名，全局唯一 */
    private String username;

    /** BCrypt 哈希，永不外发 */
    private String password;

    /** 姓名（展示用） */
    private String realName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
