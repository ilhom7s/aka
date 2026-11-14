package com.analytics.service.controllers;


import com.analytics.service.dto.ProductCardDto;
import com.analytics.service.dto.ProductVariantDto;
import com.analytics.service.dto.VariantDto;
import com.analytics.service.dto.VariantRequestDto;
import com.analytics.service.repository.UserMapper;
import com.analytics.service.services.ProductService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/catalog")
public class ProductController {

    private final ProductService productService;
    private final UserMapper userMapper;

    @GetMapping("/k7")
    public List<ProductVariantDto> getAllK7() {
        return productService.allKimonoK7().stream().map(userMapper::toProductVariantDto).toList();
    }

    @GetMapping("/k1")
    public List<ProductVariantDto> getAllK1() {
        return productService.allKimonoK1().stream().map(userMapper::toProductVariantDto).toList();
    }

    @GetMapping("/k4")
    public List<ProductVariantDto> getAllK4() {
        return productService.allKimonoK4().stream().map(userMapper::toProductVariantDto).toList();
    }

    @GetMapping("/k5")
    public List<ProductVariantDto> getAllK5() {
        return productService.allKimonoK5().stream().map(userMapper::toProductVariantDto).toList();
    }

    @GetMapping("/k")
    public List<ProductVariantDto> getAllK(){
        return productService.allK().stream().map(userMapper::toProductVariantDto).toList();
    }

    // Карточка товара (категория = product_id)
    @GetMapping("/product/{productId}")
    public ProductCardDto card(@PathVariable String productId) {
        return productService.getProductCard(productId);
    }

    // Смена размера/цвета: вернуть конкретный вариант
    @PostMapping("/variant")
    public VariantDto variant(@RequestBody VariantRequestDto req) {
        return productService.getVariant(req.productId(), req.color(), req.size());
    }




    @GetMapping
    public List<ProductVariantDto> getAll() {
        return productService.allKimono().stream().map(userMapper::toProductVariantDto).toList();
    }
}

