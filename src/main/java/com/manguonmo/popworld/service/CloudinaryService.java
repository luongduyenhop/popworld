package com.manguonmo.popworld.service;

import com.manguonmo.popworld.dto.response.CloudinaryUploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
    CloudinaryUploadResult uploadImage(MultipartFile file);
    void deleteImage(String publicId);
}
