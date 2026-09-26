package com.manguonmo.popworld.init;

import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.init-demo-data", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final CategoryRepository categoryRepository;
    private final ArtistRepository artistRepository;
    private final CharacterIpRepository characterIpRepository;
    private final SeriesRepository seriesRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CouponRepository couponRepository;
    private final NewsRepository newsRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (productRepository.count() > 0) {
            log.info("PopWorld: Dữ liệu đã tồn tại trong CSDL. Bỏ qua bước nạp dữ liệu mẫu.");
            return;
        }

        log.info("PopWorld: Bắt đầu nạp bộ dữ liệu mẫu khổng lồ chuẩn POP MART...");

        // ==========================================
        // 1. TÀI KHOẢN NGƯỜI DÙNG & ĐỊA CHỈ
        // ==========================================
        User admin = User.builder()
                .fullName("Quản Trị Viên PopWorld")
                .email("admin@popworld.com")
                .password(passwordEncoder.encode("admin123"))
                .phone("0988888888")
                .role("ROLE_ADMIN")
                .membershipTier("VIP")
                .rewardPoints(9999)
                .enabled(true)
                .build();
        userRepository.save(admin);

        User member = User.builder()
                .fullName("Nguyễn Minh Anh")
                .email("user@popworld.com")
                .password(passwordEncoder.encode("user123"))
                .phone("0912345678")
                .role("ROLE_USER")
                .membershipTier("MEMBER")
                .rewardPoints(250)
                .enabled(true)
                .build();
        userRepository.save(member);

        User member2 = User.builder()
                .fullName("Trần Thảo Linh")
                .email("thaolinh@gmail.com")
                .password(passwordEncoder.encode("user123"))
                .phone("0987654321")
                .role("ROLE_USER")
                .membershipTier("VIP")
                .rewardPoints(1200)
                .enabled(true)
                .build();
        userRepository.save(member2);

        // Địa chỉ mẫu cho member
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

        // ==========================================
        // 2. DANH MỤC PHÂN LOẠI (CATEGORIES)
        // ==========================================
        Category catBlindBox = categoryRepository.save(Category.builder()
                .name("Hộp Mù Blind Box")
                .slug("blind-box")
                .description("Hộp mù bốc ngẫu nhiên trải nghiệm cảm giác bất ngờ săn nhân vật hiếm Secret.")
                .build());

        Category catMega = categoryRepository.save(Category.builder()
                .name("Mô Hình Mega Collection")
                .slug("mega-collection")
                .description("Mô hình khổ lớn cao cấp 400% và 1000% dành cho các nhà sưu tập nghệ thuật chuyên nghiệp.")
                .build());

        Category catPlush = categoryRepository.save(Category.builder()
                .name("Búp Bê & Gấu Bông Plush")
                .slug("plush-doll")
                .description("Dòng sản phẩm búp bê lông cừu, vinyl plush và móc khóa bông đeo túi hot-trend toàn cầu.")
                .build());

        Category catAcc = categoryRepository.save(Category.builder()
                .name("Phụ Kiện & Trưng Bày")
                .slug("accessories")
                .description("Túi bảo vệ chống bụi, hộp mica đèn LED trưng bày Art Toy chuyên nghiệp.")
                .build());

        // ==========================================
        // 3. NGHỆ SĨ THIẾT KẾ (ARTISTS)
        // ==========================================
        Artist kasing = artistRepository.save(Artist.builder()
                .name("Kasing Lung")
                .avatar("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300")
                .biography("Nghệ sĩ gốc Hong Kong lớn lên tại Hà Lan, tác giả huyền thoại của vương quốc The Monsters và chú quái vật tai dài Labubu gây bão toàn cầu.")
                .build());

        Artist kenny = artistRepository.save(Artist.builder()
                .name("Kenny Wong")
                .avatar("https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300")
                .biography("Họa sĩ, nhà thiết kế Hong Kong, 'cha đẻ' của cô bé mắt xanh môi hờn dỗi Molly từ năm 2006, biểu tượng Art Toy châu Á.")
                .build());

        Artist lang = artistRepository.save(Artist.builder()
                .name("Lang")
                .avatar("https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300")
                .biography("Nghệ sĩ điêu khắc trẻ người Trung Quốc, sáng tạo nên cậu bé Hirono mang đầy chiều sâu triết lý, nỗi cô đơn và sự chữa lành.")
                .build());

        Artist ayan = artistRepository.save(Artist.builder()
                .name("Ayan Deng")
                .avatar("https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=300")
                .biography("Nữ họa sĩ tài năng tốt nghiệp Học viện Mỹ thuật Trung ương Bắc Kinh, người thổi hồn vào cậu bé bồng bềnh Dimoo và những giấc mơ mây.")
                .build());

        Artist xiongmao = artistRepository.save(Artist.builder()
                .name("Xiongmao")
                .avatar("https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300")
                .biography("Nghệ sĩ sáng tạo nên Skullpanda - sự kết hợp táo bạo giữa phong cách Gothic bí ẩn, Cyberpunk tương lai và thời trang Avant-Garde.")
                .build());

        Artist mollyPainter = artistRepository.save(Artist.builder()
                .name("Molly Yllom")
                .avatar("https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=300")
                .biography("Tác giả của dòng nhân vật Crybaby nổi tiếng với đôi mắt đẫm lệ, tôn vinh cảm xúc tự nhiên và sự đồng cảm của con người.")
                .build());

        // ==========================================
        // 4. NHÂN VẬT IP (CHARACTER IPS)
        // ==========================================
        CharacterIp labubu = characterIpRepository.save(CharacterIp.builder()
                .name("Labubu")
                .artist(kasing)
                .avatarUrl("https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/5473/29602/monsterscamping_bb__83659.1649553079.jpg?c=2")
                .bannerUrl("https://prod-america-res.popmart.com/default/20240408_161327_778318__1200x1200.jpg")
                .description("Chú tiểu quái vật rừng sâu có đôi tai dài, nụ cười tinh quái để lộ 9 chiếc răng nhọn nhưng bên trong vô cùng ấm áp.")
                .build());

        CharacterIp molly = characterIpRepository.save(CharacterIp.builder()
                .name("Molly")
                .artist(kenny)
                .avatarUrl("https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8855/68307/IMG_9225__98082.1691765433.JPG?c=2")
                .bannerUrl("https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8855/68307/IMG_9225__98082.1691765433.JPG?c=2")
                .description("Cô bé họa sĩ có đôi mắt to tròn màu xanh biển hồ, bờ môi chu ra kiêu kỳ và luôn mang theo chiếc cọ vẽ khám phá vũ trụ.")
                .build());

        CharacterIp skullpanda = characterIpRepository.save(CharacterIp.builder()
                .name("Skullpanda")
                .artist(xiongmao)
                .avatarUrl("https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2")
                .bannerUrl("https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2")
                .description("Thực thể xuyên không gian mang chiếc tai nghe độc đáo, tự do du hành qua các thực tại và phản ánh tâm lý sâu kín của con người.")
                .build());

        CharacterIp hirono = characterIpRepository.save(CharacterIp.builder()
                .name("Hirono")
                .artist(lang)
                .avatarUrl("https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/7169/51202/hirono_mischief_tmb__85325.1661682805.jpg?c=2")
                .bannerUrl("https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/7169/51202/hirono_mischief_tmb__85325.1661682805.jpg?c=2")
                .description("Cậu bé mang dáng vẻ trầm ngâm trong chiếc áo choàng và chiếc mũ giấy, cất giữ những vụn vỡ tuổi thơ và hành trình tự chữa lành.")
                .build());

        CharacterIp dimoo = characterIpRepository.save(CharacterIp.builder()
                .name("Dimoo")
                .artist(ayan)
                .avatarUrl("https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2")
                .bannerUrl("https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2")
                .description("Cậu bé nhút nhát với búi tóc đám mây kỳ diệu luôn biến đổi hình dạng theo từng giấc mơ bay bổng.")
                .build());

        CharacterIp crybaby = characterIpRepository.save(CharacterIp.builder()
                .name("Crybaby")
                .artist(mollyPainter)
                .avatarUrl("https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2")
                .bannerUrl("https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2")
                .description("Cô bé giọt lệ đáng yêu, thông điệp rằng khóc không phải là yếu đuối mà là sự dũng cảm giải phóng cảm xúc.")
                .build());

        // ==========================================
        // 5. BỘ SƯU TẬP THEO MÙA (SERIES)
        // ==========================================
        Series sFallInWild = seriesRepository.save(Series.builder()
                .name("The Monsters - Fall in Wild Series")
                .characterIp(labubu)
                .releaseDate(LocalDate.of(2024, 4, 15))
                .bannerUrl("https://images.unsplash.com/photo-1510312305653-8ed496efae75?w=1000")
                .description("Bộ sưu tập Labubu mang phong cách cắm trại dã ngoại ngoài trời cực kỳ được săn đón.")
                .build());

        Series sHaveASeat = seriesRepository.save(Series.builder()
                .name("The Monsters - Have a Seat Vinyl Plush Series")
                .characterIp(labubu)
                .releaseDate(LocalDate.of(2024, 7, 20))
                .bannerUrl("https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=1000")
                .description("Dòng hộp mù nhồi bông Labubu ngồi ghế macaron đầy màu sắc tạo nên cơn sốt phụ kiện đeo túi toàn thế giới.")
                .build());

        Series sCityOfNight = seriesRepository.save(Series.builder()
                .name("Skullpanda - City of Night Series")
                .characterIp(skullpanda)
                .releaseDate(LocalDate.of(2023, 10, 10))
                .bannerUrl("https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=1000")
                .description("Tuyệt tác Skullpanda thành phố bóng đêm rực rỡ ánh đèn neon Cyberpunk.")
                .build());

        Series sTheWarmth = seriesRepository.save(Series.builder()
                .name("Skullpanda - The Warmth Series")
                .characterIp(skullpanda)
                .releaseDate(LocalDate.of(2023, 12, 1))
                .bannerUrl("https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1000")
                .description("Tone màu ấm áp thanh nhã, tôn vinh những khoảnh khắc dịu dàng của tâm hồn.")
                .build());

        Series sLittleMischief = seriesRepository.save(Series.builder()
                .name("Hirono - Little Mischief Series")
                .characterIp(hirono)
                .releaseDate(LocalDate.of(2023, 6, 1))
                .bannerUrl("https://images.unsplash.com/photo-1513151233558-d860c5398176?w=1000")
                .description("Những trò nghịch ngợm nhỏ bé của Hirono - series đã đưa tên tuổi Hirono đến với hàng triệu bạn trẻ.")
                .build());

        Series sTheOtherOne = seriesRepository.save(Series.builder()
                .name("Hirono - The Other One Series")
                .characterIp(hirono)
                .releaseDate(LocalDate.of(2023, 9, 15))
                .bannerUrl("https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=1000")
                .description("Chuyến hành trình khám phá những góc khuất nội tâm và sự thấu cảm với chính bản thân.")
                .build());

        Series sSpaceMolly2 = seriesRepository.save(Series.builder()
                .name("Mega Space Molly 100% Series 2")
                .characterIp(molly)
                .releaseDate(LocalDate.of(2024, 1, 10))
                .bannerUrl("https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1000")
                .description("Phi hành gia nhí Molly du hành qua các thiên hà với vũ khí và bình oxy tùy biến.")
                .build());

        Series sDimooRetro = seriesRepository.save(Series.builder()
                .name("Dimoo - Retro Series")
                .characterIp(dimoo)
                .releaseDate(LocalDate.of(2023, 5, 20))
                .bannerUrl("https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1000")
                .description("Hành trình trở về tuổi thơ với những chiếc máy băng game, radio cassette và máy ảnh chụp phim.")
                .build());

        Series sCrybabyParade = seriesRepository.save(Series.builder()
                .name("Crybaby - Crying Parade Series")
                .characterIp(crybaby)
                .releaseDate(LocalDate.of(2024, 3, 8))
                .bannerUrl("https://images.unsplash.com/photo-1563245372-f21724e3856d?w=1000")
                .description("Đoàn diễu hành đầy màu sắc của những giọt nước mắt ngọt ngào.")
                .build());

        // ==========================================
        // 6. SẢN PHẨM HỘP MÙ & MÔ HÌNH (PRODUCTS)
        // ==========================================

        // --- CỤM LABUBU ---
        createProduct(
                "Hộp Mù Labubu The Monsters Fall in Wild Series",
                "labubu-the-monsters-fall-in-wild-series",
                "Bộ hộp mù Labubu chủ đề dã ngoại Fall in Wild gồm 6 nhân vật cơ bản và 1 nhân vật bí mật Secret cực hiếm. Mô hình phủ lớp nỉ mịn cao cấp.",
                new BigDecimal("350000"),
                new BigDecimal("2100000"),
                150,
                "1 Hộp lẻ / Nguyên Thùng 6 Hộp",
                "1/72",
                "PVC / ABS / Đổ nỉ Flock",
                "Chiều cao: 8.5cm - 10.5cm",
                catBlindBox,
                sFallInWild,
                true, true,
                "https://prod-america-res.popmart.com/default/20240408_161645_829152__1200x1200.jpg"
        );

        createProduct(
                "Búp Bê Móc Khóa Labubu Have a Seat Vinyl Plush Blind Box",
                "labubu-have-a-seat-vinyl-plush-blind-box",
                "Hiện tượng gây sốt toàn cầu! Labubu nhồi bông lông cừu mềm mịn với khuôn mặt vinyl có thể xoay đầu, chuyên dùng móc túi xách thời thượng.",
                new BigDecimal("420000"),
                new BigDecimal("2520000"),
                80,
                "1 Hộp lẻ / Nguyên Thùng 6 Hộp",
                "1/72",
                "Vải bông Plush cao cấp / Mặt Vinyl / Móc kim loại",
                "Chiều cao: 17cm (Có móc treo)",
                catPlush,
                sHaveASeat,
                true, true,
                "https://prod-america-res.popmart.com/default/20240408_161327_778318__1200x1200.jpg"
        );

        createProduct(
                "Mô Hình Mega Labubu Tec 1000% All About Us Limited",
                "mega-labubu-tec-1000-all-about-us",
                "Phiên bản mô hình khổng lồ cao gần 80cm cực kỳ giới hạn của Labubu Tec. Tác phẩm nghệ thuật đỉnh cao cho không gian trưng bày sang trọng.",
                new BigDecimal("28500000"),
                null,
                5,
                "Nguyên Hộp Thùng Xốp Chống Sốc Chính Hãng",
                "Chỉ 1 mẫu duy nhất (Độc bản)",
                "Nhựa ABS cao cấp / Sơn phủ bóng gương",
                "Chiều cao: 79.5cm",
                catMega,
                sFallInWild,
                true, false,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/5291/26567/monstersart_tmb__15808.1617230212.jpg?c=2"
        );

        // --- CỤM SKULLPANDA ---
        createProduct(
                "Hộp Mù Skullpanda City of Night Series",
                "skullpanda-city-of-night-series",
                "Bộ sưu tập nghệ thuật lấy cảm hứng từ thế giới tương lai Cyberpunk. Skullpanda hóa thân thành DJ, Vũ công ánh sáng, Cảnh sát ngầm với hiệu ứng đèn huỳnh quang.",
                new BigDecimal("320000"),
                new BigDecimal("3840000"),
                120,
                "1 Hộp lẻ / Nguyên Thùng 12 Hộp",
                "1/144",
                "PVC / ABS / Đèn huỳnh quang UV",
                "Chiều cao: 7.5cm - 9.0cm",
                catBlindBox,
                sCityOfNight,
                true, false,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2"
        );

        createProduct(
                "Hộp Mù Skullpanda The Warmth Series",
                "skullpanda-the-warmth-series",
                "Những khoảnh khắc dịu êm của cuộc sống với các nhân vật như The Scent, The Day Off, The Drowsiness. Nước sơn lì nhám mềm mại như gốm sứ.",
                new BigDecimal("300000"),
                new BigDecimal("3600000"),
                100,
                "1 Hộp lẻ / Nguyên Thùng 12 Hộp",
                "1/144",
                "PVC / ABS / Sơn Matte mịn",
                "Chiều cao: 7.0cm - 8.5cm",
                catBlindBox,
                sTheWarmth,
                false, false,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/products/7170/images/51201/skullpanda_mare_tmb__44889.1662154606.500.750.jpg?c=2"
        );

        createProduct(
                "Mô Hình Mega Skullpanda 400% The Feast Collector Edition",
                "mega-skullpanda-400-the-feast",
                "Mô hình nghệ thuật cỡ lớn 400% Skullpanda The Feast với trang phục dạ tiệc đính kết chi tiết tinh xảo và mặt nạ kim loại tháo rời.",
                new BigDecimal("5200000"),
                null,
                15,
                "Hộp Vali Da Kèm Thẻ NFC Xác Thực",
                "Bản Giới Hạn Sưu Tập",
                "PVC / Hợp kim kẽm mạ vàng / Vải nhung",
                "Chiều cao: 32cm",
                catMega,
                sCityOfNight,
                true, true,
                "https://prod-eurasian-res.popmart.com/default/1_X8ltv4qiy2_1200x1200.jpg"
        );

        // --- CỤM HIRONO ---
        createProduct(
                "Hộp Mù Hirono Little Mischief Series",
                "hirono-little-mischief-series",
                "Series làm nên tên tuổi của cậu bé Hirono: The Aviator, The Monster, The Fox, The Ragpicker... Những mảnh ghép ký ức tuổi thơ xúc động.",
                new BigDecimal("280000"),
                new BigDecimal("3360000"),
                200,
                "1 Hộp lẻ / Nguyên Thùng 12 Hộp",
                "1/144",
                "PVC / ABS / Sơn loang rỉ sét nghệ thuật",
                "Chiều cao: 7.5cm - 9.0cm",
                catBlindBox,
                sLittleMischief,
                true, false,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/7169/51202/hirono_mischief_tmb__85325.1661682805.jpg?c=2"
        );

        createProduct(
                "Hộp Mù Hirono The Other One Series",
                "hirono-the-other-one-series",
                "Hành trình đi tìm bản ngã khác của chính mình với các mẫu kinh điển: Amnesia, The Ghost, Staring, Marionette.",
                new BigDecimal("290000"),
                new BigDecimal("3480000"),
                140,
                "1 Hộp lẻ / Nguyên Thùng 12 Hộp",
                "1/144",
                "PVC / ABS cao cấp",
                "Chiều cao: 7.8cm - 9.2cm",
                catBlindBox,
                sTheOtherOne,
                false, true,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6245/39276/hirono_all2__76002.1658930757.jpg?c=2"
        );

        // --- CỤM MOLLY ---
        createProduct(
                "Hộp Mù Mega Space Molly 100% Series 2",
                "mega-space-molly-100-series-2",
                "Phiên bản thu nhỏ của các mẫu Space Molly đình đám: Space Molly Bananaman, Space Molly Jean-Michel Basquiat, Space Molly Melting.",
                new BigDecimal("330000"),
                new BigDecimal("2970000"),
                90,
                "1 Hộp lẻ / Nguyên Thùng 9 Hộp",
                "1/108",
                "PVC / ABS / Khớp tay cử động / Mặt nạ mở được",
                "Chiều cao: 7.6cm",
                catBlindBox,
                sSpaceMolly2,
                true, true,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8855/68307/IMG_9225__98082.1691765433.JPG?c=2"
        );

        createProduct(
                "Mô Hình Mega Space Molly 400% Patrick Star Special Edition",
                "mega-space-molly-400-patrick-star",
                "Sự kết hợp bùng nổ giữa biểu tượng Space Molly và chú sao biển Patrick Star trong SpongeBob SquarePants.",
                new BigDecimal("4800000"),
                null,
                20,
                "Hộp Quà Cao Cấp Kèm Súng Vũ Trụ",
                "Bản Hợp Tác Bản Quyền",
                "PVC / ABS / Kính nón bảo hiểm trong suốt",
                "Chiều cao: 29.5cm",
                catMega,
                sSpaceMolly2,
                true, false,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8855/68307/IMG_9225__98082.1691765433.JPG?c=2"
        );

        // --- CỤM DIMOO & CRYBABY ---
        createProduct(
                "Hộp Mù Dimoo Retro Series",
                "dimoo-retro-series",
                "Cùng cậu bé Dimoo du hành thời gian với các mẫu: Dimoo Điện Tử Bấm Nút, Dimoo Băng Cassette, Dimoo Máy Ảnh Cơ, Dimoo Máy Nghe Nhạc Đĩa Than.",
                new BigDecimal("280000"),
                new BigDecimal("3360000"),
                110,
                "1 Hộp lẻ / Nguyên Thùng 12 Hộp",
                "1/144",
                "PVC / ABS / Màu loang cổ điển",
                "Chiều cao: 8.0cm - 9.5cm",
                catBlindBox,
                sDimooRetro,
                false, false,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2"
        );

        createProduct(
                "Hộp Mù Crybaby Crying Parade Series",
                "crybaby-crying-parade-series",
                "Bộ sưu tập Crybaby đáng yêu với thông điệp ôm lấy cảm xúc chân thật của bản thân. Mẫu Secret vịt vàng khóc nhè cực hiếm.",
                new BigDecimal("310000"),
                new BigDecimal("3720000"),
                130,
                "1 Hộp lẻ / Nguyên Thùng 12 Hộp",
                "1/144",
                "PVC / ABS / Chi tiết nước mắt nhựa trong",
                "Chiều cao: 7.2cm - 8.8cm",
                catBlindBox,
                sCrybabyParade,
                true, true,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2"
        );

        // --- CỤM PHỤ KIỆN TRƯNG BÀY (ACCESSORIES) ---
        createProduct(
                "Túi Đeo Bảo Vệ Mô Hình Labubu Trong Suốt PVC Crossbody",
                "tui-deo-bao-ve-mo-hinh-labubu-crossbody",
                "Túi đựng bảo vệ mô hình chống trầy xước, chống bám bụi bẩn, có khóa zip kín nước và quai đeo chéo thời trang đi chơi.",
                new BigDecimal("120000"),
                null,
                300,
                "1 Chiếc / Đóng Gói Túi Zip Riêng",
                "Không áp dụng",
                "Nhựa PVC dẻo trong suốt cao cấp / Dây đeo dù bền",
                "Kích thước: 18cm x 12cm x 8cm",
                catAcc,
                null,
                false, false,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/5652/34060/monsters_toy_bb__61293.1631287203.jpg?c=2"
        );

        createProduct(
                "Hộp Trưng Bày Mô Hình Đèn LED Pop World Mica Acrylic 3 Tầng",
                "hop-trung-bay-mo-hinh-den-led-acrylic-3-tang",
                "Hộp trưng bày mica trong suốt 99%, tích hợp dải đèn LED trần ấm áp cắm nguồn USB, sức chứa lên tới 18-24 hộp mù.",
                new BigDecimal("380000"),
                null,
                50,
                "Nguyên Kiện Tự Lắp Ghép Chống Vỡ Kèm Cáp USB",
                "Không áp dụng",
                "Mica Acrylic đúc nguyên khối / Đèn LED 3 chế độ sáng",
                "32cm x 18cm x 26cm",
                catAcc,
                null,
                true, false,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/9174/71701/IMG_2037_2__97318.1697657311.JPG?c=2"
        );

        // ==========================================
        // 7. MÃ GIẢM GIÁ KHUYẾN MÃI (COUPONS)
        // ==========================================
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
                .description("Miễn phí vận chuyển toàn quốc (giảm 30K phí ship) cho đơn từ 500K.")
                .discountType("FIXED_AMOUNT")
                .discountValue(new BigDecimal("30000"))
                .minOrderAmount(new BigDecimal("500000"))
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

        // ==========================================
        // 8. BÀI VIẾT CẨM NANG & TIN TỨC (NEWS)
        // ==========================================
        newsRepository.save(News.builder()
                .title("Cẩm Nang Chơi Blind Box Cho Người Mới: Tỷ Lệ Secret 1/144 Thực Sự Có Nghĩa Là Gì?")
                .slug("cam-nang-choi-blind-box-ty-le-secret-la-gi")
                .summary("Giải mã toàn bộ thuật ngữ của dân chơi Hộp mù: Single Box, Whole Set, Secret Ratio, Weight Test và kinh nghiệm bốc hộp không bị trùng mẫu.")
                .content("<p>Blind Box (Hộp mù) không đơn thuần là một món đồ chơi, đó là một trải nghiệm tâm lý kết hợp giữa tính nghệ thuật và sự hồi hộp kỳ diệu...</p>")
                .thumbnail("https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=800")
                .author("PopWorld Editorial")
                .published(true)
                .build());

        newsRepository.save(News.builder()
                .title("Hiện Tượng Labubu Gây Bão Toàn Cầu: Khi Đồ Chơi Nghệ Thuật Trở Thành Biểu Tượng Thời Trang")
                .slug("hien-tuong-labubu-gay-bao-toan-cau")
                .summary("Từ một bản vẽ phác thảo của nghệ sĩ Kasing Lung đến cơn sốt càn quét giới thời trang quốc tế và các ngôi sao giải trí hàng đầu.")
                .content("<p>Chưa bao giờ một chú quái vật tai dài với hàm răng nhọn hoắt lại khiến giới trẻ khắp châu Á lẫn phương Tây phải xếp hàng từ 4 giờ sáng...</p>")
                .thumbnail("https://images.unsplash.com/photo-1558679908-541bcf1249ff?w=800")
                .author("Hoàng Nam")
                .published(true)
                .build());

        log.info("PopWorld: NẠP THÀNH CÔNG toàn bộ hệ sinh thái dữ liệu mẫu chuẩn POP MART!");
    }

    private void createProduct(String name, String slug, String description,
                               BigDecimal singlePrice, BigDecimal wholeSetPrice,
                               int stock, String packaging, String ratio,
                               String material, String dimensions,
                               Category cat, Series series,
                               boolean isFeatured, boolean isNewRelease,
                               String mainImageUrl) {
        Product p = Product.builder()
                .name(name)
                .slug(slug)
                .description(description)
                .singlePrice(singlePrice)
                .wholeSetPrice(wholeSetPrice)
                .stockQuantity(stock)
                .packagingType(packaging)
                .secretRatio(ratio)
                .material(material)
                .sizeDimensions(dimensions)
                .category(cat)
                .series(series)
                .isFeatured(isFeatured)
                .isNewRelease(isNewRelease)
                .active(true)
                .build();
        productRepository.save(p);

        // Tạo ảnh đại diện chính cho sản phẩm
        productImageRepository.save(ProductImage.builder()
                .product(p)
                .imageUrl(mainImageUrl)
                .isThumbnail(true)
                .displayOrder(0)
                .build());
    }
}