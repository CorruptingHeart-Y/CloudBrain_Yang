package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "抢号提交响应")
public class RegistrationGrabResponse {

    @Schema(description = "票据号", example = "QH2026070309301247")
    private String ticketNo;

    @Schema(description = "状态：PENDING(排队中)/SUCCESS(已抢到)/FAILED(失败)", example = "PENDING")
    private String status;
}
