package com.neusoft.hospital.ai.controller;

import com.neusoft.hospital.ai.dto.TriageConsultRequest;
import com.neusoft.hospital.ai.dto.TriageResultDTO;
import com.neusoft.hospital.ai.service.TriageService;
import com.neusoft.hospital.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI诊前分诊", description = "根据主诉在候选科室/医生中智能匹配并排序")
@RestController
@RequestMapping("/api/v1/triage")
@RequiredArgsConstructor
public class TriageController {

    private final TriageService triageService;

    @Operation(summary = "诊前分诊咨询",
            description = "根据主诉+患者信息，在全部科室与在岗医生中由 AI 推荐并排序；AI 不可用时返回 503 提示人工分诊。",
            security = @SecurityRequirement(name = "Bearer"))
    @PostMapping("/consult")
    public Result<TriageResultDTO> consult(@RequestBody @Valid TriageConsultRequest request) {
        return Result.ok(triageService.consult(request));
    }
}
