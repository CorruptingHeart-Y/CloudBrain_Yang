package com.neusoft.hospital.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neusoft.hospital.entity.Department;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
}
