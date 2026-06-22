package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("scheduling")
@Schema(description = "排班规则表")
public class Scheduling {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "规则名称，如：周一至周五白班", example = "周一至周五白班")
    private String ruleName;

    @Schema(description = "星期规则，如：1,2,3,4,5", example = "1,2,3,4,5")
    private String weekRule;

    @TableLogic
    @Schema(description = "删除标记，0-未删除，1-已删除", example = "0")
    private Integer delmark;
}
