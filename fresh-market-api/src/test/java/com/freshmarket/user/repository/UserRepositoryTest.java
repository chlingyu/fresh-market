package com.freshmarket.user.repository;

import com.freshmarket.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户数据访问层测试类
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("用户数据访问层测试")
class UserRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("encodedPassword");
        testUser.setPhone("13812345678");
    }
    
    @Test
    @DisplayName("应该根据用户名查找用户")
    void shouldFindUserByUsername() {
        // Given
        entityManager.persistAndFlush(testUser);
        
        // When
        Optional<User> found = userRepository.findByUsername("testuser");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }
    
    @Test
    @DisplayName("用户名不存在时应该返回空")
    void shouldReturnEmptyWhenUsernameNotFound() {
        // When
        Optional<User> found = userRepository.findByUsername("nonexistent");
        
        // Then
        assertThat(found).isNotPresent();
    }
    
    @Test
    @DisplayName("应该根据邮箱查找用户")
    void shouldFindUserByEmail() {
        // Given
        entityManager.persistAndFlush(testUser);
        
        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }
    
    @Test
    @DisplayName("邮箱不存在时应该返回空")
    void shouldReturnEmptyWhenEmailNotFound() {
        // When
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");
        
        // Then
        assertThat(found).isNotPresent();
    }
    
    @Test
    @DisplayName("应该根据用户名或邮箱查找用户 - 用户名匹配")
    void shouldFindUserByUsernameOrEmail_UsernameMatch() {
        // Given
        entityManager.persistAndFlush(testUser);
        
        // When
        Optional<User> found = userRepository.findByUsernameOrEmail("testuser", "testuser");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }
    
    @Test
    @DisplayName("应该根据用户名或邮箱查找用户 - 邮箱匹配")
    void shouldFindUserByUsernameOrEmail_EmailMatch() {
        // Given
        entityManager.persistAndFlush(testUser);
        
        // When
        Optional<User> found = userRepository.findByUsernameOrEmail("test@example.com", "test@example.com");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }
    
    @Test
    @DisplayName("应该正确检查用户名是否存在")
    void shouldCheckIfUsernameExists() {
        // Given
        entityManager.persistAndFlush(testUser);
        
        // When
        boolean exists = userRepository.existsByUsername("testuser");
        boolean notExists = userRepository.existsByUsername("nonexistent");
        
        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
    
    @Test
    @DisplayName("应该正确检查邮箱是否存在")
    void shouldCheckIfEmailExists() {
        // Given
        entityManager.persistAndFlush(testUser);
        
        // When
        boolean exists = userRepository.existsByEmail("test@example.com");
        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");
        
        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
    
    @Test
    @DisplayName("应该保存和检索用户完整信息")
    void shouldSaveAndRetrieveCompleteUserInfo() {
        // When
        User saved = userRepository.save(testUser);
        entityManager.flush();
        
        Optional<User> retrieved = userRepository.findById(saved.getId());
        
        // Then
        assertThat(retrieved).isPresent();
        User user = retrieved.get();
        assertThat(user.getId()).isNotNull();
        assertThat(user.getUsername()).isEqualTo("testuser");
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("encodedPassword");
        assertThat(user.getPhone()).isEqualTo("13812345678");
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }
}