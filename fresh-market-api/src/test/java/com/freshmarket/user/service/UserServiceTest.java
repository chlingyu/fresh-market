package com.freshmarket.user.service;

import com.freshmarket.common.exception.BusinessException;
import com.freshmarket.security.JwtTokenProvider;
import com.freshmarket.user.dto.*;
import com.freshmarket.user.entity.User;
import com.freshmarket.user.repository.UserRepository;
import com.freshmarket.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * 用户服务测试类
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务测试")
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    private User testUser;
    private UserRegisterRequest registerRequest;
    private UserLoginRequest loginRequest;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("encodedPassword");
        testUser.setPhone("13812345678");
        testUser.setCreatedAt(Instant.now());
        
        registerRequest = new UserRegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setPhone("13812345678");
        
        loginRequest = new UserLoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
    }
    
    @Nested
    @DisplayName("用户注册测试")
    class RegisterTests {
        
        @Test
        @DisplayName("应该成功注册用户")
        void shouldRegisterUserSuccessfully() {
            // Given
            given(userRepository.existsByUsername(anyString())).willReturn(false);
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");
            given(userRepository.save(any(User.class))).willReturn(testUser);
            
            // When
            UserResponse response = userService.register(registerRequest);
            
            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testUser.getId());
            assertThat(response.getUsername()).isEqualTo(testUser.getUsername());
            assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
            assertThat(response.getPhone()).isEqualTo(testUser.getPhone());
            
            verify(userRepository).existsByUsername(registerRequest.getUsername());
            verify(userRepository).existsByEmail(registerRequest.getEmail());
            verify(passwordEncoder).encode(registerRequest.getPassword());
            verify(userRepository).save(any(User.class));
        }
        
        @Test
        @DisplayName("用户名已存在时应该抛出异常")
        void shouldThrowExceptionWhenUsernameExists() {
            // Given
            given(userRepository.existsByUsername(anyString())).willReturn(true);
            
            // When & Then
            assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名已存在");
            
            verify(userRepository).existsByUsername(registerRequest.getUsername());
        }
        
        @Test
        @DisplayName("邮箱已存在时应该抛出异常")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            given(userRepository.existsByUsername(anyString())).willReturn(false);
            given(userRepository.existsByEmail(anyString())).willReturn(true);
            
            // When & Then
            assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱已被注册");
            
            verify(userRepository).existsByUsername(registerRequest.getUsername());
            verify(userRepository).existsByEmail(registerRequest.getEmail());
        }
    }
    
    @Nested
    @DisplayName("用户登录测试")
    class LoginTests {
        
        @Test
        @DisplayName("应该成功登录")
        void shouldLoginSuccessfully() {
            // Given
            String accessToken = "jwt-token";
            int expiresIn = 86400;
            
            given(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
            given(jwtTokenProvider.generateToken(anyLong(), anyString())).willReturn(accessToken);
            given(jwtTokenProvider.getExpirationInSeconds()).willReturn(expiresIn);
            
            // When
            LoginResponse response = userService.login(loginRequest);
            
            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo(accessToken);
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(expiresIn);
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getUsername()).isEqualTo(testUser.getUsername());
            
            verify(userRepository).findByUsernameOrEmail(loginRequest.getUsername(), loginRequest.getUsername());
            verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPasswordHash());
            verify(jwtTokenProvider).generateToken(testUser.getId(), testUser.getUsername());
            verify(jwtTokenProvider).getExpirationInSeconds();
        }
        
        @Test
        @DisplayName("用户不存在时应该抛出异常")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            given(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .willReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");
            
            verify(userRepository).findByUsernameOrEmail(loginRequest.getUsername(), loginRequest.getUsername());
        }
        
        @Test
        @DisplayName("密码错误时应该抛出异常")
        void shouldThrowExceptionWhenPasswordIncorrect() {
            // Given
            given(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);
            
            // When & Then
            assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");
            
            verify(userRepository).findByUsernameOrEmail(loginRequest.getUsername(), loginRequest.getUsername());
            verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPasswordHash());
        }
    }
    
    @Nested
    @DisplayName("获取用户信息测试")
    class GetUserTests {
        
        @Test
        @DisplayName("应该成功获取用户信息")
        void shouldGetUserSuccessfully() {
            // Given
            given(userRepository.findById(anyLong())).willReturn(Optional.of(testUser));
            
            // When
            UserResponse response = userService.getUserById(testUser.getId());
            
            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(testUser.getId());
            assertThat(response.getUsername()).isEqualTo(testUser.getUsername());
            assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
            assertThat(response.getPhone()).isEqualTo(testUser.getPhone());
            
            verify(userRepository).findById(testUser.getId());
        }
        
        @Test
        @DisplayName("用户不存在时应该抛出异常")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            given(userRepository.findById(anyLong())).willReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> userService.getUserById(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在");
            
            verify(userRepository).findById(1L);
        }
    }
    
    @Nested
    @DisplayName("更新用户信息测试")
    class UpdateUserTests {
        
        @Test
        @DisplayName("应该成功更新用户信息")
        void shouldUpdateUserSuccessfully() {
            // Given
            UserUpdateRequest updateRequest = new UserUpdateRequest();
            updateRequest.setEmail("newemail@example.com");
            updateRequest.setPhone("13987654321");
            
            User updatedUser = new User();
            updatedUser.setId(testUser.getId());
            updatedUser.setUsername(testUser.getUsername());
            updatedUser.setEmail(updateRequest.getEmail());
            updatedUser.setPasswordHash(testUser.getPasswordHash());
            updatedUser.setPhone(updateRequest.getPhone());
            updatedUser.setCreatedAt(testUser.getCreatedAt());
            
            given(userRepository.findById(anyLong())).willReturn(Optional.of(testUser));
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(userRepository.save(any(User.class))).willReturn(updatedUser);
            
            // When
            UserResponse response = userService.updateUser(testUser.getId(), updateRequest);
            
            // Then
            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo(updateRequest.getEmail());
            assertThat(response.getPhone()).isEqualTo(updateRequest.getPhone());
            
            verify(userRepository).findById(testUser.getId());
            verify(userRepository).existsByEmail(updateRequest.getEmail());
            verify(userRepository).save(any(User.class));
        }
        
        @Test
        @DisplayName("邮箱已被其他用户使用时应该抛出异常")
        void shouldThrowExceptionWhenEmailAlreadyInUse() {
            // Given
            UserUpdateRequest updateRequest = new UserUpdateRequest();
            updateRequest.setEmail("existing@example.com");
            
            given(userRepository.findById(anyLong())).willReturn(Optional.of(testUser));
            given(userRepository.existsByEmail(anyString())).willReturn(true);
            
            // When & Then
            assertThatThrownBy(() -> userService.updateUser(testUser.getId(), updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱已被其他用户注册");
            
            verify(userRepository).findById(testUser.getId());
            verify(userRepository).existsByEmail(updateRequest.getEmail());
        }
        
        @Test
        @DisplayName("用户不存在时应该抛出异常")
        void shouldThrowExceptionWhenUserNotFoundForUpdate() {
            // Given
            UserUpdateRequest updateRequest = new UserUpdateRequest();
            updateRequest.setEmail("test@example.com");
            
            given(userRepository.findById(anyLong())).willReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> userService.updateUser(1L, updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在");
            
            verify(userRepository).findById(1L);
        }
    }
}