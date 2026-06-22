package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "处置申请创建请求")
public class DisposalRequestCreateRequest {

    @NotNull(message = "挂号ID不能为空")
    @Schema(description = "挂号ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer registerId;

    @NotNull(message = "医技项目ID不能为空")
    @Schema(description = "医技项目ID", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer medicalTechnologyId;

    @Schema(description = "处置信息", example = "换药处置")
    private String disposalInfo;

    @Schema(description = "处置部位", example = "右手")
    private String disposalPosition;
}
