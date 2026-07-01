package com.neusoft.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 病历元数据表（medical_record_meta）。
 * <p>
 * v2.2 新表承载病历来源（A=AI/M=人工）+ AI 请求/结果快照，
 * 不 ALTER medical_record，符合 v2.0 零 ALTER 旧业务表约束。
 * medical_record_id 唯一——每个病历只有一条元数据行。
 */
@Data
@TableName("medical_record_meta")
@Schema(description = "病历元数据表")
public class MedicalRecordMeta {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID", example = "1")
    private Integer id;

    @Schema(description = "病历ID（唯一）", example = "1")
    private Integer medicalRecordId;

    @Schema(description = "病历来源：A=AI草稿生成，M=人工录入", example = "A")
    private String source;

    @Schema(description = "AI生成时的请求快照JSON")
    private String aiRequestSnapshot;

    @Schema(description = "AI生成时的结果快照JSON")
    private String aiResultSnapshot;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
