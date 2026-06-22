package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "挂号更新请求")
public class RegisterUpdateRequest {

    @NotNull(message = "ID不能为空")
    @Schema(description = "挂号ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;

    @Schema(description = "看诊状态：1-已挂号 2-医生接诊 3-看诊结束 4-已退号", example = "2")
    private Integer visitState;
}
