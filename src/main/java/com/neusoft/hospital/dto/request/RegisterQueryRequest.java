package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "挂号查询请求")
public class RegisterQueryRequest {

    @Schema(description = "病历号", example = "BL20240001")
    private String caseNumber;

    @Schema(description = "真实姓名(模糊查询)", example = "张")
    private String realName;

    @Schema(description = "看诊状态", example = "1")
    private Integer visitState;

    @Schema(description = "看诊日期起始", example = "2024-06-01")
    private LocalDate visitDateStart;

    @Schema(description = "看诊日期截止", example = "2024-06-30")
    private LocalDate visitDateEnd;

    @Schema(description = "科室ID", example = "1")
    private Integer deptmentId;
}
