package com.manguonmo.popworld.security.totp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TotpServiceTest {

    private TotpService totpService;

    @BeforeEach
    void setUp() {
        totpService = new TotpService();
    }

    @Test
    @DisplayName("TOTP: Sinh Secret Key Base32 hợp lệ với độ dài 32 ký tự")
    void testGenerateSecretKey() {
        String secret = totpService.generateSecretKey();
        assertNotNull(secret);
        assertEquals(32, secret.length(), "Khóa bí mật Base32 20-bytes phải có độ dài 32 ký tự");
        assertTrue(secret.matches("[A-Z2-7]+"), "Khóa bí mật chỉ được chứa ký tự Base32");
    }

    @Test
    @DisplayName("TOTP: Mã hóa và giải mã Base32 bảo toàn dữ liệu gốc")
    void testBase32RoundTrip() {
        byte[] original = "HelloPopWorldSecurity2026".getBytes();
        String encoded = totpService.encodeBase32(original);
        byte[] decoded = totpService.decodeBase32(encoded);

        assertArrayEquals(original, decoded);
    }

    @Test
    @DisplayName("TOTP: Sinh và xác thực thành công mã OTP hiện tại")
    void testVerifyCode_Success() {
        String secret = totpService.generateSecretKey();
        String currentCode = totpService.generateCurrentCode(secret);

        assertNotNull(currentCode);
        assertEquals(6, currentCode.length(), "Mã OTP phải có 6 chữ số");
        assertTrue(currentCode.matches("\\d{6}"), "Mã OTP chỉ chứa số");

        assertTrue(totpService.verifyCode(secret, currentCode), "Mã OTP vừa sinh phải xác thực thành công");
    }

    @Test
    @DisplayName("TOTP: Xác thực thành công trong cửa sổ dung sai +- 30 giây (Clock Drift)")
    void testVerifyCode_WithClockDrift() {
        String secret = totpService.generateSecretKey();
        long currentStep = System.currentTimeMillis() / 1000 / 30;

        // Sinh mã ở bước trước (30 giây trước)
        String pastCode = totpService.generateCodeForStep(secret, currentStep - 1);
        assertTrue(totpService.verifyCode(secret, pastCode), "Phải chấp nhận mã của 30 giây trước");

        // Sinh mã ở bước kế tiếp (30 giây sau)
        String futureCode = totpService.generateCodeForStep(secret, currentStep + 1);
        assertTrue(totpService.verifyCode(secret, futureCode), "Phải chấp nhận mã của 30 giây tiếp theo");

        // Mã cách 2 bước (60 giây trước) phải bị từ chối
        String expiredCode = totpService.generateCodeForStep(secret, currentStep - 2);
        assertFalse(totpService.verifyCode(secret, expiredCode), "Mã quá hạn 60 giây phải bị từ chối");
    }

    @Test
    @DisplayName("TOTP: Từ chối mã sai định dạng, mã sai giá trị, hoặc secret không hợp lệ")
    void testVerifyCode_InvalidInputs() {
        String secret = totpService.generateSecretKey();

        assertFalse(totpService.verifyCode(secret, "12345"), "Mã ít hơn 6 số bị từ chối");
        assertFalse(totpService.verifyCode(secret, "1234567"), "Mã nhiều hơn 6 số bị từ chối");
        assertFalse(totpService.verifyCode(secret, "abcdef"), "Mã chứa chữ cái bị từ chối");
        assertFalse(totpService.verifyCode(secret, null), "Mã null bị từ chối");
        assertFalse(totpService.verifyCode(null, "123456"), "Secret null bị từ chối");
        assertFalse(totpService.verifyCode("INVALIDSECRET#@!", "123456"), "Secret không phải Base32 bị từ chối");
    }

    @Test
    @DisplayName("TOTP: Sinh URI chuẩn otpauth để quét mã QR Authenticator")
    void testGenerateTotpUri() {
        String secret = "JBSWY3DPEHPK3PXP";
        String uri = totpService.generateTotpUri(secret, "admin@popworld.com", "PopWorld");

        assertNotNull(uri);
        assertTrue(uri.startsWith("otpauth://totp/PopWorld:admin%40popworld.com?secret=JBSWY3DPEHPK3PXP"));
        assertTrue(uri.contains("digits=6"));
        assertTrue(uri.contains("period=30"));
    }
}
