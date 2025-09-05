package com.freshmarket.user.service.impl;

import com.freshmarket.common.exception.BusinessException;
import com.freshmarket.security.JwtTokenProvider;
import com.freshmarket.user.dto.*;
import com.freshmarket.user.entity.User;
import com.freshmarket.user.repository.UserRepository;
import com.freshmarket.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户服务实现类
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    
    public UserServiceImpl(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    @Override
    public UserResponse register(UserRegisterRequest request) {
        logger.info("User registration attempt for username: {}", request.getUsername());
        
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw BusinessException.badRequest("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw BusinessException.badRequest("邮箱已被注册");
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        
        // 保存用户
        user = userRepository.save(user);
        
        logger.info("User registered successfully with id: {}, username: {}", user.getId(), user.getUsername());
        
        return mapToUserResponse(user);
    }
    
    @Override
    public LoginResponse login(UserLoginRequest request) {
        logger.info("User login attempt for: {}", request.getUsername());
        
        // 根据用户名或邮箱查找用户
        User user = userRepository.findByUsernameOrEmail(request.getUsername(), request.getUsername())
                .orElseThrow(() -> BusinessException.unauthorized("用户名或密码错误"));
        
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            logger.warn("Invalid password attempt for user: {}", user.getUsername());
            throw BusinessException.unauthorized("用户名或密码错误");
        }
        
        // 生成JWT令牌
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getUsername());
        
        logger.info("User logged in successfully: {} (id: {})", user.getUsername(), user.getId());
        
        return new LoginResponse(
            mapToUserResponse(user),
            accessToken,
            jwtTokenProvider.getExpirationInSeconds()
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        
        return mapToUserResponse(user);
    }
    
    @Override
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        
        boolean updated = false;
        
        // 更新邮箱
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            // 检查新邮箱是否已被其他用户使用
            if (userRepository.existsByEmail(request.getEmail())) {
                throw BusinessException.badRequest("邮箱已被其他用户注册");
            }
            user.setEmail(request.getEmail());
            updated = true;
        }
        
        // 更新手机号
        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().equals(user.getPhone())) {
            user.setPhone(request.getPhone());
            updated = true;
        }
        
        if (updated) {
            user = userRepository.save(user);
            logger.info("User updated successfully: {} (id: {})", user.getUsername(), user.getId());
        }
        
        return mapToUserResponse(user);
    }
    
    /**
     * 将User实体转换为UserResponse
     */
    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getPhone(),
            user.getCreatedAt()
        );
    }
}