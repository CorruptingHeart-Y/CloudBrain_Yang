package com.neusoft.hospital.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "员工响应DTO")
public class EmployeeResponse {

    @Schema(description = "员工ID", example = "1")
    private Integer id;

    @Schema(description = "所在科室ID", example = "1")
    private Integer deptmentId;

    @Schema(description = "所在科室名称(冗余)", example = "神经内科")
    private String deptName;

    @Schema(description = "挂号级别ID", example = "1")
    private Integer registLevelId;

    @Schema(description = "挂号级别名称(冗余)", example = "普通号")
    private String registLevelName;

    @Schema(description = "排班规则ID", example = "1")
    private Integer schedulingId;

    @Schema(description = "真实姓名", example = "李医生")
    private String realname;

    @Schema(description = "删除标记", example = "1")
    private Integer delmark;
}
