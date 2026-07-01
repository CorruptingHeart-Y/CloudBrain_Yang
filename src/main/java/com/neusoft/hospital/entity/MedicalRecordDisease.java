package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 病历首页疾病关联表
 * 病历与疾病多对多：一份病历可关联多个 ICD 疾病，(medical_record_id, disease_id) 唯一去重。
 * 由医生在确认病历时从疾病字典选择，AI 不直接挑疾病 ID。
 */
@Data
@TableName("medical_record_disease")
@Schema(description = "病历首页疾病关联表")
public class MedicalRecordDisease {

    @TableId(type = IdType.AUTO)
    @Schema(description = "自增长主键", example = "1")
    private Integer id;

    @Schema(description = "病历ID，指向medical_record(ID)", example = "1")
    private Integer medicalRecordId;

    @Schema(description = "疾病ID，指向disease(ID)", example = "3")
    private Integer diseaseId;
}
