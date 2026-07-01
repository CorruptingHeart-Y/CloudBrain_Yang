package com.neusoft.hospital.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Python /ai/medical-record 返回体，9 临床字段，camelCase 与 Python Pydantic alias 对齐。
 * 纯文本草稿，无需富化；未提及字段 Python 返回空串。
 */
@Data
@Schema(description = "AI病历草稿(来自Python)")
public class AiMedicalRecordResponse {

    @Schema(description = "主诉")
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
}
