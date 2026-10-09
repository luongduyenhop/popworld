package com.manguonmo.popworld.init;

import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.repository.*;
import com.manguonmo.popworld.service.DatabaseSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final CouponRepository couponRepository;
    private final PasswordEncoder passwordEncoder;
    private final DatabaseSeedService databaseSeedService;

    @Value("${app.init-demo-data:false}")
    private boolean initDemoCatalogData;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("PopWorld: Bắt đầu nạp / đồng bộ tài khoản Demo và dữ liệu hệ sinh thái POP MART...");

        // 1. Luôn đảm bảo 3 tài khoản Demo chuẩn và hoạt động ổn định
        seedBaseUsersAndAddresses();

        // 2. Mã giảm giá khuyến mãi cơ bản
        seedCoupons();

        // 3. Đồng bộ Catalog, Nhân vật BlindBoxItem, Khay hộp BlindBoxSlot nếu bật flag
        if (initDemoCatalogData) {
            log.info("PopWorld: Đồng bộ danh mục sản phẩm và khay hộp POP NOW...");
            databaseSeedService.reseedOfficialPopMartData();
        }

        log.info("PopWorld: Khởi tạo và đồng bộ tài khoản Demo hoàn tất!");
    }

    private void seedBaseUsersAndAddresses() {
        // Tài khoản Admin Demo
        User admin = userRepository.findByEmail("admin@popworld.com").orElse(null);
        if (admin == null) {
            admin = User.builder()
                    .fullName("Quản Trị Viên PopWorld")
                    .email("admin@popworld.com")
                    .password(passwordEncoder.encode("admin123"))
                    .phone("0988888888")
                    .role("ROLE_ADMIN")
                    .membershipTier("VIP")
                    .rewardPoints(9999)
                    .enabled(true)
                    .totpEnabled(false)
                    .build();
            userRepository.save(admin);
        } else {
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEnabled(true);
            admin.setRole("ROLE_ADMIN");
            admin.setTotpEnabled(false);
            userRepository.save(admin);
        }

        // Tài khoản Khách hàng thường Demo
        User member = userRepository.findByEmail("user@popworld.com").orElse(null);
        if (member == null) {
            member = User.builder()
                    .fullName("Nguyễn Minh Anh")
                    .email("user@popworld.com")
                    .password(passwordEncoder.encode("admin123"))
                    .phone("0912345678")
                    .role("ROLE_USER")
                    .membershipTier("MEMBER")
                    .rewardPoints(250)
                    .enabled(true)
                    .build();
            userRepository.save(member);

            userAddressRepository.save(UserAddress.builder()
                    .user(member)
                    .recipientName("Nguyễn Minh Anh")
                    .recipientPhone("0912345678")
                    .provinceCity("Hà Nội")
                    .district("Quận Hoàn Kiếm")
                    .ward("Phường Hàng Trống")
                    .detailedAddress("Số 18 Phố Tràng Thi")
                    .isDefault(true)
                    .build());

            userAddressRepository.save(UserAddress.builder()
                    .user(member)
                    .recipientName("Minh Anh (Cơ quan)")
                    .recipientPhone("0912345678")
                    .provinceCity("Hà Nội")
                    .district("Quận Cầu Giấy")
                    .ward("Phường Dịch Vọng")
                    .detailedAddress("Tòa nhà FPT Tower, Số 10 Phạm Văn Bạch")
                    .isDefault(false)
                    .build());
        } else {
            member.setPassword(passwordEncoder.encode("admin123"));
            member.setEnabled(true);
            userRepository.save(member);
        }

        // Tài khoản Khách hàng VIP Demo
        User collector = userRepository.findByEmail("collector@popworld.com").orElse(null);
        if (collector == null) {
            collector = User.builder()
                    .fullName("Trần Thảo Linh (VIP Collector)")
                    .email("collector@popworld.com")
                    .password(passwordEncoder.encode("admin123"))
                    .phone("0987654321")
                    .role("ROLE_USER")
                    .membershipTier("VIP")
                    .rewardPoints(1500)
                    .enabled(true)
                    .build();
            userRepository.save(collector);

            userAddressRepository.save(UserAddress.builder()
                    .user(collector)
                    .recipientName("Trần Thảo Linh")
                    .recipientPhone("0987654321")
                    .provinceCity("Hà Nội")
                    .district("Quận Hoàn Kiếm")
                    .ward("Phường Phan Chu Trinh")
                    .detailedAddress("Số 25 Phố Lý Thường Kiệt")
                    .isDefault(true)
                    .build());
        } else {
            collector.setPassword(passwordEncoder.encode("admin123"));
            collector.setEnabled(true);
            collector.setMembershipTier("VIP");
            userRepository.save(collector);
        }

        if (userRepository.findByEmail("thaolinh@gmail.com").isEmpty()) {
            User member2 = User.builder()
                    .fullName("Trần Thảo Linh")
                    .email("thaolinh@gmail.com")
                    .password(passwordEncoder.encode("admin123"))
                    .phone("0987654321")
                    .role("ROLE_USER")
                    .membershipTier("VIP")
                    .rewardPoints(1200)
                    .enabled(true)
                    .build();
            userRepository.save(member2);
        }
    }

    private void seedCoupons() {
        if (couponRepository.count() == 0) {
            couponRepository.save(Coupon.builder()
                    .code("POPWELCOME")
                    .description("Chào mừng thành viên mới! Giảm ngay 10% cho đơn hàng đầu tiên từ 200K.")
                    .discountType("PERCENTAGE")
                    .discountValue(new BigDecimal("10.00"))
                    .minOrderAmount(new BigDecimal("200000"))
                    .maxDiscountAmount(new BigDecimal("50000"))
                    .startDate(LocalDate.now().minusDays(10))
                    .endDate(LocalDate.now().plusMonths(6))
                    .usageLimit(1000)
                    .usedCount(12)
                    .active(true)
                    .build());

            couponRepository.save(Coupon.builder()
                    .code("FREESHIP")
                    .description("Miễn phí vận chuyển toàn quốc (giảm 30K phí ship) cho đơn từ 200K.")
                    .discountType("SHIPPING")
                    .discountValue(new BigDecimal("30000"))
                    .minOrderAmount(new BigDecimal("200000"))
                    .maxDiscountAmount(new BigDecimal("30000"))
                    .startDate(LocalDate.now().minusDays(5))
                    .endDate(LocalDate.now().plusMonths(3))
                    .usageLimit(500)
                    .usedCount(48)
                    .active(true)
                    .build());

            couponRepository.save(Coupon.builder()
                    .code("MEGAPOP500")
                    .description("Đặc quyền dân chơi Mega! Giảm trực tiếp 500K cho mọi mô hình Mega Collection.")
                    .discountType("FIXED_AMOUNT")
                    .discountValue(new BigDecimal("500000"))
                    .minOrderAmount(new BigDecimal("4000000"))
                    .maxDiscountAmount(new BigDecimal("500000"))
                    .startDate(LocalDate.now().minusDays(2))
                    .endDate(LocalDate.now().plusMonths(1))
                    .usageLimit(50)
                    .usedCount(3)
                    .active(true)
                    .build());
        }
    }
}