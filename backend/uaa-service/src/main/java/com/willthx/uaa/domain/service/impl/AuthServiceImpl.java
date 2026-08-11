package com.willthx.uaa.domain.service.impl;

import com.willthx.uaa.domain.port.TokenPort;
import com.willthx.uaa.domain.port.UserRepository;
import com.willthx.uaa.domain.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 認證領域服務實作。
 * 業務邏輯待 SDD 完成後實作。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository  userRepository;
    private final TokenPort       tokenPort;
    private final PasswordEncoder passwordEncoder;

    // TODO: 待 SDD 完成後實作
}
