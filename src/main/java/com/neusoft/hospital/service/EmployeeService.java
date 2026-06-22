package com.neusoft.hospital.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neusoft.hospital.entity.Employee;

import java.util.List;

public interface EmployeeService extends IService<Employee> {

    IPage<Employee> pageQuery(Page<Employee> page, String realname, Integer deptmentId);

    List<Employee> listByDeptId(Integer deptId);
}
