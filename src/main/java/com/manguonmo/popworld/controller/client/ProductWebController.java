package com.manguonmo.popworld.controller.client;

import com.manguonmo.popworld.entity.Category;
import com.manguonmo.popworld.entity.CharacterIp;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.service.CartService;
import com.manguonmo.popworld.service.CategoryService;
import com.manguonmo.popworld.service.CharacterIpService;
import com.manguonmo.popworld.service.ProductService;
import com.manguonmo.popworld.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.manguonmo.popworld.dto.request.ReviewCreateRequest;
import com.manguonmo.popworld.dto.response.ReviewResponse;
import com.manguonmo.popworld.service.ReviewService;
import com.manguonmo.popworld.service.WishlistService;

import java.math.BigDecimal;
import com.manguonmo.popworld.dto.response.BlindBoxItemResponse;
import com.manguonmo.popworld.service.PopNowService;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class ProductWebController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CharacterIpService characterIpService;
    private final CartService cartService;
    private final UserService userService;
    private final ReviewService reviewService;
    private final PopNowService popNowService;
    private final WishlistService wishlistService;

    public ProductWebController(ProductService productService,
                                CategoryService categoryService,
                                CharacterIpService characterIpService,
                                CartService cartService,
                                UserService userService,
                                ReviewService reviewService,
                                PopNowService popNowService,
                                WishlistService wishlistService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.characterIpService = characterIpService;
        this.cartService = cartService;
        this.userService = userService;
        this.reviewService = reviewService;
        this.popNowService = popNowService;
        this.wishlistService = wishlistService;
    }

    private void addCommonAttributes(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("characterIps", characterIpService.getAllCharacterIps());
        int cartCount = 0;
        int wishlistCount = 0;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                User user = userService.getUserByEmail(auth.getName());
                if (user != null) {
                    cartCount = cartService.getCartCount(user.getId());
                    wishlistCount = (int) wishlistService.getWishlistCount(user.getId());
                }
            }
        } catch (Exception ignored) {
        }
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("wishlistCount", wishlistCount);
    }


    @GetMapping("/products/{slug}")
    public String productDetail(@PathVariable String slug, Model model) {
        addCommonAttributes(model);
        Optional<Product> productOpt = productService.getProductBySlug(slug);
        if (productOpt.isEmpty()) {
            return "redirect:/";
        }

        Product product = productOpt.get();
        model.addAttribute("product", product);

        if (product.getCategory() != null) {
            List<Product> related = productService.getProductsByCategorySlug(product.getCategory().getSlug());
            model.addAttribute("relatedProducts", related.stream()
                    .filter(p -> !p.getId().equals(product.getId()))
                    .limit(4)
                    .toList());
        } else {
            model.addAttribute("relatedProducts", Collections.emptyList());
        }

        // Đánh giá đã duyệt (Approved Reviews) hiển thị công khai
        List<ReviewResponse> approvedReviews = reviewService.getApprovedReviewsByProductId(product.getId());
        model.addAttribute("reviews", approvedReviews);
        int totalReviews = approvedReviews.size();
        double avgRating = 0.0;
        if (totalReviews > 0) {
            avgRating = BigDecimal.valueOf(approvedReviews.stream().mapToInt(ReviewResponse::getRating).average().orElse(5.0))
                    .setScale(1, RoundingMode.HALF_UP).doubleValue();
        }
        Map<Integer, Long> starCounts = approvedReviews.stream()
                .collect(Collectors.groupingBy(ReviewResponse::getRating, Collectors.counting()));

        // Danh sách hình ảnh unboxing từ collectors thực tế
        List<ReviewResponse> unboxingPhotos = approvedReviews.stream()
                .filter(r -> r.getReviewImageUrl() != null && !r.getReviewImageUrl().isBlank())
                .toList();

        model.addAttribute("reviewsCount", totalReviews);
        model.addAttribute("averageRating", avgRating);
        model.addAttribute("starCounts", starCounts);
        model.addAttribute("unboxingPhotos", unboxingPhotos);

        boolean canReview = false;
        ReviewResponse userReview = null;
        boolean isWishlisted = false;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                User user = userService.getUserByEmail(auth.getName());
                if (user != null) {
                    canReview = reviewService.isUserEligibleToReview(user.getId(), product.getId());
                    userReview = reviewService.getUserReviewForProduct(user.getId(), product.getId());
                    isWishlisted = wishlistService.isWishlisted(user.getId(), product.getId());
                }
            }
        } catch (Exception ignored) {
        }
        model.addAttribute("canReview", canReview);
        model.addAttribute("userReview", userReview);
        model.addAttribute("isWishlisted", isWishlisted);
        model.addAttribute("reviewForm", new ReviewCreateRequest());

        // Lấy danh sách nhân vật trong series cho Showcase / Figure Styles / All Characters
        List<BlindBoxItemResponse> characters = Collections.emptyList();
        try {
            characters = popNowService.getSeriesItems(product.getId());
        } catch (Exception ignored) {
        }
        model.addAttribute("characters", characters);

        BlindBoxItemResponse secretCharacter = characters.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsSecret()))
                .findFirst()
                .orElse(null);
        model.addAttribute("secretCharacter", secretCharacter);

        List<BlindBoxItemResponse> regularCharacters = characters.stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsSecret()))
                .toList();
        model.addAttribute("regularCharacters", regularCharacters);

        long estimatedPoints = product.getSinglePrice() != null ? product.getSinglePrice().longValue() / 1000 : 0;
        model.addAttribute("estimatedPoints", estimatedPoints);

        return "product-detail";
    }

    @GetMapping({"/products", "/collection/{slug}", "/collections/{slug}"})
    public String products(
            @PathVariable(required = false) String slug,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long character,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "recommend") String sort,
            @RequestParam(required = false) Boolean popNow,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) String priceRange,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean newRelease,
            @RequestParam(defaultValue = "1") int page,
            Model model) {
        String catSlug = category;
        if (slug != null && !slug.isBlank()) {
            if ("trending".equalsIgnoreCase(slug) || "trending-now".equalsIgnoreCase(slug)) {
                featured = true;
                if ("recommend".equalsIgnoreCase(sort)) sort = "best_selling";
            } else if ("latest-drops".equalsIgnoreCase(slug) || "new-arrivals".equalsIgnoreCase(slug)) {
                newRelease = true;
                if ("recommend".equalsIgnoreCase(sort)) sort = "latest";
            } else if ("iconic_series".equalsIgnoreCase(slug) || "iconic-series".equalsIgnoreCase(slug)) {
                featured = true;
            } else if (catSlug == null || catSlug.isBlank()) {
                catSlug = slug;
            }
        }
        return buildCatalogModel(catSlug, character, keyword, sort, popNow, inStock, priceRange, featured, newRelease, page, model);
    }

    @GetMapping("/categories/{slug}")
    public String productsByCategory(
            @PathVariable String slug,
            @RequestParam(required = false) Long character,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "recommend") String sort,
            @RequestParam(required = false) Boolean popNow,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) String priceRange,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean newRelease,
            @RequestParam(defaultValue = "1") int page,
            Model model) {
        return buildCatalogModel(slug, character, keyword, sort, popNow, inStock, priceRange, featured, newRelease, page, model);
    }

    @GetMapping("/characters/{id}")
    public String productsByCharacter(
            @PathVariable Long id,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "recommend") String sort,
            @RequestParam(required = false) Boolean popNow,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) String priceRange,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean newRelease,
            @RequestParam(defaultValue = "1") int page,
            Model model) {
        return buildCatalogModel(category, id, keyword, sort, popNow, inStock, priceRange, featured, newRelease, page, model);
    }

    @GetMapping("/search")
    public String searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long character,
            @RequestParam(defaultValue = "recommend") String sort,
            @RequestParam(required = false) Boolean popNow,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) String priceRange,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean newRelease,
            @RequestParam(defaultValue = "1") int page,
            Model model) {
        return buildCatalogModel(category, character, keyword, sort, popNow, inStock, priceRange, featured, newRelease, page, model);
    }

    private String buildCatalogModel(
            String categorySlug,
            Long characterId,
            String keyword,
            String sort,
            Boolean popNow,
            Boolean inStock,
            String priceRange,
            Boolean featured,
            Boolean newRelease,
            int page,
            Model model) {
        addCommonAttributes(model);

        List<Product> allProducts = productService.getAllActiveProducts();

        // 1. Lọc theo Category
        Category selectedCategory = null;
        if (categorySlug != null && !categorySlug.isBlank()) {
            Optional<Category> catOpt = categoryService.getCategoryBySlug(categorySlug);
            if (catOpt.isPresent()) {
                selectedCategory = catOpt.get();
                allProducts = allProducts.stream()
                        .filter(p -> p.getCategory() != null && categorySlug.equalsIgnoreCase(p.getCategory().getSlug()))
                        .toList();
            }
        }

        // 2. Lọc theo Character IP
        CharacterIp selectedCharacter = null;
        if (characterId != null) {
            Optional<CharacterIp> charOpt = characterIpService.getCharacterIpById(characterId);
            if (charOpt.isPresent()) {
                selectedCharacter = charOpt.get();
                allProducts = allProducts.stream()
                        .filter(p -> p.getSeries() != null && p.getSeries().getCharacterIp() != null
                                && characterId.equals(p.getSeries().getCharacterIp().getId()))
                        .toList();
            }
        }

        // 3. Lọc theo Từ khóa tìm kiếm
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase().trim();
            allProducts = allProducts.stream()
                    .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(kw))
                            || (p.getSeries() != null && p.getSeries().getName() != null && p.getSeries().getName().toLowerCase().contains(kw))
                            || (p.getCategory() != null && p.getCategory().getName() != null && p.getCategory().getName().toLowerCase().contains(kw)))
                    .toList();
        }

        // 4. Lọc POP NOW (bóc hộp online / blind box chuẩn Pop Mart)
        if (Boolean.TRUE.equals(popNow)) {
            allProducts = allProducts.stream()
                    .filter(Product::isPopNowEligible)
                    .toList();
        }

        // 5. Lọc Có Sẵn Giao Nhanh (Local Shipping / in-stock)
        if (Boolean.TRUE.equals(inStock)) {
            allProducts = allProducts.stream()
                    .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() > 0)
                    .toList();
        }

        // 6. Lọc theo Khoảng Giá
        if (priceRange != null && !priceRange.isBlank()) {
            switch (priceRange) {
                case "under_500k" -> allProducts = allProducts.stream()
                        .filter(p -> p.getSinglePrice() != null && p.getSinglePrice().compareTo(new BigDecimal("500000")) < 0)
                        .toList();
                case "500k_1m" -> allProducts = allProducts.stream()
                        .filter(p -> p.getSinglePrice() != null && p.getSinglePrice().compareTo(new BigDecimal("500000")) >= 0
                                && p.getSinglePrice().compareTo(new BigDecimal("1000000")) <= 0)
                        .toList();
                case "1m_3m" -> allProducts = allProducts.stream()
                        .filter(p -> p.getSinglePrice() != null && p.getSinglePrice().compareTo(new BigDecimal("1000000")) > 0
                                && p.getSinglePrice().compareTo(new BigDecimal("3000000")) <= 0)
                        .toList();
                case "above_3m" -> allProducts = allProducts.stream()
                        .filter(p -> p.getSinglePrice() != null && p.getSinglePrice().compareTo(new BigDecimal("3000000")) > 0)
                        .toList();
            }
        }

        // 6.5. Lọc theo Featured (Trending) & New Release (Latest Drops)
        if (Boolean.TRUE.equals(featured)) {
            allProducts = allProducts.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsFeatured()))
                    .toList();
        }
        if (Boolean.TRUE.equals(newRelease)) {
            allProducts = allProducts.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsNewRelease()))
                    .toList();
        }

        // 7. Sắp xếp (Sorting)
        List<Product> sortedList = new java.util.ArrayList<>(allProducts);
        String activeSort = (sort != null && !sort.isBlank()) ? sort.toLowerCase() : "recommend";
        switch (activeSort) {
            case "latest" -> sortedList.sort((a, b) -> {
                if (Boolean.TRUE.equals(b.getIsNewRelease()) && !Boolean.TRUE.equals(a.getIsNewRelease())) return 1;
                if (Boolean.TRUE.equals(a.getIsNewRelease()) && !Boolean.TRUE.equals(b.getIsNewRelease())) return -1;
                return b.getId().compareTo(a.getId());
            });
            case "best_selling" -> sortedList.sort((a, b) -> {
                if (Boolean.TRUE.equals(b.getIsFeatured()) && !Boolean.TRUE.equals(a.getIsFeatured())) return 1;
                if (Boolean.TRUE.equals(a.getIsFeatured()) && !Boolean.TRUE.equals(b.getIsFeatured())) return -1;
                return b.getId().compareTo(a.getId());
            });
            case "price_asc" -> sortedList.sort(Comparator.comparing(Product::getSinglePrice, Comparator.nullsLast(BigDecimal::compareTo)));
            case "price_desc" -> sortedList.sort((a, b) -> {
                if (a.getSinglePrice() == null) return 1;
                if (b.getSinglePrice() == null) return -1;
                return b.getSinglePrice().compareTo(a.getSinglePrice());
            });
            default -> sortedList.sort((a, b) -> {
                if (Boolean.TRUE.equals(b.getIsFeatured()) && !Boolean.TRUE.equals(a.getIsFeatured())) return 1;
                if (Boolean.TRUE.equals(a.getIsFeatured()) && !Boolean.TRUE.equals(b.getIsFeatured())) return -1;
                return b.getId().compareTo(a.getId());
            });
        }

        // 8. Phân trang (15 sản phẩm / trang = 3 hàng x 5 cột)
        int pageSize = 15;
        int totalItems = sortedList.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / pageSize));
        int currentPage = Math.max(1, Math.min(page, totalPages));
        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalItems);
        List<Product> pagedProducts = (fromIndex < totalItems) ? sortedList.subList(fromIndex, toIndex) : Collections.emptyList();

        // 9. Xác định Tiêu đề, Mô tả và Theme Màu Pastel cho Panoramic Banner
        String pageTitle = "ALL PRODUCTS";
        String pageDescription = "Explore the complete designer art toy universe and official blind boxes.";
        String bannerTheme = "banner-theme-default";

        if (Boolean.TRUE.equals(featured) && selectedCategory == null && selectedCharacter == null) {
            pageTitle = "TRENDING & FEATURED";
            pageDescription = "Top các mẫu Art Toy, Blind Box được cộng đồng săn đón và bán chạy nhất.";
            bannerTheme = "banner-theme-mega";
        } else if (Boolean.TRUE.equals(newRelease) && selectedCategory == null && selectedCharacter == null) {
            pageTitle = "LATEST DROPS & NEW ARRIVALS";
            pageDescription = "Các bộ sưu tập mới ra mắt và chuẩn bị mở bán đón đầu xu hướng.";
            bannerTheme = "banner-theme-blindbox";
        } else if (selectedCategory != null) {
            pageTitle = selectedCategory.getName().toUpperCase();
            pageDescription = (selectedCategory.getDescription() != null && !selectedCategory.getDescription().isBlank())
                    ? selectedCategory.getDescription()
                    : "Discover exclusive art pieces and blind boxes in this category.";
            if (categorySlug.contains("plush")) {
                bannerTheme = "banner-theme-plush";
            } else if (categorySlug.contains("blind")) {
                bannerTheme = "banner-theme-blindbox";
            } else if (categorySlug.contains("mega")) {
                bannerTheme = "banner-theme-mega";
            } else if (categorySlug.contains("accessories")) {
                bannerTheme = "banner-theme-default";
            }
        } else if (selectedCharacter != null) {
            pageTitle = selectedCharacter.getName().toUpperCase() + " COLLECTION";
            pageDescription = (selectedCharacter.getDescription() != null && !selectedCharacter.getDescription().isBlank())
                    ? selectedCharacter.getDescription()
                    : "Iconic art toy collections featuring " + selectedCharacter.getName() + ".";
            bannerTheme = "banner-theme-character";
        } else if (keyword != null && !keyword.isBlank()) {
            pageTitle = "SEARCH: \"" + keyword.toUpperCase() + "\"";
            pageDescription = "Found " + totalItems + " art toy items matching your query.";
        }

        Set<Long> wishlistProductIds = Collections.emptySet();
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                User user = userService.getUserByEmail(auth.getName());
                if (user != null) {
                    wishlistProductIds = wishlistService.getWishlistProductIds(user.getId());
                }
            }
        } catch (Exception ignored) {
        }

        model.addAttribute("products", pagedProducts);
        model.addAttribute("wishlistProductIds", wishlistProductIds);
        model.addAttribute("totalProducts", totalItems);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", pageSize);

        model.addAttribute("currentCategory", selectedCategory);
        model.addAttribute("currentCharacter", selectedCharacter);
        model.addAttribute("selectedCategorySlug", categorySlug);
        model.addAttribute("selectedCharacterId", characterId);
        model.addAttribute("selectedSort", activeSort);
        model.addAttribute("selectedPopNow", popNow);
        model.addAttribute("selectedInStock", inStock);
        model.addAttribute("selectedPriceRange", priceRange);
        model.addAttribute("selectedFeatured", featured);
        model.addAttribute("selectedNewRelease", newRelease);
        model.addAttribute("keyword", keyword);

        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageDescription", pageDescription);
        model.addAttribute("bannerTheme", bannerTheme);

        return "product-list";
    }
}
