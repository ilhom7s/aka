package com.analytics.service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Относительный URL, по которому фронт забирает картинку, например /images/uuid.jpg */
    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    /** Порядок фото в карточке (0 — обложка). */
    @Column(name = "position")
    private Integer position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ProductVariant productVariant;

    public ProductImage(String imageUrl, Integer position, ProductVariant productVariant) {
        this.imageUrl = imageUrl;
        this.position = position;
        this.productVariant = productVariant;
    }
}
