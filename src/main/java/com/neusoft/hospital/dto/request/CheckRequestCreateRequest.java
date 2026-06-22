package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "检查申请创建请求")
public class CheckRequestCreateRequest {

    @NotNull(message = "挂号ID不能为空")
    @Schema(description = "挂号ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer registerId;

    @NotNull(message = "医技项目ID不能为空")
    @Schema(description = "医技项目ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer medicalTechnologyId;

    @Schema(description = "检查信息", example = "头部CT检查")
    private String checkInfo;

    @Schema(description = "检查部位", example = "头部")
    private String checkPosition;
}
