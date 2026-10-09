package com.manguonmo.popworld.security.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Tiện ích mã hóa xác thực chữ ký điện tử HMAC-SHA256.
 * 
 * Sử dụng Java Cryptography Architecture (JCA) chuẩn không phụ thuộc thư viện ngoài.
 * Sử dụng MessageDigest.isEqual để chống tấn công Timing Attack.
 */
@Slf4j
@Component
public class HmacSignatureVerifier {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    /**
     * Tính toán chuỗi băm HMAC-SHA256 dưới định dạng Hexadecimal chữ thường.
     *
     * @param data Dữ liệu cần băm (payload raw string)
     * @param secret Khóa bí mật chung giữa 2 bên
     * @return Chuỗi Hex 64 ký tự hoặc null nếu đầu vào không hợp lệ
     */
    public String calculateHmacSha256(String data, String secret) {
        if (data == null || secret == null || secret.isBlank()) {
            return null;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.trim().getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Lỗi khi tính toán HMAC-SHA256: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Xác minh tính hợp lệ của chữ ký điện tử HMAC-SHA256.
     * So sánh chuỗi bằng MessageDigest.isEqual để triệt tiêu lỗ hổng Timing Attack.
     *
     * @param data Dữ liệu payload nhận được
     * @param providedSignature Chữ ký do bên gửi cung cấp (ví dụ trong header X-Signature)
     * @param secret Khóa bí mật đã cấu hình
     * @return true nếu chữ ký hợp lệ và toàn vẹn, false nếu không hợp lệ hoặc có lỗi
     */
    public boolean verifySignature(String data, String providedSignature, String secret) {
        if (data == null || providedSignature == null || secret == null) {
            return false;
        }

        String trimmedProvided = providedSignature.trim().toLowerCase();
        if (trimmedProvided.isEmpty()) {
            return false;
        }

        String expectedSignature = calculateHmacSha256(data, secret);
        if (expectedSignature == null) {
            return false;
        }

        byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = trimmedProvided.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(expectedBytes, providedBytes);
    }
}
