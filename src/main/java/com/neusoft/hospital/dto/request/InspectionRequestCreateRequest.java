package com.neusoft.hospital.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "检验申请创建请求")
public class InspectionRequestCreateRequest {

    @NotNull(message = "挂号ID不能为空")
    @Schema(description = "挂号ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer registerId;

    @NotNull(message = "医技项目ID不能为空")
    @Schema(description = "医技项目ID", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer medicalTechnologyId;

    @Schema(description = "检验信息", example = "血常规")
    private String inspectionInfo;

    @Schema(description = "检验部位", example = "静脉血")
    private String inspectionPosition;
}
