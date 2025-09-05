package com.freshmarket.security;

import java.security.Principal;

/**
 * 用户主体信息
 */
public class UserPrincipal implements Principal {
    
    private final Long userId;
    private final String username;
    
    public UserPrincipal(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    @Override
    public String getName() {
        return username;
    }
    
    public String getUsername() {
        return username;
    }
}