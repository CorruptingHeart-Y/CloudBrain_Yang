package com.neusoft.hospital.auth.service;

import com.neusoft.hospital.auth.dto.ChangePasswordRequest;
import com.neusoft.hospital.auth.dto.LoginRequest;
import com.neusoft.hospital.auth.dto.LoginResponse;
import com.neusoft.hospital.auth.dto.UserInfoResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout(String token);

    UserInfoResponse currentUserInfo();

    void changePassword(ChangePasswordRequest request);
}
