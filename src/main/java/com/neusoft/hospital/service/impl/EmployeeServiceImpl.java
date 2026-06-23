package com.neusoft.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.hospital.entity.Employee;
import com.neusoft.hospital.mapper.EmployeeMapper;
import com.neusoft.hospital.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {

    @Override
    public IPage<Employee> pageQuery(Page<Employee> page, String realname, Integer deptmentId) {
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(realname)) {
            wrapper.like(Employee::getRealname, realname);
        }
        if (deptmentId != null) {
            wrapper.eq(Employee::getDeptmentId, deptmentId);
        }
        wrapper.orderByAsc(Employee::getId);
        return this.page(page, wrapper);
    }

    @Override
    public List<Employee> listByDeptId(Integer deptId) {
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Employee::getDeptmentId, deptId);
        wrapper.orderByAsc(Employee::getId);
        return this.list(wrapper);
    }
}
