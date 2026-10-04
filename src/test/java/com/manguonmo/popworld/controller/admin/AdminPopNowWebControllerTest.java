package com.manguonmo.popworld.controller.admin;

import com.manguonmo.popworld.dto.request.BlindBoxItemFormRequest;
import com.manguonmo.popworld.dto.response.PopNowAdminProductSummary;
import com.manguonmo.popworld.entity.BlindBoxItem;
import com.manguonmo.popworld.entity.BlindBoxSlot;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.RarityType;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.service.PopNowAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPopNowWebControllerTest {

    @Mock
    private PopNowAdminService popNowAdminService;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private AdminPopNowWebController controller;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder().id(10L).name("Dimoo Jurassic").slug("dimoo-jurassic").build();
    }

    @Test
    @DisplayName("listPopNowProducts: Hiển thị danh sách tóm tắt POP NOW cho admin")
    void listPopNowProducts_Success() {
        List<PopNowAdminProductSummary> summaries = List.of(
                PopNowAdminProductSummary.builder().productId(10L).productName("Dimoo Jurassic").build()
        );
        when(popNowAdminService.getPopNowProductSummaries()).thenReturn(summaries);

        String view = controller.listPopNowProducts(model);

        assertEquals("admin/popnow-list", view);
        verify(model).addAttribute("summaries", summaries);
        verify(model).addAttribute("activeItem", "popnow");
    }

    @Test
    @DisplayName("productConfigDetail: Hiển thị chi tiết cấu hình mô hình và 12 slot")
    void productConfigDetail_Success() {
        when(popNowAdminService.getProductForConfig(10L)).thenReturn(sampleProduct);
        when(popNowAdminService.getItemsByProductId(10L)).thenReturn(Collections.emptyList());
        when(popNowAdminService.getSlotsByProductId(10L)).thenReturn(Collections.emptyList());

        String view = controller.productConfigDetail(10L, model);

        assertEquals("admin/popnow-detail", view);
        verify(model).addAttribute("product", sampleProduct);
        verify(model).addAttribute(eq("items"), anyList());
        verify(model).addAttribute(eq("slots"), anyList());
        verify(model).addAttribute(eq("rarities"), any());
        verify(model).addAttribute("activeItem", "popnow");
    }

    @Test
    @DisplayName("initSlots: Khởi tạo thành công slot mới -> Gửi flash success")
    void initSlots_CreatedNew_Success() {
        when(popNowAdminService.initializeSlots(10L, null)).thenReturn(12);

        String view = controller.initSlots(10L, null, redirectAttributes);

        assertEquals("redirect:/admin/popnow/10", view);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("Đã khởi tạo thành công 12 ô hộp"));
    }

    @Test
    @DisplayName("initSlots: Đã đủ 12 slots -> Gửi flash info")
    void initSlots_AlreadyComplete_InfoMessage() {
        when(popNowAdminService.initializeSlots(10L, null)).thenReturn(0);

        String view = controller.initSlots(10L, null, redirectAttributes);

        assertEquals("redirect:/admin/popnow/10", view);
        verify(redirectAttributes).addFlashAttribute(eq("infoMessage"), contains("đã tồn tại đầy đủ"));
    }

    @Test
    @DisplayName("initSlots: Lỗi ngoại lệ -> Gửi flash error")
    void initSlots_Exception_ErrorMessage() {
        when(popNowAdminService.initializeSlots(10L, null)).thenThrow(new RuntimeException("DB Connection Timeout"));

        String view = controller.initSlots(10L, null, redirectAttributes);

        assertEquals("redirect:/admin/popnow/10", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("Không thể khởi tạo"));
    }

    @Test
    @DisplayName("createItem: Lỗi validation -> Gửi flash error và redirect")
    void createItem_ValidationErrors_RedirectsWithError() {
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getAllErrors()).thenReturn(List.of(new ObjectError("itemForm", "Tên mô hình không được để trống")));

        BlindBoxItemFormRequest request = BlindBoxItemFormRequest.builder().build();

        String view = controller.createItem(10L, request, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/popnow/10", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("Dữ liệu không hợp lệ"));
        verify(popNowAdminService, never()).saveItem(any(), any(), any());
    }

    @Test
    @DisplayName("createItem: Hợp lệ -> Lưu thành công và gửi flash success")
    void createItem_Valid_Success() {
        when(bindingResult.hasErrors()).thenReturn(false);
        BlindBoxItemFormRequest request = BlindBoxItemFormRequest.builder()
                .name("Baby Dino")
                .rarity(RarityType.REGULAR)
                .imageUrl("/img.png")
                .probabilityWeight(100)
                .build();
        BlindBoxItem saved = BlindBoxItem.builder().id(50L).name("Baby Dino").build();
        when(popNowAdminService.saveItem(eq(10L), isNull(), eq(request))).thenReturn(saved);

        String view = controller.createItem(10L, request, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/popnow/10", view);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("Thêm mô hình \"Baby Dino\" vào Series thành công!"));
    }

    @Test
    @DisplayName("createItem: Service ném BadRequestException -> Gửi flash error")
    void createItem_ServiceBadRequest_ErrorMessage() {
        when(bindingResult.hasErrors()).thenReturn(false);
        BlindBoxItemFormRequest request = BlindBoxItemFormRequest.builder().name("Dino").build();
        when(popNowAdminService.saveItem(eq(10L), isNull(), eq(request))).thenThrow(new BadRequestException("Trọng số không hợp lệ"));

        String view = controller.createItem(10L, request, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/popnow/10", view);
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Trọng số không hợp lệ");
    }

    @Test
    @DisplayName("updateItem: Hợp lệ -> Cập nhật thành công")
    void updateItem_Valid_Success() {
        when(bindingResult.hasErrors()).thenReturn(false);
        BlindBoxItemFormRequest request = BlindBoxItemFormRequest.builder()
                .name("Updated Dino")
                .build();
        BlindBoxItem saved = BlindBoxItem.builder().id(50L).name("Updated Dino").build();
        when(popNowAdminService.saveItem(eq(10L), eq(50L), eq(request))).thenReturn(saved);

        String view = controller.updateItem(10L, 50L, request, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/popnow/10", view);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("Cập nhật mô hình \"Updated Dino\" thành công!"));
    }

    @Test
    @DisplayName("updateItem: Validation có lỗi -> Redirect với thông báo lỗi")
    void updateItem_ValidationErrors_RedirectsWithError() {
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getAllErrors()).thenReturn(List.of(new ObjectError("itemForm", "Đường dẫn ảnh không được để trống")));

        BlindBoxItemFormRequest request = BlindBoxItemFormRequest.builder().build();

        String view = controller.updateItem(10L, 50L, request, bindingResult, redirectAttributes);

        assertEquals("redirect:/admin/popnow/10", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("Dữ liệu không hợp lệ"));
    }

    @Test
    @DisplayName("toggleItemActive: Thành công -> Gửi flash success")
    void toggleItemActive_Success() {
        doNothing().when(popNowAdminService).toggleItemActive(10L, 50L);

        String view = controller.toggleItemActive(10L, 50L, redirectAttributes);

        assertEquals("redirect:/admin/popnow/10", view);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("Thay đổi trạng thái kích hoạt"));
    }

    @Test
    @DisplayName("toggleItemActive: Service ném lỗi nghiệp vụ -> Gửi flash error")
    void toggleItemActive_BadRequest_ErrorMessage() {
        doThrow(new BadRequestException("Không thể tắt kích hoạt mô hình duy nhất")).when(popNowAdminService).toggleItemActive(10L, 50L);

        String view = controller.toggleItemActive(10L, 50L, redirectAttributes);

        assertEquals("redirect:/admin/popnow/10", view);
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Không thể tắt kích hoạt mô hình duy nhất");
    }

    @Test
    @DisplayName("deleteItem: Thành công -> Gửi flash success")
    void deleteItem_Success() {
        doNothing().when(popNowAdminService).deleteItem(10L, 50L);

        String view = controller.deleteItem(10L, 50L, redirectAttributes);

        assertEquals("redirect:/admin/popnow/10", view);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), contains("Xóa mô hình thành công!"));
    }

    @Test
    @DisplayName("deleteItem: Có người sở hữu trong tủ đồ -> Gửi flash error ngăn chặn")
    void deleteItem_OwnedInCabinet_ErrorMessage() {
        doThrow(new BadRequestException("Mô hình này đã có người chơi sở hữu trong tủ đồ ảo!")).when(popNowAdminService).deleteItem(10L, 50L);

        String view = controller.deleteItem(10L, 50L, redirectAttributes);

        assertEquals("redirect:/admin/popnow/10", view);
        verify(redirectAttributes).addFlashAttribute(eq("errorMessage"), contains("tủ đồ ảo"));
    }
}
