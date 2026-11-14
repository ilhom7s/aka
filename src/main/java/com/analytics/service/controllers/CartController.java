package com.analytics.service.controllers;

import com.analytics.service.dto.CartItemDto;
import com.analytics.service.dto.ProductVariantDto;
import com.analytics.service.repository.UserMapper;
import com.analytics.service.services.CartService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
@AllArgsConstructor
public class CartController {
    private final CartService cartService;
    private final UserMapper mapper;
    @PostMapping("/add")
    public void addToCard(Authentication authentication, @RequestParam Long productId,@RequestParam Integer quantity){
        String login = authentication.getName();
        cartService.addProductInCart(login,productId,quantity);
    }
    @GetMapping("/all")
    public List<CartItemDto> allItems(Authentication authentication){
        String login = authentication.getName();
        var list = cartService.getAllCartItemsUser(login);
        return list.stream().map(mapper::toCartItemDto).toList();
    }
    @DeleteMapping
    public void deleteCartItem(Authentication authentication,@RequestParam Long productId){
        String login = authentication.getName();
        cartService.deleteCartItem(login,productId);
    }
    @PutMapping
    public void updateQuantity(Authentication authentication,@RequestParam Integer newQuantity,@RequestParam Long cartId){
        String login = authentication.getName();
        cartService.updateCartItemQuantity(cartId,login,newQuantity);
    }
    @DeleteMapping("allCart")
    public void clearAll(Authentication authentication){
    String login = authentication.getName();
        cartService.clearCart(login);
}
    @GetMapping("/recommendations")
    public List<ProductVariantDto> recommendations(Authentication authentication) {
        return cartService.cartRecommendations(authentication.getName());
    }

}
