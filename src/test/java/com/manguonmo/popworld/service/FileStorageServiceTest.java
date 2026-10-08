package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.response.CloudinaryUploadResult;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.service.impl.FileStorageServiceImpl;
import org.junit.jupiter.api.AfterEach;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    private static final String TEST_UPLOAD_DIR = "uploads";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileStorageService, "cloudName", "");
        ReflectionTestUtils.setField(fileStorageService, "apiKey", "");
    }

    @AfterEach
    void tearDown() throws IOException {
        Path testDir = Paths.get(TEST_UPLOAD_DIR, "test-subfolder");
        if (Files.exists(testDir)) {
            Files.walk(testDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    @Test
    @DisplayName("storeFile: File null hoặc rỗng -> Ném lỗi BadRequestException")
    void storeFile_NullOrEmpty_ThrowsBadRequestException() {
        assertThrows(BadRequestException.class, () -> fileStorageService.storeFile(null, "avatars"));

        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
        assertThrows(BadRequestException.class, () -> fileStorageService.storeFile(emptyFile, "avatars"));
    }

    @Test
    @DisplayName("storeFile: Kích thước file vượt quá 8MB -> Ném lỗi BadRequestException")
    void storeFile_FileSizeExceeded_ThrowsBadRequestException() {
        byte[] largeBytes = new byte[9 * 1024 * 1024]; // 9MB
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.png", "image/png", largeBytes);

        assertThrows(BadRequestException.class, () -> fileStorageService.storeFile(largeFile, "avatars"));
    }

    @Test
    @DisplayName("storeFile: Định dạng file không hợp lệ (PDF) -> Ném lỗi BadRequestException")
    void storeFile_InvalidContentType_ThrowsBadRequestException() {
        MockMultipartFile pdfFile = new MockMultipartFile("file", "document.pdf", "application/pdf", "fake content".getBytes());

        assertThrows(BadRequestException.class, () -> fileStorageService.storeFile(pdfFile, "avatars"));
    }

    @Test
    @DisplayName("storeFile: Lưu trữ cục bộ thành công khi Cloudinary chưa cấu hình")
    void storeFile_LocalStorage_Success() {
        MockMultipartFile validImage = new MockMultipartFile(
                "file",
                "test-avatar.png",
                "image/png",
                "sample-image-bytes".getBytes()
        );

        String resultPath = fileStorageService.storeFile(validImage, "test-subfolder");

        assertNotNull(resultPath);
        assertTrue(resultPath.startsWith("/uploads/test-subfolder/"));
        assertTrue(resultPath.endsWith(".png"));

        // Kiểm tra file thực tế đã được tạo trên đĩa
        String filename = resultPath.replace("/uploads/test-subfolder/", "");
        Path diskFile = Paths.get(TEST_UPLOAD_DIR, "test-subfolder", filename);
        assertTrue(Files.exists(diskFile));
    }

    @Test
    @DisplayName("storeFile: Ưu tiên Cloudinary khi có cấu hình API Key")
    void storeFile_CloudinaryConfigured_UsesCloudinary() {
        ReflectionTestUtils.setField(fileStorageService, "cloudName", "my-cloud");
        ReflectionTestUtils.setField(fileStorageService, "apiKey", "my-api-key");

        MockMultipartFile validImage = new MockMultipartFile(
                "file",
                "test-cloud.jpg",
                "image/jpeg",
                "sample-image-bytes".getBytes()
        );

        when(cloudinaryService.uploadImage(any(), any())).thenReturn(
                CloudinaryUploadResult.builder()
                        .secureUrl("https://res.cloudinary.com/my-cloud/image/upload/sample.jpg")
                        .publicId("sample-id")
                        .build()
        );

        String resultPath = fileStorageService.storeFile(validImage, "avatars");

        assertNotNull(resultPath);
        assertEquals("https://res.cloudinary.com/my-cloud/image/upload/sample.jpg", resultPath);
        verify(cloudinaryService, times(1)).uploadImage(validImage, "avatars");
    }

    @Test
    @DisplayName("deleteFile: Xóa file ảnh cục bộ tồn tại trên đĩa")
    void deleteFile_DeletesLocalFile() throws IOException {
        Path testDir = Paths.get(TEST_UPLOAD_DIR, "test-subfolder");
        Files.createDirectories(testDir);
        Path sampleFile = testDir.resolve("sample-to-delete.png");
        Files.writeString(sampleFile, "delete me");

        assertTrue(Files.exists(sampleFile));

        fileStorageService.deleteFile("/uploads/test-subfolder/sample-to-delete.png");

        assertFalse(Files.exists(sampleFile));
    }
}
