package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "排班规则更新请求")
public class SchedulingUpdateRequest {

    @NotNull(message = "ID不能为空")
    @Schema(description = "排班规则ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;

    @Schema(description = "规则名称", example = "工作日全天")
    private String ruleName;

    @Schema(description = "星期规则(7位0/1)", example = "1111100")
    private String weekRule;
}
