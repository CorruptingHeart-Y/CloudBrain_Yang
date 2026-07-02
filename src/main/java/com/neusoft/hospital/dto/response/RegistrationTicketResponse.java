package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "抢号票据详情")
public class RegistrationTicketResponse {

    @Schema(description = "票据号", example = "QH2026070309301247")
    private String ticketNo;

    @Schema(description = "医生ID", example = "1")
    private Integer employeeId;

    @Schema(description = "就诊日期", example = "2026-07-03")
    private LocalDate visitDate;

    @Schema(description = "午别", example = "上午")
    private String noon;

    @Schema(description = "状态：PENDING/SUCCESS/FAILED/CANCELLED", example = "SUCCESS")
    private String status;

    @Schema(description = "成功后回填挂号ID", example = "11")
    private Integer registerId;

    @Schema(description = "成功后回填病历号", example = "BL2026070309301247")
    private String caseNumber;

    @Schema(description = "失败原因", example = "已约满")
    private String failReason;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
