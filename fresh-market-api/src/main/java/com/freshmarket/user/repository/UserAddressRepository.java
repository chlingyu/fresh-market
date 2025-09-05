package com.freshmarket.user.repository;

import com.freshmarket.user.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户地址数据访问层
 */
@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    /**
     * 根据用户ID查询地址列表
     */
    List<UserAddress> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    /**
     * 根据用户ID和地址ID查询地址
     */
    Optional<UserAddress> findByUserIdAndId(Long userId, Long addressId);

    /**
     * 根据用户ID查询默认地址
     */
    Optional<UserAddress> findByUserIdAndIsDefaultTrue(Long userId);

    /**
     * 根据用户ID统计地址数量
     */
    long countByUserId(Long userId);

    /**
     * 清除用户的所有默认地址标记
     */
    @Modifying
    @Query("UPDATE UserAddress ua SET ua.isDefault = false WHERE ua.userId = :userId AND ua.isDefault = true")
    int clearDefaultFlag(@Param("userId") Long userId);

    /**
     * 设置指定地址为默认地址
     */
    @Modifying
    @Query("UPDATE UserAddress ua SET ua.isDefault = true WHERE ua.userId = :userId AND ua.id = :addressId")
    int setAsDefault(@Param("userId") Long userId, @Param("addressId") Long addressId);

    /**
     * 删除用户的指定地址
     */
    void deleteByUserIdAndId(Long userId, Long addressId);

    /**
     * 批量删除用户地址
     */
    @Modifying
    @Query("DELETE FROM UserAddress ua WHERE ua.userId = :userId AND ua.id IN :addressIds")
    int deleteByUserIdAndIds(@Param("userId") Long userId, @Param("addressIds") List<Long> addressIds);
}