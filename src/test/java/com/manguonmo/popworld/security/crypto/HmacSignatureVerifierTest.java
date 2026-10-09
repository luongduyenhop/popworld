package com.manguonmo.popworld.security.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HmacSignatureVerifierTest {

    private HmacSignatureVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new HmacSignatureVerifier();
    }

    @Test
    @DisplayName("HMAC-SHA256: Tính toán chuỗi băm chính xác cho chuỗi mẫu")
    void testCalculateHmacSha256() {
        String data = "{\"orderCode\":\"PW-100\",\"amount\":250000}";
        String secret = "test-secret-key-123";

        String signature = verifier.calculateHmacSha256(data, secret);
        assertNotNull(signature);
        assertEquals(64, signature.length(), "HMAC-SHA256 hex string phải có độ dài 64 ký tự");

        // Kiểm tra tính nhất quán (deterministic)
        String signature2 = verifier.calculateHmacSha256(data, secret);
        assertEquals(signature, signature2);
    }

    @Test
    @DisplayName("HMAC-SHA256: Xác thực thành công khi payload và signature khớp nhau")
    void testVerifySignature_Success() {
        String data = "{\"orderCode\":\"PW-100\",\"amount\":250000}";
        String secret = "test-secret-key-123";
        String signature = verifier.calculateHmacSha256(data, secret);

        assertTrue(verifier.verifySignature(data, signature, secret));
        // Kiểm tra không phân biệt chữ hoa/thường của chuỗi hex
        assertTrue(verifier.verifySignature(data, signature.toUpperCase(), secret));
    }

    @Test
    @DisplayName("HMAC-SHA256: Từ chối khi payload bị sửa đổi dù chỉ 1 ký tự (Tính toàn vẹn Integrity)")
    void testVerifySignature_TamperedPayload_Fails() {
        String data = "{\"orderCode\":\"PW-100\",\"amount\":250000}";
        String secret = "test-secret-key-123";
        String signature = verifier.calculateHmacSha256(data, secret);

        // Kẻ tấn công sửa số tiền từ 250000 thành 25000
        String tamperedData = "{\"orderCode\":\"PW-100\",\"amount\":25000}";
        assertFalse(verifier.verifySignature(tamperedData, signature, secret));
    }

    @Test
    @DisplayName("HMAC-SHA256: Từ chối khi secret key không khớp")
    void testVerifySignature_WrongSecret_Fails() {
        String data = "{\"orderCode\":\"PW-100\",\"amount\":250000}";
        String validSecret = "test-secret-key-123";
        String wrongSecret = "wrong-secret-key-456";
        String signature = verifier.calculateHmacSha256(data, validSecret);

        assertFalse(verifier.verifySignature(data, signature, wrongSecret));
    }

    @Test
    @DisplayName("HMAC-SHA256: Xử lý an toàn với dữ liệu null hoặc rỗng")
    void testVerifySignature_NullOrEmptyInputs() {
        assertFalse(verifier.verifySignature(null, "some-sig", "secret"));
        assertFalse(verifier.verifySignature("data", null, "secret"));
        assertFalse(verifier.verifySignature("data", "some-sig", null));
        assertFalse(verifier.verifySignature("data", "", "secret"));
        assertNull(verifier.calculateHmacSha256(null, "secret"));
        assertNull(verifier.calculateHmacSha256("data", null));
    }
}
