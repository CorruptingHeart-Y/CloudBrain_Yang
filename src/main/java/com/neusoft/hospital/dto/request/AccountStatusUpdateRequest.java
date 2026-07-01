package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "账号启禁用请求")
public class AccountStatusUpdateRequest {

    @NotNull(message = "状态不能为空")
    @Schema(description = "目标状态：1-启用 0-禁用", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}
