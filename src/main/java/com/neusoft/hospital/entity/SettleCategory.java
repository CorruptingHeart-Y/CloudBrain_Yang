package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("settle_category")
@Schema(description = "结算类别表")
public class SettleCategory {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "结算类别编码，如：JSPT", example = "JSPT")
    private String settleCode;

    @Schema(description = "结算类别名称，如：自费", example = "自费")
    private String settleName;

    @Schema(description = "排序号，如：1", example = "1")
    private Integer sequenceNo;

    @TableLogic
    @Schema(description = "删除标记，0-未删除，1-已删除", example = "0")
    private Integer delmark;
}
