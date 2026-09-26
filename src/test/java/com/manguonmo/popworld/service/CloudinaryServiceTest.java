package com.manguonmo.popworld.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.manguonmo.popworld.dto.response.CloudinaryUploadResult;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.service.impl.CloudinaryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CloudinaryServiceImpl cloudinaryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cloudinaryService, "cloudName", "test-cloud");
        ReflectionTestUtils.setField(cloudinaryService, "apiKey", "test-key");
        ReflectionTestUtils.setField(cloudinaryService, "folder", "popworld/products");
    }

    @Test
    @DisplayName("Upload file null hoặc rỗng -> Ném ngoại lệ BadRequestException")
    void uploadImage_NullOrEmptyFile_ThrowsException() {
        assertThrows(BadRequestException.class, () -> cloudinaryService.uploadImage(null));

        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
        assertThrows(BadRequestException.class, () -> cloudinaryService.uploadImage(emptyFile));
    }

    @Test
    @DisplayName("Upload file vượt quá 5MB -> Ném ngoại lệ BadRequestException")
    void uploadImage_FileSizeExceeded_ThrowsException() {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.png", "image/png", largeBytes);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> cloudinaryService.uploadImage(largeFile));
        assertTrue(ex.getMessage().contains("vượt quá giới hạn"));
    }

    @Test
    @DisplayName("Upload file định dạng không được hỗ trợ (PDF) -> Ném ngoại lệ BadRequestException")
    void uploadImage_InvalidContentType_ThrowsException() {
        MockMultipartFile pdfFile = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        BadRequestException ex = assertThrows(BadRequestException.class, () -> cloudinaryService.uploadImage(pdfFile));
        assertTrue(ex.getMessage().contains("Chỉ chấp nhận ảnh định dạng JPG, PNG hoặc WEBP"));
    }

    @Test
    @DisplayName("Upload file khi Cloudinary chưa cấu hình -> Ném ngoại lệ BadRequestException")
    void uploadImage_UnconfiguredCloudinary_ThrowsException() {
        ReflectionTestUtils.setField(cloudinaryService, "cloudName", "");
        MockMultipartFile validFile = new MockMultipartFile("file", "toy.jpg", "image/jpeg", new byte[]{1, 2, 3});

        BadRequestException ex = assertThrows(BadRequestException.class, () -> cloudinaryService.uploadImage(validFile));
        assertTrue(ex.getMessage().contains("Cloudinary chưa được cấu hình"));
    }

    @Test
    @DisplayName("Upload file hợp lệ -> Trả về kết quả chứa secureUrl và publicId")
    void uploadImage_ValidFile_Success() throws IOException {
        MockMultipartFile validFile = new MockMultipartFile("file", "toy.png", "image/png", new byte[]{1, 2, 3, 4});

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/image/upload/v1234/toy.png",
                "public_id", "popworld/products/toy_xyz123"
        ));

        CloudinaryUploadResult result = cloudinaryService.uploadImage(validFile);

        assertNotNull(result);
        assertEquals("https://res.cloudinary.com/demo/image/upload/v1234/toy.png", result.getSecureUrl());
        assertEquals("popworld/products/toy_xyz123", result.getPublicId());
    }

    @Test
    @DisplayName("Xóa ảnh trên Cloudinary thành công")
    void deleteImage_ValidPublicId_Success() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("popworld/products/toy_xyz123"), anyMap())).thenReturn(Map.of("result", "ok"));

        assertDoesNotThrow(() -> cloudinaryService.deleteImage("popworld/products/toy_xyz123"));
        verify(uploader).destroy(eq("popworld/products/toy_xyz123"), anyMap());
    }

    @Test
    @DisplayName("Xóa ảnh với publicId null hoặc rỗng -> Bỏ qua không gọi API")
    void deleteImage_BlankPublicId_DoesNothing() {
        cloudinaryService.deleteImage(null);
        cloudinaryService.deleteImage("   ");

        verifyNoInteractions(cloudinary);
    }
}
