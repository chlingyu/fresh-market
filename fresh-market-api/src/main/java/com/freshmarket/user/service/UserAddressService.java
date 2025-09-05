package com.freshmarket.user.service;

import com.freshmarket.common.exception.ResourceNotFoundException;
import com.freshmarket.user.dto.AddressRequest;
import com.freshmarket.user.dto.AddressResponse;
import com.freshmarket.user.entity.UserAddress;
import com.freshmarket.user.repository.UserAddressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户地址服务
 */
@Service
@Transactional
public class UserAddressService {

    private static final Logger logger = LoggerFactory.getLogger(UserAddressService.class);
    private static final int MAX_ADDRESSES_PER_USER = 10; // 每个用户最多10个地址

    private final UserAddressRepository addressRepository;

    public UserAddressService(UserAddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    /**
     * 创建用户地址
     */
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        logger.debug("Creating address for user: {}", userId);

        // 检查用户地址数量限制
        long currentCount = addressRepository.countByUserId(userId);
        if (currentCount >= MAX_ADDRESSES_PER_USER) {
            throw new IllegalStateException(String.format("用户地址数量已达上限(%d个)", MAX_ADDRESSES_PER_USER));
        }

        UserAddress address = new UserAddress(userId, request.getName(), request.getPhone(), request.getAddress());

        // 如果设置为默认地址，先清除其他默认地址
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultFlag(userId);
            address.setIsDefault(true);
        } else if (currentCount == 0) {
            // 如果是用户的第一个地址，自动设为默认地址
            address.setIsDefault(true);
        }

        UserAddress savedAddress = addressRepository.save(address);
        logger.info("Address created successfully with ID: {} for user: {}", savedAddress.getId(), userId);

        return mapToResponse(savedAddress);
    }

    /**
     * 更新用户地址
     */
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        logger.debug("Updating address {} for user: {}", addressId, userId);

        UserAddress address = addressRepository.findByUserIdAndId(userId, addressId)
                .orElseThrow(() -> new ResourceNotFoundException("地址不存在"));

        // 更新地址信息
        address.setName(request.getName());
        address.setPhone(request.getPhone());
        address.setAddress(request.getAddress());

        // 处理默认地址设置
        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.getIsDefault()) {
            // 如果要设为默认地址，先清除其他默认地址
            addressRepository.clearDefaultFlag(userId);
            address.setIsDefault(true);
        } else if (Boolean.FALSE.equals(request.getIsDefault()) && address.getIsDefault()) {
            // 如果要取消默认地址，检查是否是唯一地址
            long totalCount = addressRepository.countByUserId(userId);
            if (totalCount == 1) {
                throw new IllegalStateException("唯一地址不能取消默认状态");
            }
            address.setIsDefault(false);
        }

        UserAddress updatedAddress = addressRepository.save(address);
        logger.info("Address {} updated successfully for user: {}", addressId, userId);

        return mapToResponse(updatedAddress);
    }

    /**
     * 设置默认地址
     */
    public void setDefaultAddress(Long userId, Long addressId) {
        logger.debug("Setting default address {} for user: {}", addressId, userId);

        // 验证地址存在
        UserAddress address = addressRepository.findByUserIdAndId(userId, addressId)
                .orElseThrow(() -> new ResourceNotFoundException("地址不存在"));

        if (!address.getIsDefault()) {
            // 清除其他默认地址并设置新的默认地址
            addressRepository.clearDefaultFlag(userId);
            addressRepository.setAsDefault(userId, addressId);
            
            logger.info("Set address {} as default for user: {}", addressId, userId);
        }
    }

    /**
     * 删除用户地址
     */
    public void deleteAddress(Long userId, Long addressId) {
        logger.debug("Deleting address {} for user: {}", addressId, userId);

        UserAddress address = addressRepository.findByUserIdAndId(userId, addressId)
                .orElseThrow(() -> new ResourceNotFoundException("地址不存在"));

        // 如果删除的是默认地址，需要重新设置默认地址
        boolean wasDefault = address.getIsDefault();
        
        addressRepository.delete(address);
        
        if (wasDefault) {
            // 找到剩余地址中最早创建的设为默认地址
            List<UserAddress> remainingAddresses = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
            if (!remainingAddresses.isEmpty()) {
                UserAddress firstAddress = remainingAddresses.get(0);
                firstAddress.setIsDefault(true);
                addressRepository.save(firstAddress);
                logger.info("Set address {} as new default after deleting default address", firstAddress.getId());
            }
        }

        logger.info("Address {} deleted successfully for user: {}", addressId, userId);
    }

    /**
     * 获取用户地址详情
     */
    @Transactional(readOnly = true)
    public AddressResponse getAddress(Long userId, Long addressId) {
        logger.debug("Getting address {} for user: {}", addressId, userId);

        UserAddress address = addressRepository.findByUserIdAndId(userId, addressId)
                .orElseThrow(() -> new ResourceNotFoundException("地址不存在"));

        return mapToResponse(address);
    }

    /**
     * 获取用户地址列表
     */
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(Long userId) {
        logger.debug("Getting addresses for user: {}", userId);

        List<UserAddress> addresses = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
        return addresses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户默认地址
     */
    @Transactional(readOnly = true)
    public AddressResponse getDefaultAddress(Long userId) {
        logger.debug("Getting default address for user: {}", userId);

        UserAddress address = addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户没有设置默认地址"));

        return mapToResponse(address);
    }

    /**
     * 获取用户地址数量
     */
    @Transactional(readOnly = true)
    public long getUserAddressCount(Long userId) {
        return addressRepository.countByUserId(userId);
    }

    // 私有辅助方法

    private AddressResponse mapToResponse(UserAddress address) {
        AddressResponse response = new AddressResponse();
        response.setId(address.getId());
        response.setName(address.getName());
        response.setPhone(address.getPhone());
        response.setAddress(address.getAddress());
        response.setIsDefault(address.getIsDefault());
        response.setCreatedAt(address.getCreatedAt());
        return response;
    }
}