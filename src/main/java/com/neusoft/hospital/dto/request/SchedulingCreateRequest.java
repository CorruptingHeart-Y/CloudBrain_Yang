package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "排班规则创建请求")
public class SchedulingCreateRequest {

    @NotBlank(message = "规则名称不能为空")
    @Schema(description = "规则名称", example = "工作日全天", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ruleName;

    @Schema(description = "星期规则(7位0/1)", example = "1111100")
    private String weekRule;
}
