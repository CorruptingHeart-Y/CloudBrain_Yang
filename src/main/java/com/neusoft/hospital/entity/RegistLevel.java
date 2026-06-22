package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("regist_level")
@Schema(description = "挂号级别表")
public class RegistLevel {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "挂号级别编码，如：GHPT", example = "GHPT")
    private String registCode;

    @Schema(description = "挂号级别名称，如：普通号", example = "普通号")
    private String registName;

    @Schema(description = "挂号费用，如：5.00", example = "5.00")
    private BigDecimal registFee;

    @Schema(description = "挂号限额，如：100", example = "100")
    private Integer registQuota;

    @Schema(description = "排序号，如：1", example = "1")
    private Integer sequenceNo;

    @TableLogic
    @Schema(description = "删除标记，0-未删除，1-已删除", example = "0")
    private Integer delmark;
}
