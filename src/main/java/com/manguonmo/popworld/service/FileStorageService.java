package com.manguonmo.popworld.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Lưu file ảnh vào hệ thống lưu trữ (Cloudinary nếu có cấu hình, hoặc lưu cục bộ vào ./uploads/{subFolder}/)
     *
     * @param file      Tập tin tải lên
     * @param subFolder Thư mục con phân loại (ví dụ: "avatars", "reviews")
     * @return Đường dẫn truy cập ảnh (URL tuyệt đối của Cloud CDN hoặc URL tĩnh /uploads/{subFolder}/filename)
     */
    String storeFile(MultipartFile file, String subFolder);

    /**
     * Xóa file ảnh cũ khỏi hệ thống lưu trữ nếu là file tải lên cục bộ hoặc trên Cloudinary
     *
     * @param fileUrl Đường dẫn ảnh cần xóa
     */
    void deleteFile(String fileUrl);
}
