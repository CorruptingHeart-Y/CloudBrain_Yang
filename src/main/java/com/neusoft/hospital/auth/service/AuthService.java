package com.neusoft.hospital.auth.service;

import com.neusoft.hospital.auth.dto.ChangePasswordRequest;
import com.neusoft.hospital.auth.dto.LoginRequest;
import com.neusoft.hospital.auth.dto.LoginResponse;
import com.neusoft.hospital.auth.dto.PatientRegisterRequest;
import com.neusoft.hospital.auth.dto.UserInfoResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout(String token);

    UserInfoResponse currentUserInfo();

    void changePassword(ChangePasswordRequest request);

    /** 患者自助注册：新建 patient+账号，或资料匹配时绑定已有 patient；事务原子，失败统一安全消息。 */
    void registerPatient(PatientRegisterRequest request);
}
