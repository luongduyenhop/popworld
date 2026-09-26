package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.dto.request.ProductCreateRequest;
import com.manguonmo.popworld.dto.request.ProductUpdateRequest;
import com.manguonmo.popworld.dto.response.CloudinaryUploadResult;
import com.manguonmo.popworld.dto.response.ProductStatsResponse;
import com.manguonmo.popworld.entity.Category;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.ProductImage;
import com.manguonmo.popworld.entity.Series;
import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.repository.*;
import com.manguonmo.popworld.service.CloudinaryService;
import com.manguonmo.popworld.service.ProductService;
import com.manguonmo.popworld.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepository;
    private final SeriesRepository seriesRepository;
    private final ProductImageRepository productImageRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final CloudinaryService cloudinaryService;

    @Override
    public List<Product> getFeaturedProducts() {
        return productRepo.findByIsFeaturedTrueAndActiveTrue();
    }

    @Override
    public List<Product> getNewReleases() {
        return productRepo.findByIsNewReleaseTrueAndActiveTrue();
    }

    @Override
    public Optional<Product> getProductBySlug(String slug) {
        return productRepo.findBySlug(slug);
    }

    @Override
    public List<Product> getProductsByCategorySlug(String categorySlug) {
        return productRepo.findByCategorySlugAndActiveTrue(categorySlug);
    }

    @Override
    public List<Product> getProductsByCharacterIp(Long characterIpId) {
        return productRepo.findByCharacterIpIdAndActiveTrue(characterIpId);
    }

    @Override
    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return productRepo.findByActiveTrue();
        }
        return productRepo.findByNameContainingIgnoreCaseAndActiveTrue(keyword.trim());
    }

    @Override
    public List<Product> getAllActiveProducts() {
        return productRepo.findByActiveTrue();
    }

    @Override
    public Product getProductById(Long id) {
        if (id == null) {
            throw new BadRequestException("ID sản phẩm không được để trống!");
        }
        return productRepo.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + id)
        );
    }

    @Override
    @Transactional
    public Product createProduct(ProductCreateRequest request) {
        if (request == null) {
            throw new BadRequestException("Dữ liệu tạo sản phẩm không được rỗng!");
        }
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với id: " + request.getCategoryId()));

        Series series = null;
        if (request.getSeriesId() != null) {
            series = seriesRepository.findById(request.getSeriesId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ sưu tập (Series) với id: " + request.getSeriesId()));
        }

        // Tạo slug chuẩn hóa và đảm bảo tính duy nhất
        String baseSlug = SlugUtils.toSlug(request.getName());
        String slug = baseSlug;
        int counter = 1;
        while (productRepo.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        Product product = Product.builder()
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription())
                .singlePrice(request.getSinglePrice())
                .wholeSetPrice(request.getWholeSetPrice())
                .stockQuantity(request.getStockQuantity())
                .packagingType(request.getPackagingType())
                .secretRatio(request.getSecretRatio())
                .material(request.getMaterial())
                .sizeDimensions(request.getSizeDimensions())
                .category(category)
                .series(series)
                .isFeatured(Boolean.TRUE.equals(request.getIsFeatured()))
                .isNewRelease(Boolean.TRUE.equals(request.getIsNewRelease()))
                .active(request.getActive() != null ? request.getActive() : true)
                .images(new ArrayList<>())
                .build();

        Product savedProduct = productRepo.save(product);

        // Upload ảnh lên Cloudinary
        if (request.getImageFiles() != null && !request.getImageFiles().isEmpty()) {
            int order = 0;
            for (MultipartFile file : request.getImageFiles()) {
                if (file != null && !file.isEmpty()) {
                    CloudinaryUploadResult uploadResult = cloudinaryService.uploadImage(file);
                    boolean isThumbnail = (order == 0); // Ảnh đầu tiên làm thumbnail
                    ProductImage pImg = ProductImage.builder()
                            .product(savedProduct)
                            .imageUrl(uploadResult.getSecureUrl())
                            .publicId(uploadResult.getPublicId())
                            .isThumbnail(isThumbnail)
                            .displayOrder(order++)
                            .build();
                    productImageRepository.save(pImg);
                    savedProduct.getImages().add(pImg);
                }
            }
        }

        return savedProduct;
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, ProductUpdateRequest request) {
        Product product = getProductById(id);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với id: " + request.getCategoryId()));

        Series series = null;
        if (request.getSeriesId() != null) {
            series = seriesRepository.findById(request.getSeriesId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ sưu tập (Series) với id: " + request.getSeriesId()));
        }

        // Cập nhật thông tin (giữ nguyên slug cũ theo yêu cầu nghiệp vụ)
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setSinglePrice(request.getSinglePrice());
        product.setWholeSetPrice(request.getWholeSetPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setPackagingType(request.getPackagingType());
        product.setSecretRatio(request.getSecretRatio());
        product.setMaterial(request.getMaterial());
        product.setSizeDimensions(request.getSizeDimensions());
        product.setCategory(category);
        product.setSeries(series);
        product.setIsFeatured(Boolean.TRUE.equals(request.getIsFeatured()));
        product.setIsNewRelease(Boolean.TRUE.equals(request.getIsNewRelease()));
        product.setActive(request.getActive() != null ? request.getActive() : true);

        // Xóa các ảnh được đánh dấu xóa
        if (request.getDeleteImageIds() != null && !request.getDeleteImageIds().isEmpty()) {
            for (Long deleteId : request.getDeleteImageIds()) {
                ProductImage img = productImageRepository.findById(deleteId).orElse(null);
                if (img != null && img.getProduct().getId().equals(product.getId())) {
                    if (img.getPublicId() != null && !img.getPublicId().isBlank()) {
                        cloudinaryService.deleteImage(img.getPublicId());
                    }
                    product.getImages().remove(img);
                    productImageRepository.delete(img);
                }
            }
            productImageRepository.flush();
        }

        // Tải lên các ảnh mới nếu có
        if (request.getNewImageFiles() != null && !request.getNewImageFiles().isEmpty()) {
            int currentMaxOrder = product.getImages().stream()
                    .mapToInt(ProductImage::getDisplayOrder)
                    .max()
                    .orElse(-1);
            for (MultipartFile file : request.getNewImageFiles()) {
                if (file != null && !file.isEmpty()) {
                    CloudinaryUploadResult uploadResult = cloudinaryService.uploadImage(file);
                    ProductImage pImg = ProductImage.builder()
                            .product(product)
                            .imageUrl(uploadResult.getSecureUrl())
                            .publicId(uploadResult.getPublicId())
                            .isThumbnail(false)
                            .displayOrder(++currentMaxOrder)
                            .build();
                    ProductImage savedImg = productImageRepository.save(pImg);
                    product.getImages().add(savedImg);
                }
            }
        }

        // Đặt thumbnail theo yêu cầu nếu có chỉ định thumbnailImageId
        if (request.getThumbnailImageId() != null) {
            for (ProductImage img : product.getImages()) {
                img.setIsThumbnail(img.getId().equals(request.getThumbnailImageId()));
                productImageRepository.save(img);
            }
        }

        // Bất biến: Mỗi Product có tối đa 1 thumbnail. Nếu xóa thumbnail, ảnh có displayOrder nhỏ nhất lên làm thumbnail.
        boolean hasThumbnail = product.getImages().stream().anyMatch(img -> Boolean.TRUE.equals(img.getIsThumbnail()));
        if (!hasThumbnail && !product.getImages().isEmpty()) {
            ProductImage lowestOrderImg = product.getImages().stream()
                    .min(Comparator.comparing(ProductImage::getDisplayOrder).thenComparing(ProductImage::getId))
                    .orElse(product.getImages().get(0));
            lowestOrderImg.setIsThumbnail(true);
            productImageRepository.save(lowestOrderImg);
        }

        return productRepo.save(product);
    }

    @Override
    @Transactional
    public boolean deleteProduct(Long id) {
        Product product = getProductById(id);

        boolean hasOrders = orderItemRepository.existsByProductId(id);
        if (hasOrders) {
            // Có OrderItem liên kết -> Soft delete (tạm dừng bán) để bảo toàn dữ liệu lịch sử
            product.setActive(false);
            productRepo.save(product);
            log.info("Sản phẩm ID={} đã có đơn hàng -> Thực hiện Soft Delete (active=false)", id);
            return false;
        } else {
            // Không có OrderItem -> Xóa vĩnh viễn (Physical delete) + dọn dẹp giỏ hàng + dọn dẹp Cloudinary
            cartItemRepository.deleteByProductId(id);

            List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(id);
            for (ProductImage img : images) {
                if (img.getPublicId() != null && !img.getPublicId().isBlank()) {
                    cloudinaryService.deleteImage(img.getPublicId());
                }
            }
            productRepo.delete(product);
            log.info("Sản phẩm ID={} chưa có đơn hàng -> Thực hiện Physical Delete và dọn dẹp Cloudinary", id);
            return true;
        }
    }

    @Override
    @Transactional
    public void setThumbnailImage(Long productId, Long imageId) {
        Product product = getProductById(productId);
        for (ProductImage img : product.getImages()) {
            img.setIsThumbnail(img.getId().equals(imageId));
            productImageRepository.save(img);
        }
    }

    @Override
    public List<Product> getAdminProducts(Long categoryId, String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return productRepo.findByNameContainingIgnoreCaseAndActiveTrue(keyword.trim());
        } else if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId).orElse(null);
            if (category != null) {
                return productRepo.findByCategorySlugAndActiveTrue(category.getSlug());
            }
        }
        return productRepo.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public ProductStatsResponse getProductStats() {
        return ProductStatsResponse.builder()
                .totalCount(productRepo.count())
                .activeCount(productRepo.countByActiveTrue())
                .lowStockCount(productRepo.countByStockQuantityLessThanEqual(10))
                .build();
    }

    @Override
    @Transactional
    public Product updateStock(Long id, Integer stockQuantity) {
        if (stockQuantity == null || stockQuantity < 0) {
            throw new BadRequestException("Số lượng tồn kho không hợp lệ!");
        }
        Product product = getProductById(id);
        product.setStockQuantity(stockQuantity);
        return productRepo.save(product);
    }

    @Override
    @Transactional
    public Product toggleActive(Long id) {
        Product product = getProductById(id);
        boolean newStatus = !Boolean.TRUE.equals(product.getActive());
        product.setActive(newStatus);
        return productRepo.save(product);
    }
}
