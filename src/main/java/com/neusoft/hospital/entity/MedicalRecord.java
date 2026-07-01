package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 患者病历表（medical_record）。
 * <p>
 * v1.0 schema 已建表（register_id 唯一）；v2.0 零 ALTER 旧表约束：不新增列。
 * 病历来源（A=AI/M=人工）外移至 medical_record_meta 新表，不修改本表结构。
 * 9 个临床字段由 AI 病历生成草稿填充或医生手写。
 */
@Data
@TableName("medical_record")
@Schema(description = "患者病历表")
public class MedicalRecord {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "挂号ID（唯一）", example = "1")
    private Integer registerId;

    @Schema(description = "主诉", example = "头痛三天")
    private String readme;

    @Schema(description = "现病史", example = "患者三天前无明显诱因出现头痛...")
    private String present;

    @Schema(description = "现病治疗情况", example = "自行服药未见缓解")
    private String presentTreat;

    @Schema(description = "既往史", example = "高血压五年")
    private String history;

    @Schema(description = "过敏史", example = "青霉素过敏")
    private String allergy;

    @Schema(description = "体格检查", example = "神清，血压140/90")
    private String physique;

    @Schema(description = "检查/检验建议", example = "建议头颅CT")
    private String proposal;

    @Schema(description = "注意事项", example = "注意休息，低盐饮食")
    private String careful;

    @Schema(description = "诊断结果", example = "偏头痛")
    private String diagnosis;

    @Schema(description = "处理意见", example = "对症止痛，随访")
    private String cure;
}
