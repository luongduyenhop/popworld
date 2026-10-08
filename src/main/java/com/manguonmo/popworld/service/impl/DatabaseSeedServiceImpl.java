package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.entity.*;
import com.manguonmo.popworld.repository.*;
import com.manguonmo.popworld.service.DatabaseSeedService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeedServiceImpl implements DatabaseSeedService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ArtistRepository artistRepository;
    private final CharacterIpRepository characterIpRepository;
    private final SeriesRepository seriesRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final BlindBoxItemRepository blindBoxItemRepository;
    private final BlindBoxSlotRepository blindBoxSlotRepository;
    private final BoxReservationRepository boxReservationRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void reseedOfficialPopMartData() {
        log.info("PopWorld: Bắt đầu đồng bộ và tái thiết lập dữ liệu chuẩn POP MART...");

        // 1. Đồng bộ người dùng đánh giá mẫu
        List<User> reviewers = syncReviewerUsers();

        // 2. Đồng bộ danh mục, nghệ sĩ, IP và bộ sưu tập
        Map<String, Category> categories = syncCategories();
        Map<String, Artist> artists = syncArtists();
        Map<String, CharacterIp> ips = syncCharacterIps(artists);
        Map<String, Series> seriesMap = syncSeries(ips);

        // 3. Đồng bộ danh sách sản phẩm với boxesPerSet chuẩn xác
        List<Product> products = syncProducts(categories, seriesMap);

        // 4. Dọn dẹp triệt để dữ liệu rác trên các sản phẩm không phải hộp mù (Mega & Phụ kiện)
        cleanupNonBlindBoxArtifacts(products);

        // 5. Dọn dẹp triệt để các sản phẩm thử nghiệm rác từ E2E tests (IDOR, Cancelled, Unpaid, ...)
        cleanupTestAndOrphanProducts();

        // 6. Đồng bộ danh sách nhân vật (BlindBoxItem) chuẩn cho từng Series
        syncBlindBoxItemsForAllSeries(products);

        // 7. Đồng bộ khay ô hộp (BlindBoxSlot) chính xác theo boxesPerSet (6, 9, 12)
        syncBlindBoxSlotsForAllSeries(products);

        // 8. Đồng bộ đánh giá khách hàng chân thực
        syncAuthenticReviews(products, reviewers);

        log.info("PopWorld: Hoàn tất đồng bộ toàn diện dữ liệu chuẩn POP MART thành công!");
    }

    private List<User> syncReviewerUsers() {
        List<User> users = new ArrayList<>();

        User admin = userRepository.findByEmail("admin@popworld.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .fullName("Quản Trị Viên PopWorld")
                        .email("admin@popworld.com")
                        .password(passwordEncoder.encode("admin123"))
                        .phone("0988888888")
                        .role("ROLE_ADMIN")
                        .membershipTier("VIP")
                        .rewardPoints(9999)
                        .enabled(true)
                        .build()));
        users.add(admin);

        User member1 = userRepository.findByEmail("user@popworld.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .fullName("Nguyễn Minh Anh")
                        .email("user@popworld.com")
                        .password(passwordEncoder.encode("user123"))
                        .phone("0912345678")
                        .role("ROLE_USER")
                        .membershipTier("MEMBER")
                        .rewardPoints(350)
                        .enabled(true)
                        .build()));
        users.add(member1);

        User member2 = userRepository.findByEmail("thaolinh@gmail.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .fullName("Trần Thảo Linh")
                        .email("thaolinh@gmail.com")
                        .password(passwordEncoder.encode("user123"))
                        .phone("0987654321")
                        .role("ROLE_USER")
                        .membershipTier("VIP")
                        .rewardPoints(1500)
                        .enabled(true)
                        .build()));
        users.add(member2);

        User member3 = userRepository.findByEmail("hoangphuc.toy@gmail.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .fullName("Lê Hoàng Phúc")
                        .email("hoangphuc.toy@gmail.com")
                        .password(passwordEncoder.encode("user123"))
                        .phone("0905123987")
                        .role("ROLE_USER")
                        .membershipTier("MEMBER")
                        .rewardPoints(420)
                        .enabled(true)
                        .build()));
        users.add(member3);

        User member4 = userRepository.findByEmail("quynhchi.collector@gmail.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .fullName("Phạm Quỳnh Chi")
                        .email("quynhchi.collector@gmail.com")
                        .password(passwordEncoder.encode("user123"))
                        .phone("0934567890")
                        .role("ROLE_USER")
                        .membershipTier("VIP")
                        .rewardPoints(2100)
                        .enabled(true)
                        .build()));
        users.add(member4);

        User member5 = userRepository.findByEmail("thangvu.blindbox@gmail.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .fullName("Vũ Đức Thắng")
                        .email("thangvu.blindbox@gmail.com")
                        .password(passwordEncoder.encode("user123"))
                        .phone("0978901234")
                        .role("ROLE_USER")
                        .membershipTier("MEMBER")
                        .rewardPoints(680)
                        .enabled(true)
                        .build()));
        users.add(member5);

        return users;
    }

    private Map<String, Category> syncCategories() {
        Map<String, Category> map = new HashMap<>();
        map.put("blind-box", getOrCreateCategory("Hộp Mù Blind Box", "blind-box",
                "Hộp mù bốc ngẫu nhiên trải nghiệm cảm giác bất ngờ săn nhân vật hiếm Secret."));
        map.put("mega-collection", getOrCreateCategory("Mô Hình Mega Collection", "mega-collection",
                "Mô hình khổ lớn cao cấp 400% và 1000% dành cho các nhà sưu tập nghệ thuật chuyên nghiệp."));
        map.put("plush-doll", getOrCreateCategory("Búp Bê & Gấu Bông Plush", "plush-doll",
                "Dòng sản phẩm búp bê lông cừu, vinyl plush và móc khóa bông đeo túi hot-trend toàn cầu."));
        map.put("accessories", getOrCreateCategory("Phụ Kiện & Trưng Bày", "accessories",
                "Túi bảo vệ chống bụi, hộp mica đèn LED trưng bày Art Toy chuyên nghiệp."));
        return map;
    }

    private Category getOrCreateCategory(String name, String slug, String desc) {
        return categoryRepository.findBySlug(slug).orElseGet(() ->
                categoryRepository.save(Category.builder().name(name).slug(slug).description(desc).build()));
    }

    private Map<String, Artist> syncArtists() {
        Map<String, Artist> map = new HashMap<>();
        map.put("kasing", getOrCreateArtist("Kasing Lung",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                "Nghệ sĩ gốc Hong Kong lớn lên tại Hà Lan, tác giả huyền thoại của vương quốc The Monsters và chú quái vật tai dài Labubu gây bão toàn cầu."));
        map.put("kenny", getOrCreateArtist("Kenny Wong",
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300",
                "Họa sĩ, nhà thiết kế Hong Kong, 'cha đẻ' của cô bé mắt xanh môi hờn dỗi Molly từ năm 2006, biểu tượng Art Toy châu Á."));
        map.put("lang", getOrCreateArtist("Lang",
                "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300",
                "Nghệ sĩ điêu khắc trẻ người Trung Quốc, sáng tạo nên cậu bé Hirono mang đầy chiều sâu triết lý, nỗi cô đơn và sự chữa lành."));
        map.put("ayan", getOrCreateArtist("Ayan Deng",
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=300",
                "Nữ họa sĩ tài năng tốt nghiệp Học viện Mỹ thuật Trung ương Bắc Kinh, người thổi hồn vào cậu bé bồng bềnh Dimoo và những giấc mơ mây."));
        map.put("xiongmao", getOrCreateArtist("Xiongmao",
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300",
                "Nghệ sĩ sáng tạo nên Skullpanda - sự kết hợp táo bạo giữa phong cách Gothic bí ẩn, Cyberpunk tương lai và thời trang Avant-Garde."));
        map.put("mollyPainter", getOrCreateArtist("Molly Yllom",
                "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=300",
                "Tác giả của dòng nhân vật Crybaby nổi tiếng với đôi mắt đẫm lệ, tôn vinh cảm xúc tự nhiên và sự đồng cảm của con người."));
        return map;
    }

    private Artist getOrCreateArtist(String name, String avatar, String bio) {
        return artistRepository.findByName(name).orElseGet(() ->
                artistRepository.save(Artist.builder().name(name).avatar(avatar).biography(bio).build()));
    }

    private Map<String, CharacterIp> syncCharacterIps(Map<String, Artist> artists) {
        Map<String, CharacterIp> map = new HashMap<>();
        map.put("labubu", getOrCreateIp("Labubu", artists.get("kasing"),
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/5473/29602/monsterscamping_bb__83659.1649553079.jpg?c=2",
                "https://prod-america-res.popmart.com/default/20240408_161327_778318__1200x1200.jpg",
                "Chú tiểu quái vật rừng sâu có đôi tai dài, nụ cười tinh quái để lộ 9 chiếc răng nhọn nhưng bên trong vô cùng ấm áp."));
        map.put("molly", getOrCreateIp("Molly", artists.get("kenny"),
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8855/68307/IMG_9225__98082.1691765433.JPG?c=2",
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8855/68307/IMG_9225__98082.1691765433.JPG?c=2",
                "Cô bé họa sĩ có đôi mắt to tròn màu xanh biển hồ, bờ môi chu ra kiêu kỳ và luôn mang theo chiếc cọ vẽ khám phá vũ trụ."));
        map.put("skullpanda", getOrCreateIp("Skullpanda", artists.get("xiongmao"),
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2",
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2",
                "Thực thể xuyên không gian mang chiếc tai nghe độc đáo, tự do du hành qua các thực tại và phản ánh tâm lý sâu kín của con người."));
        map.put("hirono", getOrCreateIp("Hirono", artists.get("lang"),
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/7169/51202/hirono_mischief_tmb__85325.1661682805.jpg?c=2",
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/7169/51202/hirono_mischief_tmb__85325.1661682805.jpg?c=2",
                "Cậu bé mang dáng vẻ trầm ngâm trong chiếc áo choàng và chiếc mũ giấy, cất giữ những vụn vỡ tuổi thơ và hành trình tự chữa lành."));
        map.put("dimoo", getOrCreateIp("Dimoo", artists.get("ayan"),
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2",
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2",
                "Cậu bé nhút nhát với búi tóc đám mây kỳ diệu luôn biến đổi hình dạng theo từng giấc mơ bay bổng."));
        map.put("crybaby", getOrCreateIp("Crybaby", artists.get("mollyPainter"),
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2",
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2",
                "Cô bé giọt lệ đáng yêu, thông điệp rằng khóc không phải là yếu đuối mà là sự dũng cảm giải phóng cảm xúc."));
        return map;
    }

    private CharacterIp getOrCreateIp(String name, Artist artist, String avatar, String banner, String desc) {
        return characterIpRepository.findByName(name).orElseGet(() ->
                characterIpRepository.save(CharacterIp.builder()
                        .name(name)
                        .artist(artist)
                        .avatarUrl(avatar)
                        .bannerUrl(banner)
                        .description(desc)
                        .build()));
    }

    private Map<String, Series> syncSeries(Map<String, CharacterIp> ips) {
        Map<String, Series> map = new HashMap<>();
        map.put("fallInWild", getOrCreateSeries("The Monsters - Fall in Wild Series", ips.get("labubu"),
                LocalDate.of(2024, 4, 15), "https://images.unsplash.com/photo-1510312305653-8ed496efae75?w=1000",
                "Bộ sưu tập Labubu mang phong cách cắm trại dã ngoại ngoài trời cực kỳ được săn đón."));
        map.put("haveASeat", getOrCreateSeries("The Monsters - Have a Seat Vinyl Plush Series", ips.get("labubu"),
                LocalDate.of(2024, 7, 20), "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=1000",
                "Dòng hộp mù nhồi bông Labubu ngồi ghế macaron đầy màu sắc tạo nên cơn sốt phụ kiện đeo túi toàn thế giới."));
        map.put("cityOfNight", getOrCreateSeries("Skullpanda - City of Night Series", ips.get("skullpanda"),
                LocalDate.of(2023, 10, 10), "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=1000",
                "Tuyệt tác Skullpanda thành phố bóng đêm rực rỡ ánh đèn neon Cyberpunk."));
        map.put("theWarmth", getOrCreateSeries("Skullpanda - The Warmth Series", ips.get("skullpanda"),
                LocalDate.of(2023, 12, 1), "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1000",
                "Tone màu ấm áp thanh nhã, tôn vinh những khoảnh khắc dịu dàng của tâm hồn."));
        map.put("littleMischief", getOrCreateSeries("Hirono - Little Mischief Series", ips.get("hirono"),
                LocalDate.of(2023, 6, 1), "https://images.unsplash.com/photo-1513151233558-d860c5398176?w=1000",
                "Những trò nghịch ngợm nhỏ bé của Hirono - series đã đưa tên tuổi Hirono đến với hàng triệu bạn trẻ."));
        map.put("theOtherOne", getOrCreateSeries("Hirono - The Other One Series", ips.get("hirono"),
                LocalDate.of(2023, 9, 15), "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=1000",
                "Chuyến hành trình khám phá những góc khuất nội tâm và sự thấu cảm với chính bản thân."));
        map.put("spaceMolly2", getOrCreateSeries("Mega Space Molly 100% Series 2", ips.get("molly"),
                LocalDate.of(2024, 1, 10), "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1000",
                "Phiên bản thu nhỏ của các mẫu Space Molly đình đám với khớp tay cử động và bình oxy rời."));
        map.put("dimooRetro", getOrCreateSeries("Dimoo - Retro Series", ips.get("dimoo"),
                LocalDate.of(2023, 5, 20), "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1000",
                "Hành trình trở về tuổi thơ với những chiếc máy băng game, radio cassette và máy ảnh chụp phim."));
        map.put("crybabyParade", getOrCreateSeries("Crybaby - Crying Parade Series", ips.get("crybaby"),
                LocalDate.of(2024, 3, 8), "https://images.unsplash.com/photo-1563245372-f21724e3856d?w=1000",
                "Đoàn diễu hành đầy màu sắc của những giọt nước mắt ngọt ngào."));
        map.put("crybabySadClub", getOrCreateSeries("Crybaby - Sad Club Series", ips.get("crybaby"),
                LocalDate.of(2024, 6, 1), "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1000",
                "Câu lạc bộ những nỗi buồn ngây thơ, thông điệp an ủi trái tim những người trẻ nhạy cảm."));
        map.put("dimooDating", getOrCreateSeries("Dimoo - Dating Series", ips.get("dimoo"),
                LocalDate.of(2024, 2, 14), "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?w=1000",
                "Những buổi hẹn hò ngọt ngào như kẹo bông của cậu bé Dimoo và những giấc mơ mây."));

        Series echoesSeries = seriesRepository.findByName("Hirono - Echoes of Silence Series")
                .or(() -> seriesRepository.findByName("Hirono Echoes of Silence Series"))
                .orElse(null);
        if (echoesSeries == null) {
            echoesSeries = seriesRepository.save(Series.builder()
                    .name("Hirono - Echoes of Silence Series")
                    .characterIp(ips.get("hirono"))
                    .releaseDate(LocalDate.of(2024, 5, 18))
                    .bannerUrl("https://prod-global-biz.popmart.com/globalAdmin/1785914967215_a365144942c74b86433a0af1c63446cb.jpg")
                    .description("Bộ sưu tập thứ 5 của Hirono mang tên Tiếng Vọng Của Sự Yên Lặng - một bản giao hưởng tĩnh lặng về sự kết nối giữa con người và thế giới xung quanh.")
                    .build());
        } else {
            echoesSeries.setCharacterIp(ips.get("hirono"));
            echoesSeries.setName("Hirono - Echoes of Silence Series");
            echoesSeries.setBannerUrl("https://prod-global-biz.popmart.com/globalAdmin/1785914967215_a365144942c74b86433a0af1c63446cb.jpg");
            seriesRepository.save(echoesSeries);
        }
        map.put("echoesOfSilence", echoesSeries);
        return map;
    }

    private Series getOrCreateSeries(String name, CharacterIp ip, LocalDate release, String banner, String desc) {
        return seriesRepository.findByName(name).orElseGet(() ->
                seriesRepository.save(Series.builder()
                        .name(name)
                        .characterIp(ip)
                        .releaseDate(release)
                        .bannerUrl(banner)
                        .description(desc)
                        .build()));
    }

    private List<Product> syncProducts(Map<String, Category> cat, Map<String, Series> s) {
        List<Product> list = new ArrayList<>();

        // 1. Labubu Fall in Wild (6 boxes)
        list.add(upsertProduct("Hộp Mù Labubu The Monsters Fall in Wild Series",
                "labubu-the-monsters-fall-in-wild-series",
                "Bộ hộp mù Labubu chủ đề dã ngoại Fall in Wild gồm 6 nhân vật cơ bản và 1 nhân vật bí mật Secret cực hiếm. Mô hình phủ lớp nỉ mịn cao cấp.",
                new BigDecimal("350000"), new BigDecimal("2100000"), 150,
                "1 Hộp lẻ / Nguyên Thùng 6 Hộp", 6, "1/72", "PVC / ABS / Đổ nỉ Flock", "Chiều cao: 8.5cm - 10.5cm",
                cat.get("blind-box"), s.get("fallInWild"), true, true,
                "https://prod-america-res.popmart.com/default/20240408_161645_829152__1200x1200.jpg"));

        // 2. Labubu Have a Seat Plush (6 boxes)
        list.add(upsertProduct("Búp Bê Móc Khóa Labubu Have a Seat Vinyl Plush Blind Box",
                "labubu-have-a-seat-vinyl-plush-blind-box",
                "Hiện tượng gây sốt toàn cầu! Labubu nhồi bông lông cừu mềm mịn với khuôn mặt vinyl có thể xoay đầu, chuyên dùng móc túi xách thời thượng.",
                new BigDecimal("420000"), new BigDecimal("2520000"), 80,
                "1 Hộp lẻ / Nguyên Thùng 6 Hộp", 6, "1/72", "Vải bông Plush cao cấp / Mặt Vinyl / Móc kim loại", "Chiều cao: 17cm (Có móc treo)",
                cat.get("plush-doll"), s.get("haveASeat"), true, true,
                "https://prod-america-res.popmart.com/default/20240408_161327_778318__1200x1200.jpg"));

        // 3. Mega Labubu 1000% (Not blind box)
        list.add(upsertProduct("Mô Hình Mega Labubu Tec 1000% All About Us Limited",
                "mega-labubu-tec-1000-all-about-us",
                "Phiên bản mô hình khổng lồ cao gần 80cm cực kỳ giới hạn của Labubu Tec. Tác phẩm nghệ thuật đỉnh cao cho không gian trưng bày sang trọng.",
                new BigDecimal("28500000"), null, 5,
                "Nguyên Hộp Thùng Xốp Chống Sốc Chính Hãng", null, "Chỉ 1 mẫu duy nhất (Độc bản)", "Nhựa ABS cao cấp / Sơn phủ bóng gương", "Chiều cao: 79.5cm",
                cat.get("mega-collection"), s.get("fallInWild"), true, false,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/5291/26567/monstersart_tmb__15808.1617230212.jpg?c=2"));

        // 4. Skullpanda City of Night (12 boxes)
        list.add(upsertProduct("Hộp Mù Skullpanda City of Night Series",
                "skullpanda-city-of-night-series",
                "Bộ sưu tập nghệ thuật lấy cảm hứng từ thế giới tương lai Cyberpunk. Skullpanda hóa thân thành DJ, Vũ công ánh sáng, Cảnh sát ngầm với hiệu ứng đèn huỳnh quang.",
                new BigDecimal("320000"), new BigDecimal("3840000"), 120,
                "1 Hộp lẻ / Nguyên Thùng 12 Hộp", 12, "1/144", "PVC / ABS / Đèn huỳnh quang UV", "Chiều cao: 7.5cm - 9.0cm",
                cat.get("blind-box"), s.get("cityOfNight"), true, false,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6595/43410/skullpanda_cityofnight_tmb__70108.1647110047.jpg?c=2"));

        // 5. Skullpanda The Warmth (12 boxes)
        list.add(upsertProduct("Hộp Mù Skullpanda The Warmth Series",
                "skullpanda-the-warmth-series",
                "Những khoảnh khắc dịu êm của cuộc sống với các nhân vật như The Scent, The Day Off, The Drowsiness. Nước sơn lì nhám mềm mại như gốm sứ.",
                new BigDecimal("300000"), new BigDecimal("3600000"), 100,
                "1 Hộp lẻ / Nguyên Thùng 12 Hộp", 12, "1/144", "PVC / ABS / Sơn Matte mịn", "Chiều cao: 7.0cm - 8.5cm",
                cat.get("blind-box"), s.get("theWarmth"), false, false,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/products/7170/images/51201/skullpanda_mare_tmb__44889.1662154606.500.750.jpg?c=2"));

        // 6. Mega Skullpanda 400% (Not blind box)
        list.add(upsertProduct("Mô Hình Mega Skullpanda 400% The Feast Collector Edition",
                "mega-skullpanda-400-the-feast",
                "Mô hình nghệ thuật cỡ lớn 400% Skullpanda The Feast với trang phục dạ tiệc đính kết chi tiết tinh xảo và mặt nạ kim loại tháo rời.",
                new BigDecimal("5200000"), null, 15,
                "Hộp Vali Da Kèm Thẻ NFC Xác Thực", null, "Bản Giới Hạn Sưu Tập", "PVC / Hợp kim kẽm mạ vàng / Vải nhung", "Chiều cao: 32cm",
                cat.get("mega-collection"), s.get("cityOfNight"), true, true,
                "https://prod-eurasian-res.popmart.com/default/1_X8ltv4qiy2_1200x1200.jpg"));

        // 7. Hirono Little Mischief (12 boxes)
        list.add(upsertProduct("Hộp Mù Hirono Little Mischief Series",
                "hirono-little-mischief-series",
                "Series làm nên tên tuổi của cậu bé Hirono: The Aviator, The Monster, The Fox, The Ragpicker... Những mảnh ghép ký ức tuổi thơ xúc động.",
                new BigDecimal("280000"), new BigDecimal("3360000"), 200,
                "1 Hộp lẻ / Nguyên Thùng 12 Hộp", 12, "1/144", "PVC / ABS / Sơn loang rỉ sét nghệ thuật", "Chiều cao: 7.5cm - 9.0cm",
                cat.get("blind-box"), s.get("littleMischief"), true, false,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/7169/51202/hirono_mischief_tmb__85325.1661682805.jpg?c=2"));

        // 8. Hirono The Other One (12 boxes)
        list.add(upsertProduct("Hộp Mù Hirono The Other One Series",
                "hirono-the-other-one-series",
                "Hành trình đi tìm bản ngã khác của chính mình với các mẫu kinh điển: Amnesia, The Ghost, Staring, Marionette.",
                new BigDecimal("290000"), new BigDecimal("3480000"), 140,
                "1 Hộp lẻ / Nguyên Thùng 12 Hộp", 12, "1/144", "PVC / ABS cao cấp", "Chiều cao: 7.8cm - 9.2cm",
                cat.get("blind-box"), s.get("theOtherOne"), false, true,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/6245/39276/hirono_all2__76002.1658930757.jpg?c=2"));

        // 9. Mega Space Molly 100% Series 2 (9 boxes)
        list.add(upsertProduct("Hộp Mù Mega Space Molly 100% Series 2",
                "mega-space-molly-100-series-2",
                "Phiên bản thu nhỏ của các mẫu Space Molly đình đám: Space Molly Bananaman, Space Molly Melting, Space Molly Basquiat. Khớp tay cử động và mũ mở được.",
                new BigDecimal("330000"), new BigDecimal("2970000"), 90,
                "1 Hộp lẻ / Nguyên Thùng 9 Hộp", 9, "1/108", "PVC / ABS / Khớp tay cử động / Mặt nạ mở được", "Chiều cao: 7.6cm",
                cat.get("blind-box"), s.get("spaceMolly2"), true, true,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8855/68307/IMG_9225__98082.1691765433.JPG?c=2"));

        // 10. Mega Space Molly 400% Patrick Star (Not blind box)
        list.add(upsertProduct("Mô Hình Mega Space Molly 400% Patrick Star Special Edition",
                "mega-space-molly-400-patrick-star",
                "Sự kết hợp bùng nổ giữa biểu tượng Space Molly và chú sao biển Patrick Star trong SpongeBob SquarePants.",
                new BigDecimal("4800000"), null, 20,
                "Hộp Quà Cao Cấp Kèm Súng Vũ Trụ", null, "Bản Hợp Tác Bản Quyền", "PVC / ABS / Kính nón bảo hiểm trong suốt", "Chiều cao: 29.5cm",
                cat.get("mega-collection"), s.get("spaceMolly2"), true, false,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8855/68307/IMG_9225__98082.1691765433.JPG?c=2"));

        // 11. Dimoo Retro Series (12 boxes)
        list.add(upsertProduct("Hộp Mù Dimoo Retro Series",
                "dimoo-retro-series",
                "Cùng cậu bé Dimoo du hành thời gian với các mẫu: Dimoo Điện Tử Bấm Nút, Dimoo Băng Cassette, Dimoo Máy Ảnh Cơ, Dimoo Máy Nghe Nhạc Đĩa Than.",
                new BigDecimal("280000"), new BigDecimal("3360000"), 110,
                "1 Hộp lẻ / Nguyên Thùng 12 Hộp", 12, "1/144", "PVC / ABS / Màu loang cổ điển", "Chiều cao: 8.0cm - 9.5cm",
                cat.get("blind-box"), s.get("dimooRetro"), false, false,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2"));

        // 12. Crybaby Crying Parade Series (12 boxes)
        list.add(upsertProduct("Hộp Mù Crybaby Crying Parade Series",
                "crybaby-crying-parade-series",
                "Bộ sưu tập Crybaby đáng yêu với thông điệp ôm lấy cảm xúc chân thật của bản thân. Mẫu Secret vịt vàng khóc nhè cực hiếm.",
                new BigDecimal("310000"), new BigDecimal("3720000"), 130,
                "1 Hộp lẻ / Nguyên Thùng 12 Hộp", 12, "1/144", "PVC / ABS / Chi tiết nước mắt nhựa trong", "Chiều cao: 7.2cm - 8.8cm",
                cat.get("blind-box"), s.get("crybabyParade"), true, true,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2"));

        // 13. Phụ kiện Túi Đeo Bảo Vệ Labubu (Not blind box)
        list.add(upsertProduct("Túi Đeo Bảo Vệ Mô Hình Labubu Trong Suốt PVC Crossbody",
                "tui-deo-bao-ve-mo-hinh-labubu-crossbody",
                "Túi đựng bảo vệ mô hình chống trầy xước, chống bám bụi bẩn, có khóa zip kín nước và quai đeo chéo thời trang đi chơi.",
                new BigDecimal("120000"), null, 300,
                "1 Chiếc / Đóng Gói Túi Zip Riêng", null, "Không áp dụng", "Nhựa PVC dẻo trong suốt cao cấp / Dây đeo dù bền", "Kích thước: 18cm x 12cm x 8cm",
                cat.get("accessories"), null, false, false,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/5652/34060/monsters_toy_bb__61293.1631287203.jpg?c=2"));

        // 14. Phụ kiện Hộp Trưng Bày Mica LED (Not blind box)
        list.add(upsertProduct("Hộp Trưng Bày Mô Hình Đèn LED Pop World Mica Acrylic 3 Tầng",
                "hop-trung-bay-mo-hinh-den-led-acrylic-3-tang",
                "Hộp trưng bày mica trong suốt 99%, tích hợp dải đèn LED trần ấm áp cắm nguồn USB, sức chứa lên tới 18-24 hộp mù.",
                new BigDecimal("380000"), null, 50,
                "Nguyên Kiện Tự Lắp Ghép Chống Vỡ Kèm Cáp USB", null, "Không áp dụng", "Mica Acrylic đúc nguyên khối / Đèn LED 3 chế độ sáng", "32cm x 18cm x 26cm",
                cat.get("accessories"), null, true, false,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/9174/71701/IMG_2037_2__97318.1697657311.JPG?c=2"));

        // 15. Crybaby Sad Club Series (6 boxes)
        list.add(upsertProduct("Hộp Mù Crybaby Sad Club Series",
                "crybaby-sad-club-series",
                "Bộ sưu tập Crybaby Sad Club phiên bản giới hạn 6 nhân vật cơ bản và 1 nhân vật bí mật Cầu Vồng Nước Mắt.",
                new BigDecimal("320000"), new BigDecimal("1920000"), 100,
                "1 Hộp lẻ / Nguyên Thùng 6 Hộp", 6, "1/72", "PVC / ABS / Bề mặt nhám mờ", "Chiều cao: 8.0cm - 9.2cm",
                cat.get("blind-box"), s.get("crybabySadClub"), true, true,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/11154/90496/IMG_2719__82920.1733668882.JPG?c=2"));

        // 16. Dimoo Dating Series (6 boxes)
        list.add(upsertProduct("Hộp Mù Dimoo Dating Series",
                "dimoo-dating-series",
                "Bộ sưu tập Dimoo Dating Series mang chủ đề những cuộc hẹn hò ngọt ngào: Xem phim, Cà phê, Dạo công viên, Đạp xe.",
                new BigDecimal("300000"), new BigDecimal("1800000"), 110,
                "1 Hộp lẻ / Nguyên Thùng 6 Hộp", 6, "1/72", "PVC / ABS", "Chiều cao: 7.8cm - 9.0cm",
                cat.get("blind-box"), s.get("dimooDating"), true, false,
                "https://cdn11.bigcommerce.com/s-fvv65gjhoq/images/stencil/1200x1200/products/8420/63431/IMG_4777__91468.1684134185.JPG?c=2"));

        // 17. Hirono Echoes of Silence (12 boxes)
        list.add(upsertProduct("Hộp Mù Hirono Echoes of Silence Series",
                "hirono-echoes-of-silence-series",
                "Bộ sưu tập nghệ thuật đương đại Hirono Echoes of Silence Series gồm 12 nhân vật tiêu chuẩn và 1 nhân vật bí mật Time quý hiếm. Nước sơn và chất liệu mộc mạc biểu cảm.",
                new BigDecimal("350000"), new BigDecimal("4200000"), 120,
                "1 Hộp lẻ / Nguyên Thùng 12 Hộp", 12, "1/144", "PVC / ABS cao cấp / Hiệu ứng sơn cổ điển", "Chiều cao: 7.5cm - 9.0cm",
                cat.get("blind-box"), s.get("echoesOfSilence"), true, true,
                "https://prod-global-biz.popmart.com/globalAdmin/1785914967215_a365144942c74b86433a0af1c63446cb.jpg"));

        return list;
    }

    private Product upsertProduct(String name, String slug, String description,
                                  BigDecimal singlePrice, BigDecimal wholeSetPrice,
                                  int stock, String packaging, Integer boxesPerSet, String ratio,
                                  String material, String dimensions,
                                  Category cat, Series series,
                                  boolean isFeatured, boolean isNewRelease,
                                  String mainImageUrl) {
        Product p = productRepository.findBySlug(slug).orElse(null);
        if (p == null && "hirono-echoes-of-silence-series".equals(slug)) {
            p = productRepository.findBySlug("hop-mu-hirono-echoes-of-silence-series").orElse(null);
        }
        if (p == null) {
            p = Product.builder()
                    .name(name)
                    .slug(slug)
                    .description(description)
                    .singlePrice(singlePrice)
                    .wholeSetPrice(wholeSetPrice)
                    .stockQuantity(stock)
                    .packagingType(packaging)
                    .boxesPerSet(boxesPerSet)
                    .secretRatio(ratio)
                    .material(material)
                    .sizeDimensions(dimensions)
                    .category(cat)
                    .series(series)
                    .isFeatured(isFeatured)
                    .isNewRelease(isNewRelease)
                    .active(true)
                    .build();
            p = productRepository.save(p);
            productImageRepository.save(ProductImage.builder()
                    .product(p)
                    .imageUrl(mainImageUrl)
                    .isThumbnail(true)
                    .displayOrder(0)
                    .build());
        } else {
            p.setName(name);
            p.setSlug(slug);
            p.setDescription(description);
            p.setSinglePrice(singlePrice);
            p.setWholeSetPrice(wholeSetPrice);
            p.setPackagingType(packaging);
            p.setBoxesPerSet(boxesPerSet);
            p.setSecretRatio(ratio);
            p.setMaterial(material);
            p.setSizeDimensions(dimensions);
            p.setCategory(cat);
            p.setSeries(series);
            p.setIsFeatured(isFeatured);
            p.setIsNewRelease(isNewRelease);
            p.setActive(true);
            p = productRepository.save(p);

            // Đảm bảo có ảnh thumbnail và cập nhật ảnh mới nhất
            List<ProductImage> existingImgs = productImageRepository.findByProductIdOrderByDisplayOrderAsc(p.getId());
            if (existingImgs.isEmpty()) {
                productImageRepository.save(ProductImage.builder()
                        .product(p)
                        .imageUrl(mainImageUrl)
                        .isThumbnail(true)
                        .displayOrder(0)
                        .build());
            } else if (mainImageUrl != null) {
                ProductImage thumb = existingImgs.get(0);
                thumb.setImageUrl(mainImageUrl);
                thumb.setIsThumbnail(true);
                productImageRepository.save(thumb);
            }
        }
        return p;
    }

    private void cleanupNonBlindBoxArtifacts(List<Product> products) {
        for (Product p : products) {
            boolean isBlindBoxCategory = p.getCategory() != null &&
                    ("blind-box".equals(p.getCategory().getSlug()) || "plush-doll".equals(p.getCategory().getSlug()));
            boolean isMega = p.getCategory() != null && "mega-collection".equals(p.getCategory().getSlug());

            if (!isBlindBoxCategory || isMega) {
                // 1. Gỡ liên kết các phiếu giữ hộp trỏ tới slot của sản phẩm này
                entityManager.createQuery("UPDATE BoxReservation r SET r.slot = null WHERE r.product.id = :pid")
                        .setParameter("pid", p.getId())
                        .executeUpdate();

                // 2. Xóa liên kết currentReservation trên slot
                entityManager.createQuery("UPDATE BlindBoxSlot s SET s.currentReservation = null WHERE s.product.id = :pid")
                        .setParameter("pid", p.getId())
                        .executeUpdate();

                // 3. Xóa các phiếu giữ hộp thử nghiệm chưa hoàn tất
                entityManager.createQuery("DELETE FROM BoxReservation r WHERE r.product.id = :pid AND r.status != :purchased AND r.status != :unboxed")
                        .setParameter("pid", p.getId())
                        .setParameter("purchased", ReservationStatus.PURCHASED)
                        .setParameter("unboxed", ReservationStatus.UNBOXED)
                        .executeUpdate();

                // 4. Xóa toàn bộ slot rác
                entityManager.createQuery("DELETE FROM BlindBoxSlot s WHERE s.product.id = :pid")
                        .setParameter("pid", p.getId())
                        .executeUpdate();

                // 5. Xóa toàn bộ item rác (trừ khi đã có người sở hữu trong tủ đồ)
                try {
                    entityManager.createQuery("DELETE FROM BlindBoxItem i WHERE i.product.id = :pid AND i.id NOT IN (SELECT o.blindBoxItem.id FROM OwnedItem o)")
                            .setParameter("pid", p.getId())
                            .executeUpdate();
                } catch (Exception e) {
                    log.warn("Không thể xóa blind_box_items cho sản phẩm ID {}: {}", p.getId(), e.getMessage());
                }
            }
        }
    }

    private void syncBlindBoxItemsForAllSeries(List<Product> products) {
        for (Product p : products) {
            String slug = p.getSlug();
            List<ItemDef> defs = getItemDefsForSlug(slug);
            if (defs == null || defs.isEmpty()) continue;

            List<BlindBoxItem> existing = blindBoxItemRepository.findByProductId(p.getId());
            Map<String, BlindBoxItem> byName = new HashMap<>();
            for (BlindBoxItem item : existing) {
                byName.put(item.getName().trim().toLowerCase(), item);
            }

            String fallbackImg = (p.getImages() != null && !p.getImages().isEmpty())
                    ? p.getImages().get(0).getImageUrl()
                    : "https://prod-america-res.popmart.com/default/20240408_161645_829152__1200x1200.jpg";

            for (ItemDef def : defs) {
                String key = def.name.trim().toLowerCase();
                BlindBoxItem item = byName.remove(key);
                if (item == null) {
                    item = BlindBoxItem.builder()
                            .product(p)
                            .name(def.name)
                            .rarity(def.rarity)
                            .imageUrl(def.imageUrl != null ? def.imageUrl : fallbackImg)
                            .probabilityWeight(def.rarity == RarityType.SECRET ? 10 : 100)
                            .active(true)
                            .build();
                } else {
                    item.setRarity(def.rarity);
                    if (def.imageUrl != null) item.setImageUrl(def.imageUrl);
                    item.setProbabilityWeight(def.rarity == RarityType.SECRET ? 10 : 100);
                    item.setActive(true);
                }
                blindBoxItemRepository.save(item);
            }

            // Xóa các item thừa không nằm trong danh sách chuẩn (nếu đã unbox thì ẩn đi)
            for (BlindBoxItem excess : byName.values()) {
                if (isItemReferencedInOwnedItems(excess.getId())) {
                    excess.setActive(false);
                    blindBoxItemRepository.save(excess);
                } else {
                    blindBoxItemRepository.delete(excess);
                }
            }
        }
    }

    private void syncBlindBoxSlotsForAllSeries(List<Product> products) {
        for (Product p : products) {
            if (p.getBoxesPerSet() == null || p.getBoxesPerSet() <= 0) continue;
            boolean isBlindBoxCategory = p.getCategory() != null &&
                    ("blind-box".equals(p.getCategory().getSlug()) || "plush-doll".equals(p.getCategory().getSlug()));
            if (!isBlindBoxCategory) continue;

            int targetCount = p.getBoxesPerSet();
            List<BlindBoxSlot> existingSlots = blindBoxSlotRepository.findByProductIdOrderBySlotIndexAsc(p.getId());
            Map<Integer, BlindBoxSlot> slotMap = new HashMap<>();
            for (BlindBoxSlot s : existingSlots) {
                slotMap.put(s.getSlotIndex(), s);
            }

            // 1. Đảm bảo các slot từ 1 đến targetCount tồn tại
            for (int i = 1; i <= targetCount; i++) {
                BlindBoxSlot s = slotMap.remove(i);
                if (s == null) {
                    s = BlindBoxSlot.builder()
                            .product(p)
                            .slotIndex(i)
                            .status(SlotStatus.AVAILABLE)
                            .build();
                    blindBoxSlotRepository.save(s);
                } else {
                    // Nếu slot đang bị kẹt ở trạng thái hết hạn / hủy mà chưa được giải phóng -> hồi sinh AVAILABLE
                    if (s.getStatus() == SlotStatus.HELD && s.getCurrentReservation() == null) {
                        s.setStatus(SlotStatus.AVAILABLE);
                        blindBoxSlotRepository.save(s);
                    }
                }
            }

            // 2. Xóa các slot vượt quá targetCount (nếu có và không bị giữ chỗ)
            for (BlindBoxSlot excess : slotMap.values()) {
                if (excess.getStatus() == SlotStatus.AVAILABLE || excess.getCurrentReservation() == null) {
                    entityManager.createQuery("UPDATE BoxReservation r SET r.slot = null WHERE r.slot.id = :slotId")
                            .setParameter("slotId", excess.getId())
                            .executeUpdate();
                    blindBoxSlotRepository.delete(excess);
                }
            }
        }
    }

    private void syncAuthenticReviews(List<Product> products, List<User> reviewers) {
        String[][] reviewTemplates = {
                {"5", "Mở hộp lần đầu tiên mà bốc trúng ngay em mình thích nhất! Chi tiết sơn siêu sắc nét, hộp đóng gói bọc xốp chống sốc 3 lớp cực kỳ cẩn thận."},
                {"5", "Hàng chuẩn authentic chính hãng POP MART, quét mã QR ra đúng seri. Đã ủng hộ shop 3 lần và lần nào cũng siêu ưng ý!"},
                {"5", "Tính năng lắc hộp POP NOW trên web vui xỉu, lắc trúng loại trừ chuẩn đét luôn. Giao hàng hỏa tốc trong 2 tiếng tại Hà Nội, đỉnh nóc kịch trần!"},
                {"4", "Mẫu Secret đẹp mê hồn nhưng bốc 2 hộp mới trúng mẫu thường. Chất lượng nhựa ABS hoàn thiện rất mịn tay, không có ba via."},
                {"5", "Full thùng 12 hộp không trùng mẫu nào, cảm giác đập hộp nguyên seal phê không thể tả. 10/10 điểm cho chất lượng và độ uy tín của PopWorld!"}
        };

        for (Product p : products) {
            if (p.getCategory() == null || "accessories".equals(p.getCategory().getSlug())) continue;

            List<Review> existingReviews = reviewRepository.findByProductId(p.getId());
            Set<Long> reviewedUserIds = new HashSet<>();
            for (Review r : existingReviews) {
                reviewedUserIds.add(r.getUser().getId());
            }

            int count = 0;
            for (User user : reviewers) {
                if (reviewedUserIds.contains(user.getId())) continue;
                String[] template = reviewTemplates[count % reviewTemplates.length];
                reviewRepository.save(Review.builder()
                        .product(p)
                        .user(user)
                        .rating(Integer.parseInt(template[0]))
                        .comment(template[1])
                        .approved(true)
                        .build());
                count++;
                if (count >= 3) break; // 3 đánh giá chân thực mỗi sản phẩm
            }
        }
    }

    private static class ItemDef {
        String name;
        RarityType rarity;
        String imageUrl;

        ItemDef(String name, RarityType rarity, String imageUrl) {
            this.name = name;
            this.rarity = rarity;
            this.imageUrl = imageUrl;
        }
    }

    private List<ItemDef> getItemDefsForSlug(String slug) {
        List<ItemDef> list = new ArrayList<>();
        switch (slug) {
            case "labubu-the-monsters-fall-in-wild-series":
                list.add(new ItemDef("Datura", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_The_Monsters_Labubu_Fall_in_Wild_Series_Badge_Datura_1800x1800.jpg"));
                list.add(new ItemDef("Bellflower", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_The_Monsters_Labubu_Fall_in_Wild_Series_Badge_Bellflower_1800x1800.jpg"));
                list.add(new ItemDef("Cactus", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_The_Monsters_Labubu_Fall_in_Wild_Series_Badge_Cactus_1800x1800.jpg"));
                list.add(new ItemDef("Monstera Deliciosa", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_The_Monsters_Labubu_Fall_in_Wild_Series_Badge_Monstera_Deliciosa_1800x1800.jpg"));
                list.add(new ItemDef("Platy Cerium", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_The_Monsters_Labubu_Fall_in_Wild_Series_Badge_Platy_Cerium_1800x1800.jpg"));
                list.add(new ItemDef("Crotalaria", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_The_Monsters_Labubu_Fall_in_Wild_Series_Badge_Crotalaria_1800x1800.jpg"));
                list.add(new ItemDef("Golden Forest King", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/Popmart_The_Monsters_Labubu_Fall_in_Wild_Series_Badge_Gardener_Secret_1800x1800.jpg"));
                break;

            case "labubu-have-a-seat-vinyl-plush-blind-box":
                list.add(new ItemDef("Dada", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/the_monsters_labubu_have_a_seat_vinyl_plush_blind_box_Dada_1800x1800.png"));
                list.add(new ItemDef("Ququ", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/the_monsters_labubu_have_a_seat_vinyl_plush_blind_box_Ququ_1800x1800.png"));
                list.add(new ItemDef("Sisi", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/the_monsters_labubu_have_a_seat_vinyl_plush_blind_box_Sisi_1800x1800.png"));
                list.add(new ItemDef("Hehe", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/the_monsters_labubu_have_a_seat_vinyl_plush_blind_box_Hehe_1800x1800.png"));
                list.add(new ItemDef("Zizi", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/the_monsters_labubu_have_a_seat_vinyl_plush_blind_box_Zizi_1800x1800.png"));
                list.add(new ItemDef("Baba", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/the_monsters_labubu_have_a_seat_vinyl_plush_blind_box_Baba_1800x1800.png"));
                list.add(new ItemDef("DuoDuo Chestnut", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/the_monsters_labubu_have_a_seat_vinyl_plush_blind_box_Duoduo_secret_1800x1800.png"));
                break;

            case "skullpanda-city-of-night-series":
                list.add(new ItemDef("Law Executor", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_Law_Executor_1800x1800.jpg"));
                list.add(new ItemDef("Puppet Singer", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_Puppet_Singer_1800x1800.jpg"));
                list.add(new ItemDef("Ardent Youth", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_Skullpanda_City_Of_Night_Ardent_Youth_1800x1800.jpg"));
                list.add(new ItemDef("Scroll Delivery", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_Scroll_Delivery_1800x1800.jpg"));
                list.add(new ItemDef("Pet Cat", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_Pet_Cat_1800x1800.jpg"));
                list.add(new ItemDef("Traveller", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_Traveller_1800x1800.jpg"));
                list.add(new ItemDef("Heart Seeker", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_Heart_Seeker_1800x1800.jpg"));
                list.add(new ItemDef("Naughty Bodyguard", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_Naughty_Bodyguard_1800x1800.jpg"));
                list.add(new ItemDef("DJ Player", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_DJ_Player_1800x1800.jpg"));
                list.add(new ItemDef("The Princess", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_The_Princess_1800x1800.jpg"));
                list.add(new ItemDef("Meditator", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Skullpanda_City_Of_Night_Meditator_1800x1800.jpg"));
                list.add(new ItemDef("Dancer", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_Skullpanda_City_Of_Night_Dancer_1800x1800.jpg"));
                list.add(new ItemDef("Guardian of Night", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/popmart_Skullpanda_City_Of_Night_Guardian_of_Night_secret_1800x1800.jpg"));
                break;

            case "skullpanda-the-warmth-series":
                list.add(new ItemDef("The Raining Day", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_The_Raining_Day_1800x1800.jpg"));
                list.add(new ItemDef("The Encounter", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_The_Encounter_1800x1800.jpg"));
                list.add(new ItemDef("The Day Off", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_The_Day_Off_1800x1800.jpg"));
                list.add(new ItemDef("Enjoy Oneself", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_Enjoy_Oneself_1800x1800.jpg"));
                list.add(new ItemDef("Mind With The Wind", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_Mind_With_The_Wind_1800x1800.jpg"));
                list.add(new ItemDef("Doodling", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_Doodling_1800x1800.jpg"));
                list.add(new ItemDef("Wandering", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_Wandering_1800x1800.jpg"));
                list.add(new ItemDef("Recall The Past", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_Recall_The_Past_1800x1800.jpg"));
                list.add(new ItemDef("Chirping", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_Chirping_1800x1800.jpg"));
                list.add(new ItemDef("Loosening", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_Loosening_1800x1800.jpg"));
                list.add(new ItemDef("Taste From The Memory", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_Taste_From_The_Memory_1800x1800.jpg"));
                list.add(new ItemDef("The Scent", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_The_Scent_1800x1800.jpg"));
                list.add(new ItemDef("The Warmth", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Skullpanda_The_Warmth_Series_The_Warmth_secret_1800x1800.jpg"));
                break;

            case "hirono-little-mischief-series":
                list.add(new ItemDef("Ragpicker", RarityType.REGULAR, "/images/popnow/hirono/ragpicker.png"));
                list.add(new ItemDef("Loose Fish", RarityType.REGULAR, "/images/popnow/hirono/loose-fish.png"));
                list.add(new ItemDef("Manacle", RarityType.REGULAR, "/images/popnow/hirono/manacle.png"));
                list.add(new ItemDef("The Aviator", RarityType.REGULAR, "/images/popnow/hirono/the-aviator.png"));
                list.add(new ItemDef("Protector", RarityType.REGULAR, "/images/popnow/hirono/protector.png"));
                list.add(new ItemDef("Persona", RarityType.REGULAR, "/images/popnow/hirono/persona.png"));
                list.add(new ItemDef("Robot", RarityType.REGULAR, "/images/popnow/hirono/robot.png"));
                list.add(new ItemDef("Birdman", RarityType.REGULAR, "/images/popnow/hirono/birdman.png"));
                list.add(new ItemDef("Destroyer", RarityType.REGULAR, "/images/popnow/hirono/destroyer.png"));
                list.add(new ItemDef("Pretender", RarityType.REGULAR, "/images/popnow/hirono/pretender.png"));
                list.add(new ItemDef("Boiling Frog", RarityType.REGULAR, "/images/popnow/hirono/boiling-frog.png"));
                list.add(new ItemDef("Float", RarityType.REGULAR, "/images/popnow/hirono/float.png"));
                list.add(new ItemDef("Unknown Journey", RarityType.SECRET, "/images/popnow/hirono/unknown-journey.png"));
                break;

            case "hirono-the-other-one-series":
                list.add(new ItemDef("Amnesia", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_Hirono_The_Other_One_Series_Amnesia.png"));
                list.add(new ItemDef("The Ghost", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/POP_MART_-_Hirono_The_Other_One_Series_Ghost_-_The_Ghost.png"));
                list.add(new ItemDef("Fox", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_Hirono_The_Other_One_Series_The_Fox.png"));
                list.add(new ItemDef("Staring", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_hirono_the_other_one_series_Staring.png"));
                list.add(new ItemDef("Marionette", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_hirono_the_other_one_series_Marionette.png"));
                list.add(new ItemDef("The Monster", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_hirono_the_other_one_series_The_Monster.png"));
                list.add(new ItemDef("Being Alive", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_hirono_the_other_one_series_Boxing_Alive.png"));
                list.add(new ItemDef("Nowhere Safe", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_Hirono_The_Other_One_Series_Nowhere_Safe.png"));
                list.add(new ItemDef("Vagrant", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_Hirono_The_Other_One_Series_Vagrancy.png"));
                list.add(new ItemDef("The Crow", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_hirono_the_other_one_series_The_Crow.png"));
                list.add(new ItemDef("Raving", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_Hirono_The_Other_One_Series_Raving.png"));
                list.add(new ItemDef("Cuckoo", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_hirono_the_other_one_series_Cuckoo.png"));
                list.add(new ItemDef("Silent Scream", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/popmart_hirono_the_other_one_Dreaming_secret.png"));
                break;

            case "mega-space-molly-100-series-2":
                list.add(new ItemDef("Space Molly Bananaman", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/87221d51f26e77a08e388addbd6f0b51_1800x1800.jpg"));
                list.add(new ItemDef("Space Molly Melting", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/05f23838846685b9e6eac31ed835a68a_1800x1800.jpg"));
                list.add(new ItemDef("Space Molly Basquiat", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/926e3484a7587c8e39cafdb56a4f2196_1800x1800.jpg"));
                list.add(new ItemDef("Space Molly Keith Haring", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/3722d791b1b966bca0b8155f276c6779_1800x1800.jpg"));
                list.add(new ItemDef("Space Molly Heartfelt", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/eb114a19a00b495b7cb8073688755c9a_1800x1800.jpg"));
                list.add(new ItemDef("Space Molly Mint Chocolate", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/39ee4912734cf17a743fbbeec4ed4d71_1800x1800.jpg"));
                list.add(new ItemDef("Space Molly Toffee", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/ef7eb9f5b66d45a85b120836bfe2334a_1800x1800.jpg"));
                list.add(new ItemDef("Space Molly Cheerleader", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/107db3269a7c1bef66ae07d03e673a42_1800x1800.jpg"));
                list.add(new ItemDef("Space Molly Glacier", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/c726a28c0554c6126a1fcc5f957de382_1800x1800.jpg"));
                list.add(new ItemDef("Space Molly Galactic Star", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/be5597caf043c8bec9b8a74a06fd5778_1800x1800.jpg"));
                break;

            case "dimoo-retro-series":
                list.add(new ItemDef("Angel", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Angel_1800x1800.jpg"));
                list.add(new ItemDef("Devil", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Devil_1800x1800.jpg"));
                list.add(new ItemDef("Flamingo", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Flamingo_1800x1800.jpg"));
                list.add(new ItemDef("Elk", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Elk_1800x1800.jpg"));
                list.add(new ItemDef("Little Green Dragon", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Little_Green_Dragon_1800x1800.jpg"));
                list.add(new ItemDef("Joker", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Joker_1800x1800.jpg"));
                list.add(new ItemDef("Magician", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Magician_1800x1800.jpg"));
                list.add(new ItemDef("Rocky Overlord", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Rocky_Overlord_1800x1800.jpg"));
                list.add(new ItemDef("Pajamas Rabbit", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Pajamas_Rabbit_1800x1800.jpg"));
                list.add(new ItemDef("Rocky King", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Rodky_King_1800x1800.jpg"));
                list.add(new ItemDef("Snowy Owl", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Snowy_Owl_1800x1800.jpg"));
                list.add(new ItemDef("Snowball", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Snowball_1800x1800.jpg"));
                list.add(new ItemDef("Golden Walkman", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/popmart_dimoo_retro_series_Dark_Night_secret_1800x1800.jpg"));
                break;

            case "crybaby-crying-parade-series":
                list.add(new ItemDef("The Letter", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_blind_box_popmart_The_Letter_1800x1800.jpg"));
                list.add(new ItemDef("The Drummer", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_blindbox_popmart_The_Drummer_1800x1800.jpg"));
                list.add(new ItemDef("Peace Please", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_blindbox_popmart_Peace_Please_1800x1800.jpg"));
                list.add(new ItemDef("Monkey", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_blindbox_popmart_Monkey_1800x1800.jpg"));
                list.add(new ItemDef("Long Legged Clown", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_blindbox_popmart_Long_Legged_Clown_1800x1800.jpg"));
                list.add(new ItemDef("Keep Go Go", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_blindbox_popmart_Keep_Go_Go_1800x1800.jpg"));
                list.add(new ItemDef("Good Girl", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_blindbox_popmart_Good_Girl_1800x1800.jpg"));
                list.add(new ItemDef("Free Lion", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_popmart_Free_Lion_1800x1800.jpg"));
                list.add(new ItemDef("Fall", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_popmart_fall_1800x1800.jpg"));
                list.add(new ItemDef("Yes Can Can", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_popmart_yes_can_can_1800x1800.jpg"));
                list.add(new ItemDef("Ugly Duckling", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_popmart_blindbox_ugly_duckling_1800x1800.jpg"));
                list.add(new ItemDef("The Trumpet", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_popmart_blindbox_The_Trumpet_1800x1800.jpg"));
                list.add(new ItemDef("The Saddest King", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/crybaby_crying_parade_series_blindbox_popmart_The_Saddest_King_secret_1800x1800.jpg"));
                break;

            case "crybaby-sad-club-series":
                list.add(new ItemDef("Big Cleaning Day", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Crybaby_Sad_Clunb_Series_Scene_Sets_Big_Cleaning_Day_1800x1800.jpg"));
                list.add(new ItemDef("Teardrops On The Pillow", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Crybaby_Sad_Clunb_Series_Scene_Sets_Teardrops_On_The_Pillow_1800x1800.jpg"));
                list.add(new ItemDef("The Hottest Day Of Summer", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Crybaby_Sad_Clunb_Series_Scene_Sets_The_Hottest_Day_Of_Summer_1800x1800.jpg"));
                list.add(new ItemDef("Withering Flower", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Crybaby_Sad_Clunb_Series_Scene_Sets_Withering_Flower_1800x1800.jpg"));
                list.add(new ItemDef("Teardrop Bowl", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Crybaby_Sad_Clunb_Series_Scene_Sets_Teardrop_Bowl_1800x1800.jpg"));
                list.add(new ItemDef("Devastated", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Crybaby_Sad_Clunb_Series_Scene_Sets_Devastated_1800x1800.jpg"));
                list.add(new ItemDef("A Sad Show", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/Popmart_Crybaby_Sad_Clunb_Series_Scene_Sets_A_Sad_Show_Secret_1800x1800.jpg"));
                break;

            case "dimoo-dating-series":
                list.add(new ItemDef("Ice Cream", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/dimoo_dating_series_Ice_Cream_1800x1800.jpg"));
                list.add(new ItemDef("Marshmallow", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/dimoo_dating_series_Marshmallow_1800x1800.jpg"));
                list.add(new ItemDef("Love Theatre", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/dimoo_dating_series_Love_Theatre_1800x1800.jpg"));
                list.add(new ItemDef("Love Fountain", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/dimoo_dating_series_Love_Fountain_1800x1800.jpg"));
                list.add(new ItemDef("Joyriding Bumper Car", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/dimoo_dating_series_Joyriding_Bumper_Car_1800x1800.jpg"));
                list.add(new ItemDef("Record Anniversary", RarityType.REGULAR, "https://arttoyfamilia.com/cdn/shop/files/dimoo_dating_series_Record_Anniversary_1800x1800.jpg"));
                list.add(new ItemDef("Photo Prop Wall", RarityType.SECRET, "https://arttoyfamilia.com/cdn/shop/files/dimoo_dating_series_Photo_Prop_Wall_secret_1800x1800.jpg"));
                break;

            case "hirono-echoes-of-silence-series":
            case "hop-mu-hirono-echoes-of-silence-series":
                list.add(new ItemDef("The Ghost", RarityType.REGULAR, "https://prod-global-biz.popmart.com/globalAdmin/1785914967215_a365144942c74b86433a0af1c63446cb.jpg"));
                list.add(new ItemDef("Shelter", RarityType.REGULAR, "https://prod-global-biz.popmart.com/globalAdmin/1785914967215_a365144942c74b86433a0af1c63446cb.jpg"));
                list.add(new ItemDef("The Fox", RarityType.REGULAR, "https://prod-global-biz.popmart.com/globalAdmin/1785914967215_a365144942c74b86433a0af1c63446cb.jpg"));
                list.add(new ItemDef("Standing", RarityType.REGULAR, "https://prod-global-biz.popmart.com/globalAdmin/1785914967215_a365144942c74b86433a0af1c63446cb.jpg"));
                list.add(new ItemDef("Marionette", RarityType.REGULAR, "https://prod-global-biz.popmart.com/globalAdmin/1785914967215_a365144942c74b86433a0af1c63446cb.jpg"));
                list.add(new ItemDef("Monster", RarityType.REGULAR, "https://prod-global-biz.popmart.com/globalAdmin/1785914967215_a365144942c74b86433a0af1c63446cb.jpg"));
                list.add(new ItemDef("Broken", RarityType.REGULAR, "https://prod-global-biz.popmart.com/globalAdmin/1785914967215_a365144942c74b86433a0af1c63446cb.jpg"));
                list.add(new ItemDef("Silent", RarityType.REGULAR, "https://prod-global-biz.popmart.com/globalAdmin/1785914967215_a365144942c74b86433a0af1c63446cb.jpg"));
                list.add(new ItemDef("The Boy", RarityType.REGULAR, "https://prod-global-biz.popmart.com/globalAdmin/1785914967215_a365144942c74b86433a0af1c63446cb.jpg"));
                list.add(new ItemDef("Puppet", RarityType.REGULAR, "https://prod-global-biz.popmart.com/globalAdmin/1785914967215_a365144942c74b86433a0af1c63446cb.jpg"));
                list.add(new ItemDef("Memory", RarityType.REGULAR, "https://prod-global-biz.popmart.com/globalAdmin/1785914967215_a365144942c74b86433a0af1c63446cb.jpg"));
                list.add(new ItemDef("Floating", RarityType.REGULAR, "https://prod-global-biz.popmart.com/globalAdmin/1785914967215_a365144942c74b86433a0af1c63446cb.jpg"));
                list.add(new ItemDef("Time", RarityType.SECRET, "https://prod-global-biz.popmart.com/globalAdmin/1785914967215_a365144942c74b86433a0af1c63446cb.jpg"));
                break;
        }
        return list;
    }

    private void cleanupTestAndOrphanProducts() {
        List<Product> all = productRepository.findAll();
        for (Product p : all) {
            String slug = p.getSlug() != null ? p.getSlug().toLowerCase() : "";
            String name = p.getName() != null ? p.getName().toLowerCase() : "";

            boolean isTestDummy = slug.startsWith("popnow-idor-product")
                    || slug.startsWith("molly-space-blind-box-")
                    || slug.startsWith("cancelled-res-toy")
                    || slug.startsWith("unpaid-box-toy")
                    || slug.startsWith("popnow-series-box-")
                    || slug.startsWith("dimoo-dating-series-blind-box-")
                    || name.contains("idor product")
                    || name.contains("cancelled res toy")
                    || name.contains("unpaid box toy")
                    || name.contains("popnow series box")
                    || slug.equals("hop-mu-test")
                    || name.contains("hộp mù test");

            if (isTestDummy) {
                log.info("PopWorld Seed: Đang dọn dẹp sản phẩm rác E2E: ID={}, slug={}", p.getId(), p.getSlug());
                deleteProductSafely(p.getId());
            }
        }
    }

    private void deleteProductSafely(Long productId) {
        try {
            // 0. Xóa các vật phẩm sở hữu trong tủ đồ phát sinh từ sản phẩm kiểm thử này
            entityManager.createQuery("DELETE FROM OwnedItem o WHERE o.product.id = :pid")
                    .setParameter("pid", productId)
                    .executeUpdate();
            entityManager.createQuery("UPDATE OwnedItem o SET o.reservation = null WHERE o.reservation.product.id = :pid")
                    .setParameter("pid", productId)
                    .executeUpdate();

            // 1. Gỡ liên kết slot trong reservation
            entityManager.createQuery("UPDATE BoxReservation r SET r.slot = null WHERE r.product.id = :pid")
                    .setParameter("pid", productId)
                    .executeUpdate();

            // 2. Gỡ liên kết currentReservation trong slot
            entityManager.createQuery("UPDATE BlindBoxSlot s SET s.currentReservation = null WHERE s.product.id = :pid")
                    .setParameter("pid", productId)
                    .executeUpdate();

            // 3. Xóa reservation
            entityManager.createQuery("DELETE FROM BoxReservation r WHERE r.product.id = :pid")
                    .setParameter("pid", productId)
                    .executeUpdate();

            // 4. Xóa slot
            entityManager.createQuery("DELETE FROM BlindBoxSlot s WHERE s.product.id = :pid")
                    .setParameter("pid", productId)
                    .executeUpdate();

            // 5. Xóa blind box items
            entityManager.createQuery("DELETE FROM BlindBoxItem i WHERE i.product.id = :pid")
                    .setParameter("pid", productId)
                    .executeUpdate();

            // 6. Xóa cart items
            entityManager.createQuery("DELETE FROM CartItem c WHERE c.product.id = :pid")
                    .setParameter("pid", productId)
                    .executeUpdate();

            // 7. Xóa wishlist items
            entityManager.createQuery("DELETE FROM WishlistItem w WHERE w.product.id = :pid")
                    .setParameter("pid", productId)
                    .executeUpdate();

            // 8. Xóa review
            entityManager.createQuery("DELETE FROM Review r WHERE r.product.id = :pid")
                    .setParameter("pid", productId)
                    .executeUpdate();

            // 9. Xóa product images
            entityManager.createQuery("DELETE FROM ProductImage pi WHERE pi.product.id = :pid")
                    .setParameter("pid", productId)
                    .executeUpdate();

            // 10. Kiểm tra OrderItem
            Long orderCount = entityManager.createQuery("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.product.id = :pid", Long.class)
                    .setParameter("pid", productId)
                    .getSingleResult();

            if (orderCount != null && orderCount > 0) {
                // Đã có đơn hàng liên kết -> Soft delete
                entityManager.createQuery("UPDATE Product p SET p.active = false WHERE p.id = :pid")
                        .setParameter("pid", productId)
                        .executeUpdate();
                log.info("Sản phẩm ID={} đã có đơn hàng -> Soft delete (active=false)", productId);
            } else {
                // Chưa có đơn hàng -> Xóa vĩnh viễn
                entityManager.createQuery("DELETE FROM Product p WHERE p.id = :pid")
                        .setParameter("pid", productId)
                        .executeUpdate();
                log.info("Sản phẩm ID={} chưa có đơn hàng -> Đã xóa hoàn toàn khỏi CSDL", productId);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xóa an toàn sản phẩm ID={}: {}", productId, e.getMessage());
        }
    }

    private boolean isItemReferencedInOwnedItems(Long itemId) {
        if (itemId == null) return false;
        try {
            Long count = entityManager.createQuery("SELECT COUNT(o) FROM OwnedItem o WHERE o.blindBoxItem.id = :itemId", Long.class)
                    .setParameter("itemId", itemId)
                    .getSingleResult();
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
