package com.neusoft.hospital.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "当前登录用户信息")
public class UserInfoResponse {

    @Schema(description = "员工ID", example = "1")
    private Integer id;

    @Schema(description = "真实姓名", example = "李医生")
    private String realname;

    @Schema(description = "所在科室ID", example = "1")
    private Integer deptmentId;

    @Schema(description = "所在科室名称", example = "神经内科")
    private String deptName;

    @Schema(description = "挂号级别ID", example = "1")
    private Integer registLevelId;

    @Schema(description = "挂号级别名称", example = "普通号")
    private String registLevelName;
}
