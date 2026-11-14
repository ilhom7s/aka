package com.analytics.service.services;


import com.analytics.service.Exceptions.NotFoundException;
import com.analytics.service.dto.ProductVariantDto;
import com.analytics.service.entity.CartItem;
import com.analytics.service.entity.ProductVariant;
import com.analytics.service.entity.User;
import com.analytics.service.repository.CartItemRepository;
import com.analytics.service.repository.ProductVariantRepository;
import com.analytics.service.repository.UserMapper;
import com.analytics.service.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CartService {
    private final CartItemRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository repositoryProduct;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository repository;
    private final UserMapper mapper;
    private final UserService userService;
    private static final int RECOMMENDATIONS_LIMIT = 10;

    @Transactional
    public void addProductInCart(String loginToken, Long productId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Количество должно быть больше 0");
        }

        User user = userService.findUserByEmailOrPhone(loginToken);
        lockCart(user.getId());
        ProductVariant product = repositoryProduct.findByIdForUpdate(productId)
                .orElseThrow(() -> new NotFoundException("Товар не найден"));

        Optional<CartItem> existingOpt = cartRepository.findByUserIdAndVariantId(user.getId(), productId);

        int currentQty = existingOpt.map(CartItem::getQuantity).orElse(0);
        int total = currentQty + quantity;

        if (product.getQuantity() == null || product.getQuantity() < total) {
            throw new IllegalArgumentException("Количество товара нету в наличии");
        }

        if (existingOpt.isPresent()){
                CartItem existing = existingOpt.get();
                existing.setQuantity(total);
                cartRepository.save(existing);


        } else {
            CartItem cartItem = new CartItem();
            cartItem.setQuantity(quantity);
            cartItem.setUserId(user.getId());
            cartItem.setVariantId(productId);
            cartRepository.save(cartItem);
        }
    }

    @Transactional
    public void updateCartItemQuantity(Long cartId, String loginToken, Integer newQuantity){
        User user = userService.findUserByEmailOrPhone(loginToken);
        lockCart(user.getId());
        CartItem cartItem = cartRepository.findByIdAndUserId(cartId,user.getId()).orElseThrow(()->
                new NotFoundException("Позиция не найдена")
        );
        if(newQuantity == null || newQuantity < 1) throw new IllegalArgumentException("Количество должно быть больше 0");
        var product = repositoryProduct.findByIdForUpdate(cartItem.getVariantId()).orElseThrow(() -> new RuntimeException("Товар не найден"));
        if(product.getQuantity() == null || product.getQuantity() < newQuantity) throw new IllegalArgumentException("На складе недостаточно товара");
        cartItem.setQuantity(newQuantity);
    }

    public List<CartItem> getAllCartItemsUser(String loginToken){
        User user = userService.findUserByEmailOrPhone(loginToken);
        List<CartItem> listOfCartItems =  cartRepository.findByUserId(user.getId());
        return listOfCartItems;
    }
    @Transactional
    public void deleteCartItem(String loginToken,Long variantId){
        User user = userService.findUserByEmailOrPhone(loginToken);
        lockCart(user.getId());
        CartItem cartItem = cartRepository.findByUserIdAndVariantId(user.getId(),variantId).orElseThrow(()->
                     new NotFoundException("Товар не найден в корзине")
                );
        cartRepository.delete(cartItem);
    }

    @Transactional
    public void clearCart(String login){
        User user = userService.findUserByEmailOrPhone(login);
        lockCart(user.getId());
        cartRepository.deleteByUserId(user.getId());
    }


   @Transactional
    public List<ProductVariantDto> cartRecommendations(String loginToken) {
        User user = userService.findUserByEmailOrPhone(loginToken);

        List<CartItem> cart = cartItemRepository.findByUserId(user.getId());
        if (cart.isEmpty()) return List.of();

        Set<Long> inCart = cart.stream().map(CartItem::getVariantId).collect(Collectors.toSet());
        Map<Long, ProductVariant> result = new LinkedHashMap<>();   // без дублей, порядок сохраняется

        for (CartItem item : cart) {
            ProductVariant p = item.getProductVariant();
            Integer size = parseSize(p == null ? null : p.getSize());
            if (size == null) continue;                              // нечисловой размер — пропускаем

            String color = "K".equals(p.getProductId()) ? p.getColor() : null;  // для поясов — тот же цвет

            for (ProductVariant n : repository.findNearestSizes(p.getProductId(), size, color)) {
                if (!inCart.contains(n.getId())) result.putIfAbsent(n.getId(), n);
            }
            if (result.size() >= RECOMMENDATIONS_LIMIT) break;
        }

        return result.values().stream()
                .limit(RECOMMENDATIONS_LIMIT)
                .map(mapper::toProductVariantDto)
                .toList();
    }

    /** Один и тот же порядок: сначала пользователь, потом вариант. Иначе add и update взаимно блокируются. */
    private void lockCart(Long userId) {
        userRepository.lockById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private static Integer parseSize(String size) {
        if (size == null || !size.trim().matches("\\d+")) return null;
        return Integer.valueOf(size.trim());
    }
}
