package com.travelease.backend.auth.service;

import com.travelease.backend.auth.dto.RegisterRequest;
import com.travelease.backend.auth.dto.UserResponse;
import com.travelease.backend.auth.entity.User;

public interface UserService {

    UserResponse register(RegisterRequest request);

    User getByEmail(String email);
}
