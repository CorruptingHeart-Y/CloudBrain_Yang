package com.neusoft.hospital.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.hospital.common.BusinessException;
import com.neusoft.hospital.common.ErrorCode;
import com.neusoft.hospital.entity.Employee;
import com.neusoft.hospital.mapper.EmployeeMapper;
import com.neusoft.hospital.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

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

    @Override
    public boolean save(Employee entity) {
        if (entity.getPassword() == null || entity.getPassword().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "密码不能为空");
        }
        if (!isBcrypt(entity.getPassword())) {
            entity.setPassword(BCRYPT.encode(entity.getPassword()));
        }
        return super.save(entity);
    }

    @Override
    public boolean updateById(Employee entity) {
        String pwd = entity.getPassword();
        if (pwd == null) {
            return super.updateById(entity);
        }
        if (pwd.isEmpty()) {
            entity.setPassword(null);
        } else if (!isBcrypt(pwd)) {
            entity.setPassword(BCRYPT.encode(pwd));
        }
        return super.updateById(entity);
    }

    private static boolean isBcrypt(String h) {
        return h.length() == 60 && (h.startsWith("$2a$") || h.startsWith("$2b$") || h.startsWith("$2y$"));
    }
}

