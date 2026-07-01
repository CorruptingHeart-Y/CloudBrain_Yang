package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 患者病历表
 * 一个挂号一份病历（register_id 唯一）。source 标记内容来源：A=AI草稿生成，M=人工录入。
 * 9 个临床字段由 AI 病历生成草稿填充或医生手写，diagnosis 经 medical_record_disease 关联 disease 表做 ICD 编码。
 */
@Data
@TableName("medical_record")
@Schema(description = "患者病历表")
public class MedicalRecord {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "挂号ID，外键", example = "14")
    private Integer registerId;

    @Schema(description = "主诉", example = "头疼三天，伴恶心")
    private String readme;

    @Schema(description = "现病史")
    private String present;

    @Schema(description = "现病治疗情况")
    private String presentTreat;

    @Schema(description = "既往史")
    private String history;

    @Schema(description = "过敏史")
    private String allergy;

    @Schema(description = "体格检查")
    private String physique;

    @Schema(description = "检查/检验建议")
    private String proposal;

    @Schema(description = "注意事项")
    private String careful;

    @Schema(description = "诊断结果")
    private String diagnosis;

    @Schema(description = "处理意见")
    private String cure;

    @Schema(description = "病历来源：A=AI草稿生成，M=人工录入", example = "A")
    private String source;
}
