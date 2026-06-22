package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "排班规则响应DTO")
public class SchedulingResponse {

    @Schema(description = "排班规则ID", example = "1")
    private Integer id;

    @Schema(description = "规则名称", example = "工作日全天")
    private String ruleName;

    @Schema(description = "星期规则", example = "1111100")
    private String weekRule;
}
