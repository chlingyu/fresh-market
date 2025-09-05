package com.freshmarket.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshmarket.security.UserPrincipal;
import com.freshmarket.user.dto.*;
import com.freshmarket.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户控制器测试类
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("用户控制器测试")
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private UserService userService;
    
    private UserResponse testUserResponse;
    private UserRegisterRequest registerRequest;
    private UserLoginRequest loginRequest;
    private LoginResponse loginResponse;
    
    @BeforeEach
    void setUp() {
        testUserResponse = new UserResponse();
        testUserResponse.setId(1L);
        testUserResponse.setUsername("testuser");
        testUserResponse.setEmail("test@example.com");
        testUserResponse.setPhone("13812345678");
        testUserResponse.setCreatedAt(Instant.now());
        
        registerRequest = new UserRegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setPhone("13812345678");
        
        loginRequest = new UserLoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
        
        loginResponse = new LoginResponse();
        loginResponse.setUser(testUserResponse);
        loginResponse.setAccessToken("jwt-token");
        loginResponse.setExpiresIn(86400);
    }
    
    @Nested
    @DisplayName("用户注册API测试")
    class RegisterApiTests {
        
        @Test
        @DisplayName("应该成功注册用户")
        void shouldRegisterUserSuccessfully() throws Exception {
            // Given
            given(userService.register(any(UserRegisterRequest.class))).willReturn(testUserResponse);
            
            // When & Then
            mockMvc.perform(post("/api/v1/users/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.id").value(testUserResponse.getId()))
                .andExpect(jsonPath("$.data.username").value(testUserResponse.getUsername()))
                .andExpect(jsonPath("$.data.email").value(testUserResponse.getEmail()));
        }
        
        @Test
        @DisplayName("无效请求参数应该返回400错误")
        void shouldReturn400ForInvalidRequest() throws Exception {
            // Given
            UserRegisterRequest invalidRequest = new UserRegisterRequest();
            invalidRequest.setUsername("ab"); // 太短
            invalidRequest.setEmail("invalid-email"); // 无效邮箱
            invalidRequest.setPassword("123"); // 太短
            invalidRequest.setPhone("123"); // 无效手机号
            
            // When & Then
            mockMvc.perform(post("/api/v1/users/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }
    
    @Nested
    @DisplayName("用户登录API测试")
    class LoginApiTests {
        
        @Test
        @DisplayName("应该成功登录")
        void shouldLoginSuccessfully() throws Exception {
            // Given
            given(userService.login(any(UserLoginRequest.class))).willReturn(loginResponse);
            
            // When & Then
            mockMvc.perform(post("/api/v1/users/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").value(loginResponse.getAccessToken()))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(loginResponse.getExpiresIn()));
        }
        
        @Test
        @DisplayName("空请求参数应该返回400错误")
        void shouldReturn400ForEmptyCredentials() throws Exception {
            // Given
            UserLoginRequest emptyRequest = new UserLoginRequest();
            
            // When & Then
            mockMvc.perform(post("/api/v1/users/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(emptyRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }
    
    @Nested
    @DisplayName("获取用户信息API测试")
    class GetUserProfileApiTests {
        
        @Test
        @DisplayName("应该成功获取用户信息")
        @WithMockUser
        void shouldGetUserProfileSuccessfully() throws Exception {
            // Given
            UserPrincipal userPrincipal = new UserPrincipal(1L, "testuser");
            given(userService.getUserById(anyLong())).willReturn(testUserResponse);
            
            // When & Then
            mockMvc.perform(get("/api/v1/users/profile")
                    .with(authentication(createMockAuthentication(userPrincipal))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(testUserResponse.getId()))
                .andExpect(jsonPath("$.data.username").value(testUserResponse.getUsername()));
        }
        
        @Test
        @DisplayName("未认证用户应该返回401错误")
        void shouldReturn401ForUnauthenticatedUser() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/users/profile"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
        }
    }
    
    @Nested
    @DisplayName("更新用户信息API测试")
    class UpdateUserProfileApiTests {
        
        @Test
        @DisplayName("应该成功更新用户信息")
        @WithMockUser
        void shouldUpdateUserProfileSuccessfully() throws Exception {
            // Given
            UserPrincipal userPrincipal = new UserPrincipal(1L, "testuser");
            UserUpdateRequest updateRequest = new UserUpdateRequest();
            updateRequest.setEmail("newemail@example.com");
            updateRequest.setPhone("13987654321");
            
            UserResponse updatedResponse = new UserResponse();
            updatedResponse.setId(1L);
            updatedResponse.setUsername("testuser");
            updatedResponse.setEmail(updateRequest.getEmail());
            updatedResponse.setPhone(updateRequest.getPhone());
            updatedResponse.setCreatedAt(testUserResponse.getCreatedAt());
            
            given(userService.updateUser(anyLong(), any(UserUpdateRequest.class))).willReturn(updatedResponse);
            
            // When & Then
            mockMvc.perform(put("/api/v1/users/profile")
                    .with(authentication(createMockAuthentication(userPrincipal)))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.email").value(updateRequest.getEmail()))
                .andExpect(jsonPath("$.data.phone").value(updateRequest.getPhone()));
        }
        
        @Test
        @DisplayName("无效邮箱格式应该返回400错误")
        @WithMockUser
        void shouldReturn400ForInvalidEmail() throws Exception {
            // Given
            UserPrincipal userPrincipal = new UserPrincipal(1L, "testuser");
            UserUpdateRequest invalidRequest = new UserUpdateRequest();
            invalidRequest.setEmail("invalid-email");
            
            // When & Then
            mockMvc.perform(put("/api/v1/users/profile")
                    .with(authentication(createMockAuthentication(userPrincipal)))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }
    
    private org.springframework.security.core.Authentication createMockAuthentication(UserPrincipal userPrincipal) {
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            userPrincipal, null, java.util.Collections.emptyList());
    }
}