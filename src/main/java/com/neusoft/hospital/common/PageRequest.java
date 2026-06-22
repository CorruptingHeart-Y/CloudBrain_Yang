package com.neusoft.hospital.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分页请求基类")
public class PageRequest {

    @Schema(description = "页码，默认1", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，默认10", example = "10")
    private Integer pageSize = 10;
}
