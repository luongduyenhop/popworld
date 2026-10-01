package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.response.CloudinaryUploadResult;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.service.CloudinaryService;
import com.manguonmo.popworld.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private static final long MAX_FILE_SIZE = 8 * 1024 * 1024; // 8 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/jpg",
            "image/gif"
    );

    private final CloudinaryService cloudinaryService;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Override
    public String storeFile(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn tập tin ảnh để tải lên.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Dung lượng file ảnh không được vượt quá 8MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Định dạng file không được hỗ trợ! Chỉ chấp nhận ảnh JPG, PNG, WEBP hoặc GIF.");
        }

        String safeSubFolder = (subFolder != null && !subFolder.isBlank())
                ? subFolder.replaceAll("[^a-zA-Z0-9_-]", "")
                : "common";

        // Chiến lược 1: Nếu Cloudinary đã được cấu hình đầy đủ, ưu tiên upload lên Cloud CDN
        if (cloudName != null && !cloudName.isBlank() && apiKey != null && !apiKey.isBlank()) {
            try {
                CloudinaryUploadResult result = cloudinaryService.uploadImage(file);
                if (result != null && result.getSecureUrl() != null && !result.getSecureUrl().isBlank()) {
                    log.info("Đã tải ảnh lên Cloudinary thành công: {}", result.getSecureUrl());
                    return result.getSecureUrl();
                }
            } catch (Exception e) {
                log.warn("Upload lên Cloudinary thất bại ({}), chuyển sang lưu trữ cục bộ...", e.getMessage());
            }
        }

        // Chiến lược 2: Lưu trữ cục bộ an toàn trong thư mục ./uploads/{safeSubFolder}/
        try {
            Path targetDir = Paths.get("uploads", safeSubFolder);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = ".jpg";
            if (originalFilename != null && originalFilename.contains(".")) {
                String rawExt = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
                if (rawExt.matches("^\\.(jpe?g|png|webp|gif)$")) {
                    extension = rawExt;
                }
            }

            String uniqueFilename = UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + extension;
            Path destination = targetDir.resolve(uniqueFilename);

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            log.info("Đã lưu file ảnh cục bộ thành công: {}", destination.toAbsolutePath());

            return "/uploads/" + safeSubFolder + "/" + uniqueFilename;
        } catch (IOException e) {
            log.error("Lỗi khi lưu trữ file cục bộ: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể lưu trữ tập tin ảnh trên máy chủ: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        if (fileUrl.startsWith("/uploads/")) {
            try {
                String relativePath = fileUrl.substring("/uploads/".length());
                Path filePath = Paths.get("uploads").resolve(relativePath);
                if (Files.exists(filePath)) {
                    Files.deleteIfExists(filePath);
                    log.info("Đã xóa file ảnh cục bộ: {}", filePath.toAbsolutePath());
                }
            } catch (Exception e) {
                log.warn("Không thể xóa file ảnh cục bộ: {}", e.getMessage());
            }
        }
    }
}
