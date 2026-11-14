package com.analytics.service.repository;

import com.analytics.service.dto.*;
import com.analytics.service.entity.*;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    UserDto toDto(User user);

    ProductVariantDto toProductVariantDto(ProductVariant productVariant);

    ProductVariant toProductVariantEntity(ProductRequestDto requestDto);

    ProductVariant toEntity(ProductVariantDto productVariantDto);

    ProductImageDto toProductImageDto(ProductImage productImage);

    OrderItems toOrderItemsEntity(OrderItemsDto orderItemsDto);

    OrderItemsDto toOrderItemsDto(OrderItems orderItems);

    Order toOrderEntity(OrderDto orderDto);

    @AfterMapping
    default void linkOrderItems(@MappingTarget Order order) {
        order.getOrderItems().forEach(orderItem -> orderItem.setOrder(order));
    }

    @Mapping(target = "userEmail", source = "user.email")
    OrderDto toOrderDto(Order order);


    @AfterMapping
    default void linkImages(@MappingTarget ProductVariant productVariant) {
        productVariant.getImages().forEach(image -> image.setProductVariant(productVariant));
    }

    CartItem toEntity(CartItemDto cartItemDto);

    CartItemDto toCartItemDto(CartItem cartItem);
}
