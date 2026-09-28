package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.request.BlindBoxItemFormRequest;
import com.manguonmo.popworld.dto.response.PopNowAdminProductSummary;
import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.BlindBoxItemRepository;
import com.manguonmo.popworld.repository.BlindBoxSlotRepository;
import com.manguonmo.popworld.repository.OwnedItemRepository;
import com.manguonmo.popworld.repository.ProductRepository;
import com.manguonmo.popworld.service.impl.PopNowAdminServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PopNowAdminServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BlindBoxItemRepository blindBoxItemRepository;

    @Mock
    private BlindBoxSlotRepository blindBoxSlotRepository;

    @Mock
    private OwnedItemRepository ownedItemRepository;

    @InjectMocks
    private PopNowAdminServiceImpl popNowAdminService;

    private Product sampleProduct;
    private BlindBoxItem activeItem1;
    private BlindBoxItem activeItem2;

    @BeforeEach
    void setUp() {
        Category category = Category.builder().id(1L).name("Art Toys").build();
        sampleProduct = Product.builder()
                .id(10L)
                .name("Skullpanda City of Night")
                .slug("skullpanda-city-of-night")
                .category(category)
                .stockQuantity(50)
                .singlePrice(BigDecimal.valueOf(250000))
                .active(true)
                .build();

        activeItem1 = BlindBoxItem.builder()
                .id(101L)
                .product(sampleProduct)
                .name("The Night City")
                .rarity(RarityType.REGULAR)
                .imageUrl("/images/item1.png")
                .probabilityWeight(100)
                .stockQuantity(10)
                .active(true)
                .build();

        activeItem2 = BlindBoxItem.builder()
                .id(102L)
                .product(sampleProduct)
                .name("Cyber Mirage")
                .rarity(RarityType.SECRET)
                .imageUrl("/images/item2.png")
                .probabilityWeight(10)
                .stockQuantity(2)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("getPopNowProductSummaries: Trả về danh sách tóm tắt và thống kê slot / item chính xác")
    void getPopNowProductSummaries_Success() {
        when(productRepository.findAll(any(Sort.class))).thenReturn(List.of(sampleProduct));
        when(blindBoxItemRepository.findByProductId(10L)).thenReturn(List.of(activeItem1, activeItem2));
        when(blindBoxItemRepository.countByProductIdAndActiveTrue(10L)).thenReturn(2L);
        when(blindBoxSlotRepository.countByProductIdAndStatus(10L, SlotStatus.AVAILABLE)).thenReturn(8L);
        when(blindBoxSlotRepository.countByProductIdAndStatus(10L, SlotStatus.HELD)).thenReturn(2L);
        when(blindBoxSlotRepository.countByProductIdAndStatus(10L, SlotStatus.SOLD)).thenReturn(2L);

        List<PopNowAdminProductSummary> summaries = popNowAdminService.getPopNowProductSummaries();

        assertNotNull(summaries);
        assertEquals(1, summaries.size());
        PopNowAdminProductSummary s = summaries.get(0);
        assertEquals(10L, s.getProductId());
        assertEquals("Skullpanda City of Night", s.getProductName());
        assertEquals("Art Toys", s.getCategoryName());
        assertEquals(2, s.getTotalItems());
        assertEquals(2, s.getActiveItems());
        assertEquals(8, s.getAvailableSlots());
        assertEquals(2, s.getHeldSlots());
        assertEquals(2, s.getSoldSlots());
        assertEquals(12, s.getTotalSlots());
        assertTrue(s.isConfigReady());
    }

    @Test
    @DisplayName("getProductForConfig: Thành công khi tìm thấy sản phẩm")
    void getProductForConfig_Success() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));

        Product product = popNowAdminService.getProductForConfig(10L);

        assertNotNull(product);
        assertEquals(10L, product.getId());
    }

    @Test
    @DisplayName("getProductForConfig: Ném ResourceNotFoundException nếu không tìm thấy")
    void getProductForConfig_NotFound_ThrowsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> popNowAdminService.getProductForConfig(99L));
    }

    @Test
    @DisplayName("getProductForConfig: Ném BadRequestException nếu ID null")
    void getProductForConfig_NullId_ThrowsException() {
        assertThrows(BadRequestException.class, () -> popNowAdminService.getProductForConfig(null));
    }

    @Test
    @DisplayName("getItemById: Ném BadRequestException nếu itemId null")
    void getItemById_NullItemId_ThrowsException() {
        assertThrows(BadRequestException.class, () -> popNowAdminService.getItemById(10L, null));
    }

    @Test
    @DisplayName("getItemById: Ném BadRequestException nếu item không thuộc productId chỉ định (IDOR)")
    void getItemById_IdorMismatch_ThrowsException() {
        Product otherProduct = Product.builder().id(20L).name("Other").build();
        BlindBoxItem otherItem = BlindBoxItem.builder().id(50L).product(otherProduct).build();
        when(blindBoxItemRepository.findById(50L)).thenReturn(Optional.of(otherItem));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> popNowAdminService.getItemById(10L, 50L));
        assertTrue(ex.getMessage().contains("không thuộc về sản phẩm"));
    }

    @Test
    @DisplayName("saveItem: Tạo mới item thành công")
    void saveItem_CreateNew_Success() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));

        BlindBoxItemFormRequest request = BlindBoxItemFormRequest.builder()
                .name("New Model")
                .rarity(RarityType.REGULAR)
                .imageUrl("/images/new.png")
                .probabilityWeight(50)
                .stockQuantity(15)
                .active(true)
                .build();

        BlindBoxItem savedItem = BlindBoxItem.builder()
                .id(200L)
                .product(sampleProduct)
                .name("New Model")
                .rarity(RarityType.REGULAR)
                .imageUrl("/images/new.png")
                .probabilityWeight(50)
                .stockQuantity(15)
                .active(true)
                .build();

        when(blindBoxItemRepository.save(any(BlindBoxItem.class))).thenReturn(savedItem);

        BlindBoxItem result = popNowAdminService.saveItem(10L, null, request);

        assertNotNull(result);
        assertEquals(200L, result.getId());
        assertEquals("New Model", result.getName());
        verify(blindBoxItemRepository).save(any(BlindBoxItem.class));
    }

    @Test
    @DisplayName("saveItem: Cập nhật item thành công")
    void saveItem_Update_Success() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
        when(blindBoxItemRepository.findById(101L)).thenReturn(Optional.of(activeItem1));
        when(blindBoxItemRepository.save(any(BlindBoxItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BlindBoxItemFormRequest request = BlindBoxItemFormRequest.builder()
                .name("Updated Name")
                .rarity(RarityType.SECRET)
                .imageUrl("/images/updated.png")
                .probabilityWeight(80)
                .stockQuantity(5)
                .active(true)
                .build();

        BlindBoxItem updated = popNowAdminService.saveItem(10L, 101L, request);

        assertEquals("Updated Name", updated.getName());
        assertEquals(RarityType.SECRET, updated.getRarity());
        assertEquals("/images/updated.png", updated.getImageUrl());
        assertEquals(80, updated.getProbabilityWeight());
    }

    @Test
    @DisplayName("saveItem: Ném BadRequestException nếu tắt active của item duy nhất còn hoạt động")
    void saveItem_DeactivateOnlyActiveItem_ThrowsBadRequestException() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
        when(blindBoxItemRepository.findById(101L)).thenReturn(Optional.of(activeItem1));
        // Chỉ có duy nhất 1 item active là item 101L
        when(blindBoxItemRepository.findByProductIdAndActiveTrue(10L)).thenReturn(List.of(activeItem1));

        BlindBoxItemFormRequest request = BlindBoxItemFormRequest.builder()
                .name("The Night City")
                .rarity(RarityType.REGULAR)
                .imageUrl("/images/item1.png")
                .probabilityWeight(100)
                .stockQuantity(10)
                .active(false) // Thử tắt
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> popNowAdminService.saveItem(10L, 101L, request));
        assertTrue(ex.getMessage().contains("mô hình hoạt động duy nhất"));
    }

    @Test
    @DisplayName("saveItem: Ném BadRequestException khi thiếu trường bắt buộc")
    void saveItem_ValidationFailures_ThrowsBadRequestException() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));

        assertThrows(BadRequestException.class, () -> popNowAdminService.saveItem(10L, null, null));

        BlindBoxItemFormRequest noName = BlindBoxItemFormRequest.builder().name("").build();
        assertThrows(BadRequestException.class, () -> popNowAdminService.saveItem(10L, null, noName));

        BlindBoxItemFormRequest noImage = BlindBoxItemFormRequest.builder().name("Item").imageUrl("").build();
        assertThrows(BadRequestException.class, () -> popNowAdminService.saveItem(10L, null, noImage));

        BlindBoxItemFormRequest badWeight = BlindBoxItemFormRequest.builder().name("Item").imageUrl("/img.png").probabilityWeight(0).build();
        assertThrows(BadRequestException.class, () -> popNowAdminService.saveItem(10L, null, badWeight));

        BlindBoxItemFormRequest noRarity = BlindBoxItemFormRequest.builder().name("Item").imageUrl("/img.png").probabilityWeight(10).rarity(null).build();
        assertThrows(BadRequestException.class, () -> popNowAdminService.saveItem(10L, null, noRarity));
    }

    @Test
    @DisplayName("toggleItemActive: Bật thành tắt khi có nhiều hơn 1 item active -> Thành công")
    void toggleItemActive_TurnOff_Success() {
        when(blindBoxItemRepository.findById(101L)).thenReturn(Optional.of(activeItem1));
        when(blindBoxItemRepository.findByProductIdAndActiveTrue(10L)).thenReturn(List.of(activeItem1, activeItem2));

        popNowAdminService.toggleItemActive(10L, 101L);

        assertFalse(activeItem1.getActive());
        verify(blindBoxItemRepository).save(activeItem1);
    }

    @Test
    @DisplayName("toggleItemActive: Bật thành tắt khi chỉ có 1 item active -> Bị từ chối")
    void toggleItemActive_TurnOffOnlyActiveItem_ThrowsBadRequestException() {
        when(blindBoxItemRepository.findById(101L)).thenReturn(Optional.of(activeItem1));
        when(blindBoxItemRepository.findByProductIdAndActiveTrue(10L)).thenReturn(List.of(activeItem1));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> popNowAdminService.toggleItemActive(10L, 101L));
        assertTrue(ex.getMessage().contains("mô hình duy nhất của Series"));
    }

    @Test
    @DisplayName("toggleItemActive: Đang tắt bật lên -> Thành công")
    void toggleItemActive_TurnOn_Success() {
        activeItem1.setActive(false);
        when(blindBoxItemRepository.findById(101L)).thenReturn(Optional.of(activeItem1));

        popNowAdminService.toggleItemActive(10L, 101L);

        assertTrue(activeItem1.getActive());
        verify(blindBoxItemRepository).save(activeItem1);
    }

    @Test
    @DisplayName("deleteItem: Xóa an toàn khi chưa ai sở hữu và còn item active khác")
    void deleteItem_SafeDelete_Success() {
        when(blindBoxItemRepository.findById(101L)).thenReturn(Optional.of(activeItem1));
        when(ownedItemRepository.existsByBlindBoxItemId(101L)).thenReturn(false);
        when(blindBoxItemRepository.findByProductIdAndActiveTrue(10L)).thenReturn(List.of(activeItem1, activeItem2));

        popNowAdminService.deleteItem(10L, 101L);

        verify(blindBoxItemRepository).delete(activeItem1);
    }

    @Test
    @DisplayName("deleteItem: Bị từ chối khi mô hình đã có khách hàng sở hữu trong Virtual Cabinet")
    void deleteItem_OwnedInCabinet_ThrowsBadRequestException() {
        when(blindBoxItemRepository.findById(101L)).thenReturn(Optional.of(activeItem1));
        when(ownedItemRepository.existsByBlindBoxItemId(101L)).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> popNowAdminService.deleteItem(10L, 101L));
        assertTrue(ex.getMessage().contains("tủ đồ ảo"));
        verify(blindBoxItemRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteItem: Bị từ chối khi là item hoạt động duy nhất")
    void deleteItem_OnlyActiveItem_ThrowsBadRequestException() {
        when(blindBoxItemRepository.findById(101L)).thenReturn(Optional.of(activeItem1));
        when(ownedItemRepository.existsByBlindBoxItemId(101L)).thenReturn(false);
        when(blindBoxItemRepository.findByProductIdAndActiveTrue(10L)).thenReturn(List.of(activeItem1));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> popNowAdminService.deleteItem(10L, 101L));
        assertTrue(ex.getMessage().contains("duy nhất đang hoạt động"));
        verify(blindBoxItemRepository, never()).delete(any());
    }

    @Test
    @DisplayName("initializeStandardSlots: Khởi tạo đầy đủ 12 slots khi chưa có slot nào")
    void initializeStandardSlots_AllMissing_Initializes12Slots() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
        for (int i = 1; i <= 12; i++) {
            when(blindBoxSlotRepository.findByProductIdAndSlotIndex(10L, i)).thenReturn(Optional.empty());
        }

        int created = popNowAdminService.initializeStandardSlots(10L);

        assertEquals(12, created);
        verify(blindBoxSlotRepository, times(12)).save(any(BlindBoxSlot.class));
    }

    @Test
    @DisplayName("initializeStandardSlots: Không ghi đè hay thay đổi các slot đã tồn tại (HELD/SOLD)")
    void initializeStandardSlots_PreservesExistingSlots() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));

        // Slot 1..3 đã tồn tại
        BlindBoxSlot slot1 = BlindBoxSlot.builder().id(1L).product(sampleProduct).slotIndex(1).status(SlotStatus.HELD).build();
        BlindBoxSlot slot2 = BlindBoxSlot.builder().id(2L).product(sampleProduct).slotIndex(2).status(SlotStatus.SOLD).build();
        BlindBoxSlot slot3 = BlindBoxSlot.builder().id(3L).product(sampleProduct).slotIndex(3).status(SlotStatus.AVAILABLE).build();

        when(blindBoxSlotRepository.findByProductIdAndSlotIndex(10L, 1)).thenReturn(Optional.of(slot1));
        when(blindBoxSlotRepository.findByProductIdAndSlotIndex(10L, 2)).thenReturn(Optional.of(slot2));
        when(blindBoxSlotRepository.findByProductIdAndSlotIndex(10L, 3)).thenReturn(Optional.of(slot3));

        for (int i = 4; i <= 12; i++) {
            when(blindBoxSlotRepository.findByProductIdAndSlotIndex(10L, i)).thenReturn(Optional.empty());
        }

        int created = popNowAdminService.initializeStandardSlots(10L);

        assertEquals(9, created);
        // Chỉ lưu 9 slot mới
        verify(blindBoxSlotRepository, times(9)).save(any(BlindBoxSlot.class));
    }

    @Test
    @DisplayName("getSlotsByProductId: Trả về danh sách slots theo thứ tự")
    void getSlotsByProductId_Success() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
        when(blindBoxSlotRepository.findByProductIdOrderBySlotIndexAsc(10L)).thenReturn(Collections.emptyList());

        List<BlindBoxSlot> slots = popNowAdminService.getSlotsByProductId(10L);

        assertNotNull(slots);
        verify(blindBoxSlotRepository).findByProductIdOrderBySlotIndexAsc(10L);
    }
}
