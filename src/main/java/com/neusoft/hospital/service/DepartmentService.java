package com.neusoft.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.hospital.entity.Department;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DepartmentService extends IService<Department> {

    IPage<Department> pageQuery(Page<Department> page, @Param("deptName") String deptName);

    List<Department> listAll();
}
