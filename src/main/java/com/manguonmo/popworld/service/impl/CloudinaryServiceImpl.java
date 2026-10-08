package com.manguonmo.popworld.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.manguonmo.popworld.dto.response.CloudinaryUploadResult;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/jpg"
    );

    private final Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.folder:popworld/products}")
    private String folder;

    @Value("${cloudinary.local-fallback:true}")
    private boolean localFallback = true;

    @Override
    public CloudinaryUploadResult uploadImage(MultipartFile file) {
        return uploadImage(file, null);
    }

    @Override
    public CloudinaryUploadResult uploadImage(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File ảnh tải lên không được để trống.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Kích thước file ảnh vượt quá giới hạn cho phép (tối đa 5MB).");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Định dạng file không được hỗ trợ! Chỉ chấp nhận ảnh định dạng JPG, PNG hoặc WEBP.");
        }

        if (cloudName == null || cloudName.isBlank() || apiKey == null || apiKey.isBlank()) {
            if (localFallback) {
                log.info("Cloudinary API Key chưa cấu hình. Kích hoạt Local Storage Fallback lưu ảnh vào thư mục uploads/images/");
                return saveLocally(file);
            }
            throw new BadRequestException("Cloudinary chưa được cấu hình. Vui lòng thiết lập biến môi trường CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET.");
        }

        try {
            String targetFolder = folder;
            if (subFolder != null && !subFolder.isBlank()) {
                targetFolder = "popworld/" + subFolder.replaceAll("[^a-zA-Z0-9_-]", "");
            }
            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", targetFolder,
                    "resource_type", "image"
            );
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

            String secureUrl = (String) uploadResult.get("secure_url");
            if (secureUrl == null) {
                secureUrl = (String) uploadResult.get("url");
            }
            String publicId = (String) uploadResult.get("public_id");

            log.info("Tải ảnh lên Cloudinary thành công: publicId={}, url={}", publicId, secureUrl);

            return CloudinaryUploadResult.builder()
                    .secureUrl(secureUrl)
                    .publicId(publicId)
                    .build();
        } catch (IOException e) {
            log.error("Lỗi I/O khi tải ảnh lên Cloudinary: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể tải ảnh lên hệ thống lưu trữ Cloudinary: " + e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi tải ảnh lên Cloudinary: {}", e.getMessage(), e);
            throw new BadRequestException("Lỗi tải ảnh Cloudinary: " + e.getMessage());
        }
    }

    private CloudinaryUploadResult saveLocally(MultipartFile file) {
        try {
            java.nio.file.Path uploadPath = java.nio.file.Paths.get("uploads", "images");
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = ".jpg";
            if (originalFilename != null && originalFilename.lastIndexOf('.') > 0) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }
            String publicId = "local_" + java.util.UUID.randomUUID().toString();
            String storedFilename = publicId + extension;
            java.nio.file.Path destination = uploadPath.resolve(storedFilename);

            java.nio.file.Files.copy(file.getInputStream(), destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String localUrl = "/uploads/images/" + storedFilename;
            log.info("Lưu trữ ảnh cục bộ thành công: publicId={}, url={}", publicId, localUrl);

            return CloudinaryUploadResult.builder()
                    .secureUrl(localUrl)
                    .publicId(publicId)
                    .build();
        } catch (IOException e) {
            log.error("Lỗi khi lưu trữ ảnh cục bộ: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể lưu trữ file ảnh: " + e.getMessage());
        }
    }

    @Override
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        if (publicId.startsWith("local_")) {
            try {
                java.nio.file.Path uploadPath = java.nio.file.Paths.get("uploads", "images");
                if (java.nio.file.Files.exists(uploadPath)) {
                    try (var stream = java.nio.file.Files.list(uploadPath)) {
                        stream.filter(p -> p.getFileName().toString().startsWith(publicId))
                                .forEach(p -> {
                                    try {
                                        java.nio.file.Files.deleteIfExists(p);
                                        log.info("Đã xóa ảnh lưu trữ cục bộ: {}", p);
                                    } catch (IOException ignored) {}
                                });
                    }
                }
            } catch (Exception e) {
                log.warn("Lỗi khi xóa file ảnh cục bộ: {}", e.getMessage());
            }
            return;
        }

        if (cloudName == null || cloudName.isBlank() || apiKey == null || apiKey.isBlank()) {
            log.warn("Cloudinary chưa được cấu hình, bỏ qua xóa ảnh publicId={}", publicId);
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Đã xóa ảnh thành công trên Cloudinary: publicId={}", publicId);
        } catch (Exception e) {
            log.warn("Không thể xóa ảnh trên Cloudinary (publicId={}): {}", publicId, e.getMessage());
        }
    }
}
