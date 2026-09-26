package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.AddressRequest;
import com.manguonmo.popworld.entity.UserAddress;

import java.util.List;

public interface UserAddressService {
    List<UserAddress> getAddressesByUserId(Long userId);
    UserAddress getAddressByIdAndUserId(Long addressId, Long userId);
    UserAddress createAddress(Long userId, AddressRequest request);
    UserAddress updateAddress(Long userId, Long addressId, AddressRequest request);
    void deleteAddress(Long userId, Long addressId);
    void setDefaultAddress(Long userId, Long addressId);
}
