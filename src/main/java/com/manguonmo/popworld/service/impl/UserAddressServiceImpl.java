package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.request.AddressRequest;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.entity.UserAddress;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.UserAddressRepository;
import com.manguonmo.popworld.repository.UserRepository;
import com.manguonmo.popworld.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;

    @Override
    public List<UserAddress> getAddressesByUserId(Long userId) {
        if (userId == null) {
            throw new BadRequestException("Yêu cầu mã định danh người dùng.");
        }
        return userAddressRepository.findByUserIdOrderByIsDefaultDescIdDesc(userId);
    }

    @Override
    public UserAddress getAddressByIdAndUserId(Long addressId, Long userId) {
        if (userId == null || addressId == null) {
            throw new BadRequestException("Thông tin tra cứu địa chỉ không hợp lệ.");
        }
        return userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ hoặc bạn không có quyền truy cập."));
    }

    @Override
    @Transactional
    public UserAddress createAddress(Long userId, AddressRequest request) {
        if (userId == null) {
            throw new BadRequestException("Yêu cầu xác thực người dùng để thêm địa chỉ.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        long count = userAddressRepository.countByUserId(userId);
        boolean isDefault = (count == 0) || Boolean.TRUE.equals(request.getIsDefault());

        if (isDefault && count > 0) {
            List<UserAddress> existingAddresses = userAddressRepository.findByUserId(userId);
            for (UserAddress addr : existingAddresses) {
                addr.setIsDefault(false);
            }
            userAddressRepository.saveAll(existingAddresses);
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .recipientName(request.getRecipientName().trim())
                .recipientPhone(request.getRecipientPhone().trim())
                .provinceCity(request.getProvinceCity().trim())
                .district(request.getDistrict().trim())
                .ward(request.getWard() != null ? request.getWard().trim() : "")
                .detailedAddress(request.getDetailedAddress().trim())
                .isDefault(isDefault)
                .build();

        return userAddressRepository.save(address);
    }

    @Override
    @Transactional
    public UserAddress updateAddress(Long userId, Long addressId, AddressRequest request) {
        UserAddress address = getAddressByIdAndUserId(addressId, userId);

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            List<UserAddress> allAddresses = userAddressRepository.findByUserId(userId);
            for (UserAddress a : allAddresses) {
                a.setIsDefault(false);
            }
            userAddressRepository.saveAll(allAddresses);
            address.setIsDefault(true);
        }

        address.setRecipientName(request.getRecipientName().trim());
        address.setRecipientPhone(request.getRecipientPhone().trim());
        address.setProvinceCity(request.getProvinceCity().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setWard(request.getWard() != null ? request.getWard().trim() : "");
        address.setDetailedAddress(request.getDetailedAddress().trim());

        return userAddressRepository.save(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        UserAddress address = getAddressByIdAndUserId(addressId, userId);
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());

        userAddressRepository.delete(address);
        userAddressRepository.flush();

        if (wasDefault) {
            List<UserAddress> remaining = userAddressRepository.findByUserIdOrderByIsDefaultDescIdDesc(userId);
            if (!remaining.isEmpty()) {
                UserAddress newDefault = remaining.get(0);
                newDefault.setIsDefault(true);
                userAddressRepository.save(newDefault);
            }
        }
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        // Kiểm tra quyền sở hữu
        getAddressByIdAndUserId(addressId, userId);

        List<UserAddress> allAddresses = userAddressRepository.findByUserId(userId);
        for (UserAddress a : allAddresses) {
            a.setIsDefault(a.getId().equals(addressId));
        }
        userAddressRepository.saveAll(allAddresses);
    }
}
