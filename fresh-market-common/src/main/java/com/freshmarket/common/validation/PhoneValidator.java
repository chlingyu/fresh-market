package com.freshmarket.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * 手机号验证器
 */
public class PhoneValidator implements ConstraintValidator<Phone, String> {

    private static final Pattern CHINA_PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private boolean nullable;

    @Override
    public void initialize(Phone constraintAnnotation) {
        this.nullable = constraintAnnotation.nullable();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 如果允许为空且值为空，则验证通过
        if (nullable && (value == null || value.trim().isEmpty())) {
            return true;
        }

        // 如果不允许为空且值为空，则验证失败
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        // 验证手机号格式
        return CHINA_PHONE_PATTERN.matcher(value.trim()).matches();
    }
}