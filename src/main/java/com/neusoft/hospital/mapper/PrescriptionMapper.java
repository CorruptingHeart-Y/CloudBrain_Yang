package com.neusoft.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.hospital.entity.Prescription;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PrescriptionMapper extends BaseMapper<Prescription> {
}
