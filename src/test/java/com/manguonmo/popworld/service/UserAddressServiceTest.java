package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.AddressRequest;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.entity.UserAddress;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.UserAddressRepository;
import com.manguonmo.popworld.repository.UserRepository;
import com.manguonmo.popworld.service.impl.UserAddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceTest {

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAddressServiceImpl userAddressService;

    private User sampleUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id(1L).email("user1@popworld.com").fullName("User One").build();
        otherUser = User.builder().id(2L).email("user2@popworld.com").fullName("User Two").build();
    }

    @Test
    @DisplayName("getAddressesByUserId: Trả về danh sách địa chỉ sắp xếp mặc định trước")
    void getAddressesByUserId_Success() {
        UserAddress a1 = UserAddress.builder().id(10L).user(sampleUser).isDefault(true).build();
        UserAddress a2 = UserAddress.builder().id(11L).user(sampleUser).isDefault(false).build();
        when(userAddressRepository.findByUserIdOrderByIsDefaultDescIdDesc(1L)).thenReturn(List.of(a1, a2));

        List<UserAddress> list = userAddressService.getAddressesByUserId(1L);
        assertEquals(2, list.size());
        assertTrue(list.get(0).getIsDefault());
    }

    @Test
    @DisplayName("createAddress: Địa chỉ đầu tiên tự động thành mặc định kể cả khi request isDefault=false")
    void createAddress_FirstAddress_AutomaticallyDefault() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userAddressRepository.countByUserId(1L)).thenReturn(0L);
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(i -> i.getArgument(0));

        AddressRequest request = AddressRequest.builder()
                .recipientName("Minh Anh")
                .recipientPhone("0912345678")
                .provinceCity("Hà Nội")
                .district("Cầu Giấy")
                .detailedAddress("10 Phạm Văn Bạch")
                .isDefault(false)
                .build();

        UserAddress created = userAddressService.createAddress(1L, request);
        assertTrue(created.getIsDefault());
        assertEquals("Minh Anh", created.getRecipientName());
        verify(userAddressRepository).save(any(UserAddress.class));
    }

    @Test
    @DisplayName("createAddress: Thêm địa chỉ mới đánh dấu mặc định -> Bỏ mặc định các địa chỉ cũ")
    void createAddress_WithIsDefaultTrue_UnsetsPreviousDefaults() {
        UserAddress oldDefault = UserAddress.builder().id(10L).user(sampleUser).isDefault(true).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userAddressRepository.countByUserId(1L)).thenReturn(1L);
        when(userAddressRepository.findByUserId(1L)).thenReturn(new ArrayList<>(List.of(oldDefault)));
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(i -> i.getArgument(0));

        AddressRequest request = AddressRequest.builder()
                .recipientName("Minh Anh 2")
                .recipientPhone("0912345678")
                .provinceCity("Hà Nội")
                .district("Hoàn Kiếm")
                .detailedAddress("18 Tràng Thi")
                .isDefault(true)
                .build();

        UserAddress created = userAddressService.createAddress(1L, request);
        assertTrue(created.getIsDefault());
        assertFalse(oldDefault.getIsDefault());
        verify(userAddressRepository).saveAll(any());
    }

    @Test
    @DisplayName("updateAddress: Cập nhật thành công thông tin địa chỉ")
    void updateAddress_Success() {
        UserAddress existing = UserAddress.builder()
                .id(10L)
                .user(sampleUser)
                .recipientName("Old")
                .recipientPhone("0900000000")
                .provinceCity("HN")
                .district("CG")
                .detailedAddress("Old Addr")
                .isDefault(true)
                .build();
        when(userAddressRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(existing));
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(i -> i.getArgument(0));

        AddressRequest request = AddressRequest.builder()
                .recipientName("New Name")
                .recipientPhone("0911111111")
                .provinceCity("HN")
                .district("HK")
                .detailedAddress("New Addr")
                .isDefault(true)
                .build();

        UserAddress updated = userAddressService.updateAddress(1L, 10L, request);
        assertEquals("New Name", updated.getRecipientName());
        assertEquals("0911111111", updated.getRecipientPhone());
    }

    @Test
    @DisplayName("updateAddress: Chặn IDOR - Không thể sửa địa chỉ của người dùng khác")
    void updateAddress_IDOR_ThrowsResourceNotFound() {
        when(userAddressRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        AddressRequest request = AddressRequest.builder()
                .recipientName("Hacker")
                .recipientPhone("0999999999")
                .provinceCity("HN")
                .district("CG")
                .detailedAddress("Hacked")
                .build();

        assertThrows(ResourceNotFoundException.class, () -> userAddressService.updateAddress(1L, 99L, request));
    }

    @Test
    @DisplayName("deleteAddress: Chặn IDOR - Không thể xóa địa chỉ của người dùng khác")
    void deleteAddress_IDOR_ThrowsResourceNotFound() {
        when(userAddressRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userAddressService.deleteAddress(1L, 99L));
        verify(userAddressRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteAddress: Xóa địa chỉ mặc định -> Tự động chuyển địa chỉ còn lại thành mặc định")
    void deleteAddress_DeletingDefaultAddress_PromotesNextRemaining() {
        UserAddress defaultAddr = UserAddress.builder().id(10L).user(sampleUser).isDefault(true).build();
        UserAddress nextAddr = UserAddress.builder().id(11L).user(sampleUser).isDefault(false).build();

        when(userAddressRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(defaultAddr));
        when(userAddressRepository.findByUserIdOrderByIsDefaultDescIdDesc(1L)).thenReturn(List.of(nextAddr));

        userAddressService.deleteAddress(1L, 10L);

        verify(userAddressRepository).delete(defaultAddr);
        assertTrue(nextAddr.getIsDefault());
        verify(userAddressRepository).save(nextAddr);
    }

    @Test
    @DisplayName("setDefaultAddress: Đặt địa chỉ chỉ định làm mặc định và bỏ mặc định địa chỉ khác")
    void setDefaultAddress_Success() {
        UserAddress a1 = UserAddress.builder().id(10L).user(sampleUser).isDefault(true).build();
        UserAddress a2 = UserAddress.builder().id(11L).user(sampleUser).isDefault(false).build();

        when(userAddressRepository.findByIdAndUserId(11L, 1L)).thenReturn(Optional.of(a2));
        when(userAddressRepository.findByUserId(1L)).thenReturn(List.of(a1, a2));

        userAddressService.setDefaultAddress(1L, 11L);

        assertFalse(a1.getIsDefault());
        assertTrue(a2.getIsDefault());
        verify(userAddressRepository).saveAll(List.of(a1, a2));
    }

    @Test
    @DisplayName("createAddress: Mô hình hành chính 2 cấp - district null/trống được lưu thành chuỗi rỗng an toàn")
    void createAddress_TwoLevelAdministrativeDivision_DistrictDefaultsToEmptyString() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userAddressRepository.countByUserId(1L)).thenReturn(0L);
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(i -> i.getArgument(0));

        AddressRequest request = AddressRequest.builder()
                .recipientName("Hồ Quang")
                .recipientPhone("0988888888")
                .provinceCity("Thành phố Hà Nội")
                .district(null) // Cấp huyện được bỏ trong mô hình 2 cấp
                .ward("Phường Dịch Vọng")
                .detailedAddress("Tòa FPT Cầu Giấy")
                .isDefault(true)
                .build();

        UserAddress created = userAddressService.createAddress(1L, request);
        assertNotNull(created);
        assertEquals("", created.getDistrict(), "District phải là chuỗi rỗng khi không cung cấp để tương thích MySQL NOT NULL");
        assertEquals("Phường Dịch Vọng", created.getWard());
        assertEquals("Thành phố Hà Nội", created.getProvinceCity());
        assertTrue(created.getIsDefault());
    }

    @Test
    @DisplayName("updateAddress: Mô hình hành chính 2 cấp - update với district null không ném lỗi NPE")
    void updateAddress_TwoLevelAdministrativeDivision_DistrictNullSafe() {
        UserAddress existing = UserAddress.builder()
                .id(20L)
                .user(sampleUser)
                .recipientName("Cũ")
                .recipientPhone("0900000000")
                .provinceCity("Thành phố Hà Nội")
                .district("Quận Cầu Giấy")
                .ward("Phường Dịch Vọng")
                .detailedAddress("10 Phạm Văn Bạch")
                .isDefault(true)
                .build();

        when(userAddressRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(existing));
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(i -> i.getArgument(0));

        AddressRequest request = AddressRequest.builder()
                .recipientName("Mới")
                .recipientPhone("0911111111")
                .provinceCity("Thành phố Hà Nội")
                .district(null) // 2 cấp mới không truyền district
                .ward("Phường Nghĩa Đô")
                .detailedAddress("20 Hoàng Quốc Việt")
                .isDefault(true)
                .build();

        UserAddress updated = userAddressService.updateAddress(1L, 20L, request);
        assertNotNull(updated);
        assertEquals("", updated.getDistrict());
        assertEquals("Phường Nghĩa Đô", updated.getWard());
    }
}
