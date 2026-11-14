package com.analytics.service.services;

import com.analytics.service.Exceptions.NotFoundException;
import com.analytics.service.dto.ProductCardDto;
import com.analytics.service.entity.ProductImage;
import com.analytics.service.entity.ProductVariant;
import com.analytics.service.dto.VariantDto;
import com.analytics.service.dto.ProductCardShortDto;
import com.analytics.service.dto.ProductImageDto;
import com.analytics.service.repository.CartItemRepository;
import com.analytics.service.repository.ProductVariantRepository;
import com.analytics.service.repository.UserMapper;
import com.analytics.service.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductVariantRepository productRepository;
    private final UserMapper mapper;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;


    public List<ProductVariant> allKimonoK7() { return productRepository.findActiveByCategory("K7"); }
    public List<ProductVariant> allKimonoK1() { return productRepository.findActiveByCategory("K1"); }
    public List<ProductVariant> allKimonoK4() { return productRepository.findActiveByCategory("K4"); }
    public List<ProductVariant> allKimonoK5() { return productRepository.findActiveByCategory("K5"); }

    public List<ProductVariant> allKimono()   { return productRepository.findAllGrouped(); }
    public List<ProductVariant> allK(){
        return  productRepository.findAllK();
    }


    public ProductCardDto getProductCard(String productId){
        List<ProductVariant> variants = productRepository.findActiveByCategory(productId);
        if (variants.isEmpty()) throw new RuntimeException("Товары не найден");

        ProductVariant base = variants.get(0);

        List<String> sizes = variants.stream()
                .map(ProductVariant::getSize).filter(Objects::nonNull).distinct()
                .sorted(Comparator.comparingInt(ProductService::sizeKey))
                .toList();

        List<String> colors = variants.stream()
                .map(ProductVariant::getColor).filter(Objects::nonNull).distinct()
                .toList();

        BigDecimal priceFrom = variants.stream()
                .map(ProductVariant::getPrice).filter(Objects::nonNull)
                .min(Comparator.naturalOrder()).orElse(base.getPrice());

        List<VariantDto> variantDto = variants.stream().map(ProductService::toVariantDto).toList();

        return new ProductCardDto(
                base.getProductName(), productId, base.getDescription(),
                base.getMaterial(), base.getPlotnost(), priceFrom,
                sizes, colors, coverImages(base), variantDto);
    }

    // ===== Конкретный вариант (смена размера/цвета) =====
    public VariantDto getVariant(String productId, String color, String size) {
        ProductVariant v = productRepository.findVariant(productId, color, size).orElseThrow(() -> new NotFoundException("Такого варианта нет"));
        return toVariantDto(v);
    }

    // ===== Похожие товары (другие категории) =====
    public List<ProductCardShortDto> getSimilar(String productId) {
        Map<String, List<ProductVariant>> byCategory = productRepository.findAllActive().stream()
                .filter(v -> v.getProductId() != null && !v.getProductId().equals(productId) && !v.getProductId().equals("K"))
                .collect(Collectors.groupingBy(ProductVariant::getProductId, LinkedHashMap::new, Collectors.toList()));

        return byCategory.entrySet().stream().map(e -> {
            List<ProductVariant> vs = e.getValue();
            ProductVariant b = vs.get(0);
            BigDecimal from = vs.stream().map(ProductVariant::getPrice).filter(Objects::nonNull)
                    .min(Comparator.naturalOrder()).orElse(b.getPrice());
            return new ProductCardShortDto(b.getProductName(), e.getKey(), from, coverImages(b));
        }).toList();
    }

    // ===== helpers =====
    private static VariantDto toVariantDto(ProductVariant v) {
        boolean available = v.getQuantity() != null && v.getQuantity() > 0;
        return new VariantDto(v.getId(), v.getSize(), v.getColor(), v.getPrice(),
                v.getImageUrl(), toImageDtos(v.getImages()), available);
    }

    private static int sizeKey(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return Integer.MAX_VALUE; }
    }

    private static List<ProductImageDto> toImageDtos(List<ProductImage> imgs) {
        if (imgs == null) return List.of();
        return imgs.stream()
                .map(im -> new ProductImageDto(im.getId(), im.getImageUrl(), im.getPosition()))
                .toList();
    }

    private static List<ProductImageDto> coverImages(ProductVariant v) {
        List<ProductImageDto> imgs = toImageDtos(v.getImages());
        if (imgs.isEmpty() && v.getImageUrl() != null) {
            return List.of(new ProductImageDto(null, v.getImageUrl(), 0));
        }
        return imgs;
    }



}
