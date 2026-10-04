package com.manguonmo.popworld.e2e.base;

import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.repository.*;
import com.manguonmo.popworld.security.ratelimit.RateLimiterService;
import com.manguonmo.popworld.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
public abstract class BaseE2ETest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected CategoryRepository categoryRepository;

    @Autowired
    protected SeriesRepository seriesRepository;

    @Autowired
    protected CartItemRepository cartItemRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected OrderItemRepository orderItemRepository;

    @Autowired
    protected OrderTimelineRepository orderTimelineRepository;

    @Autowired
    protected CouponRepository couponRepository;

    @Autowired
    protected UserCouponRepository userCouponRepository;

    @Autowired
    protected BlindBoxSlotRepository blindBoxSlotRepository;

    @Autowired
    protected BlindBoxItemRepository blindBoxItemRepository;

    @Autowired
    protected BoxReservationRepository boxReservationRepository;

    @Autowired
    protected OwnedItemRepository ownedItemRepository;

    @Autowired
    protected UserAddressRepository userAddressRepository;

    @Autowired
    protected OrderService orderService;

    @Autowired
    protected CartService cartService;

    @Autowired
    protected PopNowService popNowService;

    @Autowired
    protected CouponService couponService;

    @Autowired
    protected PaymentService paymentService;

    @Autowired
    protected RateLimiterService rateLimiterService;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    // Track test-created entity IDs for safe isolated teardown
    protected final List<Long> createdUserIds = new ArrayList<>();
    protected final List<Long> createdProductIds = new ArrayList<>();
    protected final List<Long> createdCouponIds = new ArrayList<>();
    protected final List<Long> createdCategoryIds = new ArrayList<>();

    @BeforeEach
    public void baseSetUp() {
        if (rateLimiterService != null) {
            rateLimiterService.clear();
        }
    }

    @AfterEach
    public void baseTearDown() {
        // Clean up any test-created reservations first
        for (Long userId : createdUserIds) {
            try {
                var userReservations = boxReservationRepository.findAll().stream()
                        .filter(r -> r.getUser() != null && r.getUser().getId().equals(userId)).toList();
                for (var r : userReservations) {
                    if (r.getSlot() != null) {
                        r.getSlot().setCurrentReservation(null);
                        blindBoxSlotRepository.save(r.getSlot());
                    }
                    boxReservationRepository.delete(r);
                }
                cartItemRepository.deleteAll(cartItemRepository.findByUserId(userId));
                List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
                for (Order order : orders) {
                    orderTimelineRepository.deleteAll(orderTimelineRepository.findByOrderIdOrderByCreatedAtAsc(order.getId()));
                    orderItemRepository.deleteAll(orderItemRepository.findByOrderId(order.getId()));
                    orderRepository.delete(order);
                }
                userAddressRepository.deleteAll(userAddressRepository.findByUserId(userId));
                userCouponRepository.deleteAll(userCouponRepository.findAll().stream()
                        .filter(uc -> uc.getUser() != null && uc.getUser().getId().equals(userId)).toList());
                ownedItemRepository.deleteAll(ownedItemRepository.findByUserIdOrderByUnboxedAtDesc(userId));
                userRepository.deleteById(userId);
            } catch (Exception ignored) {
            }
        }
        createdUserIds.clear();

        for (Long couponId : createdCouponIds) {
            try {
                userCouponRepository.deleteAll(userCouponRepository.findAll().stream()
                        .filter(uc -> uc.getCoupon() != null && uc.getCoupon().getId().equals(couponId)).toList());
                couponRepository.deleteById(couponId);
            } catch (Exception ignored) {
            }
        }
        createdCouponIds.clear();

        for (Long productId : createdProductIds) {
            try {
                var prodReservations = boxReservationRepository.findAll().stream()
                        .filter(r -> r.getProduct() != null && r.getProduct().getId().equals(productId)).toList();
                for (var r : prodReservations) {
                    if (r.getSlot() != null) {
                        r.getSlot().setCurrentReservation(null);
                        blindBoxSlotRepository.save(r.getSlot());
                    }
                    boxReservationRepository.delete(r);
                }
                var slots = blindBoxSlotRepository.findByProductIdOrderBySlotIndexAsc(productId);
                for (var s : slots) {
                    s.setCurrentReservation(null);
                }
                blindBoxSlotRepository.saveAll(slots);
                blindBoxSlotRepository.deleteAll(slots);
                blindBoxItemRepository.deleteAll(blindBoxItemRepository.findByProductId(productId));
                productRepository.deleteById(productId);
            } catch (Exception ignored) {
            }
        }
        createdProductIds.clear();

        for (Long categoryId : createdCategoryIds) {
            try {
                categoryRepository.deleteById(categoryId);
            } catch (Exception ignored) {
            }
        }
        createdCategoryIds.clear();

        if (rateLimiterService != null) {
            rateLimiterService.clear();
        }
    }

    protected User createTestUser(String email, String role) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            return existing.get();
        }
        User user = User.builder()
                .email(email)
                .fullName("Test " + email)
                .password(passwordEncoder.encode("TestPassword123!"))
                .role(role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .enabled(true)
                .phone("0987" + (100000 + new Random().nextInt(900000)))
                .rewardPoints(100)
                .luckyPoints(50)
                .hintCards(2)
                .membershipTier("MEMBER")
                .memberCode("MC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();
        User saved = userRepository.save(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    protected Category getOrCreateDefaultCategory() {
        List<Category> all = categoryRepository.findAll();
        if (!all.isEmpty()) {
            return all.get(0);
        }
        Category category = Category.builder()
                .name("E2E Test Category")
                .slug("e2e-test-category-" + UUID.randomUUID().toString().substring(0, 6))
                .description("Category for E2E tests")
                .build();
        Category saved = categoryRepository.save(category);
        createdCategoryIds.add(saved.getId());
        return saved;
    }

    protected Product createTestProduct(String name, int stock, BigDecimal singlePrice, BigDecimal wholeSetPrice) {
        Category category = getOrCreateDefaultCategory();
        String slug = (name.toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + UUID.randomUUID().toString().substring(0, 8))
                .replaceAll("^-+|-+$", "");

        Product product = Product.builder()
                .name(name)
                .slug(slug)
                .description("E2E Test Product Description")
                .category(category)
                .stockQuantity(stock)
                .singlePrice(singlePrice)
                .wholeSetPrice(wholeSetPrice != null ? wholeSetPrice : singlePrice.multiply(BigDecimal.valueOf(12)))
                .boxesPerSet(12)
                .packagingType("BLIND_BOX")
                .active(true)
                .isNewRelease(true)
                .isFeatured(false)
                .build();

        Product saved = productRepository.save(product);
        createdProductIds.add(saved.getId());
        return saved;
    }

    protected Coupon createTestCoupon(String code, String discountType, BigDecimal discountValue,
                                      BigDecimal minOrderAmount, BigDecimal maxDiscountAmount,
                                      LocalDate startDate, LocalDate endDate, Integer usageLimit) {
        Coupon coupon = Coupon.builder()
                .code(code.toUpperCase())
                .description("E2E Test Coupon " + code)
                .discountType(discountType)
                .discountValue(discountValue)
                .minOrderAmount(minOrderAmount != null ? minOrderAmount : BigDecimal.ZERO)
                .maxDiscountAmount(maxDiscountAmount)
                .startDate(startDate != null ? startDate : LocalDate.now().minusDays(1))
                .endDate(endDate != null ? endDate : LocalDate.now().plusMonths(1))
                .usageLimit(usageLimit != null ? usageLimit : 100)
                .usedCount(0)
                .active(true)
                .build();

        Coupon saved = couponRepository.save(coupon);
        createdCouponIds.add(saved.getId());
        return saved;
    }

    protected BlindBoxItem createTestBlindBoxItem(Product product, String name, RarityType rarity, int weight) {
        BlindBoxItem item = BlindBoxItem.builder()
                .product(product)
                .name(name)
                .rarity(rarity != null ? rarity : RarityType.REGULAR)
                .probabilityWeight(weight)
                .imageUrl("/images/products/test-item.png")
                .active(true)
                .build();
        return blindBoxItemRepository.save(item);
    }

    protected UserAddress createTestAddress(User user) {
        UserAddress address = UserAddress.builder()
                .user(user)
                .recipientName(user.getFullName() != null ? user.getFullName() : "Nguyen Van A")
                .recipientPhone("0987654321")
                .provinceCity("Ha Noi")
                .district("Cau Giay")
                .ward("Dich Vong")
                .detailedAddress("123 Xuan Thuy")
                .isDefault(true)
                .build();
        return userAddressRepository.save(address);
    }
}
