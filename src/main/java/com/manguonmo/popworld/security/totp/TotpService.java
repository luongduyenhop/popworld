package com.manguonmo.popworld.security.totp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Dịch vụ Xác thực Hai yếu tố TOTP (Time-based One-Time Password) theo chuẩn RFC 6238 và RFC 4226.
 * 
 * - Chuần mã hóa HMAC-SHA1 với cửa sổ thời gian 30 giây (Step = 30s).
 * - Tự động hỗ trợ độ lệch đồng hồ (Clock Skew) trong khoảng [-30s, +30s].
 * - Sử dụng MessageDigest.isEqual để chống tấn công phân tích thời gian (Timing Attack).
 * - Cài đặt thuần Java tiêu chuẩn, không phụ thuộc thư viện bên ngoài.
 */
@Slf4j
@Service
public class TotpService {

    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final int DIGIT_MODULO = 1_000_000;
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Tạo khóa bí mật ngẫu nhiên Base32 độ dài 32 ký tự (160 bits entropy an toàn cao)
     */
    public String generateSecretKey() {
        byte[] bytes = new byte[20];
        secureRandom.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    /**
     * Sinh URI chuẩn `otpauth://` để hiển thị QR Code cho Google Authenticator, Authy, Microsoft Authenticator.
     *
     * @param secret Khóa bí mật Base32
     * @param accountName Tên tài khoản hoặc email của Admin
     * @param issuer Tên ứng dụng phát hành (mặc định "PopWorld")
     * @return Chuỗi URI định dạng otpauth
     */
    public String generateTotpUri(String secret, String accountName, String issuer) {
        String cleanIssuer = (issuer == null || issuer.isBlank()) ? "PopWorld" : issuer.trim();
        String cleanAccount = (accountName == null || accountName.isBlank()) ? "admin" : accountName.trim();
        
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                URLEncoder.encode(cleanIssuer, StandardCharsets.UTF_8),
                URLEncoder.encode(cleanAccount, StandardCharsets.UTF_8),
                secret.trim(),
                URLEncoder.encode(cleanIssuer, StandardCharsets.UTF_8),
                CODE_DIGITS,
                TIME_STEP_SECONDS
        );
    }

    /**
     * Sinh mã OTP 6 chữ số tại thời điểm hiện tại dựa trên secret key.
     */
    public String generateCurrentCode(String secretKey) {
        long currentStep = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;
        return generateCodeForStep(secretKey, currentStep);
    }

    /**
     * Xác thực mã OTP 6 chữ số do người dùng nhập.
     * Kiểm tra 3 bước thời gian: [T - 1, T, T + 1] (chấp nhận chênh lệch tối đa +- 30 giây).
     *
     * @param secretKey Khóa bí mật Base32 của người dùng
     * @param inputCode Mã 6 chữ số nhập từ ứng dụng xác thực
     * @return true nếu mã chính xác và còn trong hạn hiệu lực; false nếu sai hoặc hết hạn
     */
    public boolean verifyCode(String secretKey, String inputCode) {
        if (secretKey == null || secretKey.isBlank() || inputCode == null || inputCode.isBlank()) {
            return false;
        }

        String trimmedCode = inputCode.trim();
        if (trimmedCode.length() != CODE_DIGITS || !trimmedCode.matches("\\d{6}")) {
            return false;
        }

        long currentStep = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;

        // Cho phép dung sai lệch thời gian: bước trước (-1), bước hiện tại (0), bước sau (+1)
        for (long step = currentStep - 1; step <= currentStep + 1; step++) {
            String expectedCode = generateCodeForStep(secretKey, step);
            if (expectedCode != null && MessageDigest.isEqual(
                    expectedCode.getBytes(StandardCharsets.UTF_8),
                    trimmedCode.getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Thuật toán tính toán mã TOTP tại một bước thời gian cụ thể (RFC 6238 / RFC 4226)
     */
    public String generateCodeForStep(String secretKey, long timeStep) {
        try {
            byte[] keyBytes = decodeBase32(secretKey);
            if (keyBytes == null || keyBytes.length == 0) {
                return null;
            }

            // Chuyển bước thời gian thành mảng 8 bytes (Big-Endian)
            byte[] data = new byte[8];
            long value = timeStep;
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (value & 0xFF);
                value >>= 8;
            }

            // Tính toán HMAC-SHA1
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(keyBytes, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data);

            // Dynamic Truncation trích xuất số nguyên 31-bit (RFC 4226 Section 5.4)
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % DIGIT_MODULO;
            return String.format("%06d", otp);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Lỗi khi tạo mã TOTP: {}", e.getMessage(), e);
            return null;
        }
    }

    // =========================================================================
    // Base32 Encoding & Decoding Utilities (RFC 4648)
    // =========================================================================

    public String encodeBase32(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;

        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                result.append(BASE32_ALPHABET.charAt(index));
                bitsLeft -= 5;
            }
        }

        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            result.append(BASE32_ALPHABET.charAt(index));
        }

        return result.toString();
    }

    public byte[] decodeBase32(String base32) {
        if (base32 == null || base32.isBlank()) {
            return new byte[0];
        }

        String clean = base32.trim().toUpperCase().replace("=", "").replaceAll("\\s+", "");
        int outputLength = clean.length() * 5 / 8;
        byte[] result = new byte[outputLength];

        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;

        for (char c : clean.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) {
                log.warn("Ký tự Base32 không hợp lệ: '{}'", c);
                return null;
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                if (index < result.length) {
                    result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                }
                bitsLeft -= 8;
            }
        }

        return (index == result.length) ? result : Arrays.copyOf(result, index);
    }
}
