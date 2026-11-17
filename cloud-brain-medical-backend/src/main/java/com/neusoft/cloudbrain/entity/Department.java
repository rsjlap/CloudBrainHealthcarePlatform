package com.neusoft.cloudbrain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("department")
public class Department {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private String description;

    private String location;

    @TableField("sort_order")
    private Integer sortOrder;
}
