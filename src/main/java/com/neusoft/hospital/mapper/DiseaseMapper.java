package com.neusoft.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.hospital.entity.Disease;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiseaseMapper extends BaseMapper<Disease> {
}
