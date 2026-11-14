package com.analytics.service.controllers;

import com.analytics.service.dto.*;

import com.analytics.service.entity.OrderStatus;
import com.analytics.service.entity.ProductVariant;
import com.analytics.service.repository.UserMapper;
import com.analytics.service.services.AdminService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;

@RestController
@RequestMapping("/admin")
@AllArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserMapper mapper;

    @GetMapping("/users")
    public List<UserDto> all() {
        return adminService.getAllUsers().stream().map(mapper::toDto).toList();
    }

    @PatchMapping("/price")
    public void updatePrice(@RequestBody PriceUpdate update){
            adminService.changeProductPrice(update.id(),update.price());
    }
    @PatchMapping("/quantity")
    public void updateQuantity(@RequestBody QuantityUpdate update){
        adminService.changeQuantity(update.id(),update.quantity());
    }

    // ==================== ТОВАРЫ ====================

    @GetMapping("/allgoods")
    public List<ProductVariantDto> allGoods() {
        return adminService.getAllProducts().stream().map(mapper::toProductVariantDto).toList();
    }

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductVariantDto create(@RequestPart("product") ProductRequestDto product,
                                    @RequestPart("images") List<MultipartFile> images) {
        ProductVariant savedProduct = adminService.createProduct(product, images);
        return mapper.toProductVariantDto(savedProduct);
    }

    @PutMapping(value = "/products/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductVariantDto edit(@PathVariable Long id,
                                  @RequestPart("product") ProductRequestDto product,
                                  @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        ProductVariant savedProduct = adminService.editProduct(id, product, images);
        return mapper.toProductVariantDto(savedProduct);
    }

    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        return adminService.deleteProduct(id);
    }

    @PatchMapping("/update/{id}")
    public ProductVariantDto update(@PathVariable Long id, @RequestParam Integer quantity) {
        return mapper.toProductVariantDto(adminService.updateQuantity(id, quantity));
    }


    @GetMapping("/delivered")
    public List<OrderDto> delivered() {
        return adminService.getDeliveredOrders().stream().map(mapper::toOrderDto).toList();
    }

    @GetMapping("/orders")
    public List<OrderDto> undelivered() {
        return adminService.getUndeliveredOrders().stream().map(mapper::toOrderDto).toList();
    }

    @GetMapping("/all")
    public List<OrderDto> allorders() {
        return adminService.allOrders().stream().map(mapper::toOrderDto).toList();
    }

    @PatchMapping("/orders/{id}/status")
    public OrderDto changeStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        return mapper.toOrderDto(adminService.changeOrderStatus(id, status));
    }
    @GetMapping("/user-orders")
    public List<OrderDto> userOrders(@RequestParam String phoneOrEmail){
        return adminService.findUserOrdesByEmailOrPhone(phoneOrEmail).stream().map(mapper::toOrderDto).toList();
    }
}





