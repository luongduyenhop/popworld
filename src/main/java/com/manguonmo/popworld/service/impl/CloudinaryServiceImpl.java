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

    @Override
    public CloudinaryUploadResult uploadImage(MultipartFile file) {
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
            throw new BadRequestException("Cloudinary chưa được cấu hình. Vui lòng thiết lập biến môi trường CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET.");
        }

        try {
            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", folder,
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

    @Override
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
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
