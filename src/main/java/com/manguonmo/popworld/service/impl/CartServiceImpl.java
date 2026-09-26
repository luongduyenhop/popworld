package com.manguonmo.popworld.service.impl;

import com.manguonmo.popworld.exception.BadRequestException;
import com.manguonmo.popworld.exception.OutOfStockException;
import com.manguonmo.popworld.exception.ResourceNotFoundException;
import com.manguonmo.popworld.service.CartService;
import com.manguonmo.popworld.entity.CartItem;
import com.manguonmo.popworld.entity.Product;
import com.manguonmo.popworld.entity.User;
import com.manguonmo.popworld.repository.CartItemRepository;
import com.manguonmo.popworld.repository.ProductRepository;
import com.manguonmo.popworld.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartServiceImpl(CartItemRepository cartItemRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(Long userId) {
        return cartItemRepository.findByUserId(userId);
    }

    @Override
    public CartItem addToCart(Long userId, Long productId, String purchaseType, int quantity) {
        if (userId == null) {
            throw new BadRequestException("Yêu cầu xác thực người dùng để thêm vào giỏ hàng.");
        }
        if (quantity <= 0) {
            throw new BadRequestException("Số lượng sản phẩm phải lớn hơn 0!");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + userId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm ID: " + productId));

        if (Boolean.FALSE.equals(product.getActive())) {
            throw new BadRequestException("Sản phẩm '" + product.getName() + "' hiện đang tạm dừng mở bán!");
        }

        String type = (purchaseType != null && purchaseType.equalsIgnoreCase("WHOLE_SET")) 
                ? "WHOLE_SET" : "SINGLE_BOX";

        Optional<CartItem> existingItemOpt = cartItemRepository
                .findByUserIdAndProductIdAndPurchaseType(userId, productId, type);

        int requestedBoxes = "WHOLE_SET".equalsIgnoreCase(type) ? quantity * 12 : quantity;
        int totalBoxes = requestedBoxes;
        if (existingItemOpt.isPresent()) {
            int currentQty = existingItemOpt.get().getQuantity();
            totalBoxes = "WHOLE_SET".equalsIgnoreCase(type) ? (currentQty + quantity) * 12 : (currentQty + quantity);
        }

        int availableStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        if (totalBoxes > availableStock) {
            throw new OutOfStockException("Sản phẩm '" + product.getName() + "' không đủ số lượng tồn kho (hiện còn " + availableStock + " hộp)!");
        }

        if (existingItemOpt.isPresent()) {
            CartItem item = existingItemOpt.get();
            item.setQuantity(item.getQuantity() + quantity);
            return cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .user(user)
                    .product(product)
                    .purchaseType(type)
                    .quantity(quantity)
                    .isSelected(true)
                    .build();
            return cartItemRepository.save(newItem);
        }
    }

    @Override
    public void updateQuantity(Long userId, Long cartItemId, int quantity) {
        if (userId == null) {
            throw new BadRequestException("Yêu cầu xác thực người dùng để cập nhật giỏ hàng.");
        }
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món hàng với ID: " + cartItemId));

        if (item.getUser() == null || !item.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền thao tác trên món hàng này!");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            if (item.getProduct() != null && item.getProduct().getStockQuantity() != null) {
                int requestedBoxes = "WHOLE_SET".equalsIgnoreCase(item.getPurchaseType()) ? quantity * 12 : quantity;
                int availableStock = item.getProduct().getStockQuantity();
                if (requestedBoxes > availableStock) {
                    throw new OutOfStockException("Sản phẩm '" + item.getProduct().getName() + "' không đủ số lượng tồn kho (hiện còn " + availableStock + " hộp)!");
                }
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }
    }

    @Override
    public void updateSelection(Long userId, Long cartItemId, boolean isSelected) {
        if (userId == null) {
            throw new BadRequestException("Yêu cầu xác thực người dùng để cập nhật giỏ hàng.");
        }
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món hàng với ID: " + cartItemId));

        if (item.getUser() == null || !item.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền thao tác trên món hàng này!");
        }

        item.setIsSelected(isSelected);
        cartItemRepository.save(item);
    }

    @Override
    public void removeFromCart(Long userId, Long cartItemId) {
        if (userId == null) {
            throw new BadRequestException("Yêu cầu xác thực người dùng để xóa sản phẩm khỏi giỏ.");
        }
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món hàng với ID: " + cartItemId));

        if (item.getUser() == null || !item.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền thao tác trên món hàng này!");
        }

        cartItemRepository.delete(item);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateSelectedTotal(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : items) {
            if (Boolean.TRUE.equals(item.getIsSelected())) {
                BigDecimal unitPrice;
                if ("WHOLE_SET".equalsIgnoreCase(item.getPurchaseType()) && item.getProduct().getWholeSetPrice() != null) {
                    unitPrice = item.getProduct().getWholeSetPrice();
                } else {
                    unitPrice = item.getProduct().getSinglePrice();
                }
                BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                total = total.add(itemTotal);
            }
        }
        return total;
    }

    @Override
    @Transactional(readOnly = true)
    public int getCartCount(Long userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }
}
