package com.travelease.backend.auth.service;

import com.travelease.backend.auth.dto.LoginRequest;
import com.travelease.backend.auth.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
