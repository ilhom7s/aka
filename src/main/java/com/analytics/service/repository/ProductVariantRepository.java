package com.analytics.service.repository;

import com.analytics.service.entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /* Пояса — отдельная категория product_id = 'K'. */

    @Query("SELECT p FROM ProductVariant p  WHERE p.productId = 'K' AND (p.active IS NULL OR p.active = true)")
    List<ProductVariant> findAllK();
    /* Витрина "Все" — сгруппировано по product_id/size/color/price (как было). */
    @Query(value = "SELECT MAX(id) as id, product_id, size, color, SUM(quantity) as quantity, price, " +
            "MAX(product_name) as product_name, MAX(image_url) as image_url, MAX(description) as description, " +
            "MAX(plotnost) as plotnost, MAX(material) as material, bool_or(active) as active " +
            "FROM product_variants WHERE active IS DISTINCT FROM false " +
            "GROUP BY product_id, size, color, price", nativeQuery = true)

    List<ProductVariant> findAllGrouped();

    /* Все варианты категории. active видим, если true ИЛИ null (старые строки). */
    @Query("SELECT p FROM ProductVariant p WHERE p.productId = :productId AND (p.active IS NULL OR p.active = true)")
    List<ProductVariant> findActiveByCategory(@Param("productId") String productId);

    /* Все активные варианты (для похожих товаров). */
    @Query("SELECT p FROM ProductVariant p WHERE (p.active IS NULL OR p.active = true)")
    List<ProductVariant> findAllActive();

    /* Конкретный вариант: категория + цвет + размер. */
    @Query("SELECT p FROM ProductVariant p WHERE p.productId = :productId AND p.color = :color " +
            "AND p.size = :size AND (p.active IS NULL OR p.active = true)")
    Optional<ProductVariant> findVariant(@Param("productId") String productId,
                                         @Param("color") String color,
                                         @Param("size") String size);

//    /* Поиск. */
//    @Query(value = "SELECT * FROM product_variants WHERE " +
//            "LOWER(product_name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
//            "LOWER(color) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
//            "LOWER(size) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
//            "LOWER(product_id) LIKE LOWER(CONCAT('%', :query, '%'))", nativeQuery = true)
//    List<ProductVariant> searchProducts(@Param("query") String query);

    /** Вариант с блокировкой строки — для резерва/возврата остатков без гонок. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductVariant p WHERE p.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);
    /**
     * Ближайший меньший и ближайший больший размер того же товара.
     * Пришло 180 → вернёт, например, 175 и 185; если 175 нет — вернёт 170.
     * Нечисловые размеры (S, M, L) пропускаются, а не роняют запрос.
     */
    @Query(value = """
        SELECT t.* FROM (
            (SELECT * FROM product_variants
             WHERE product_id = :productId
               AND (active IS NULL OR active = true)
               AND quantity > 0
               AND (CAST(:color AS varchar) IS NULL OR color = :color)
               AND CASE WHEN size ~ '^[0-9]+$' THEN CAST(size AS INTEGER) END < :size
             ORDER BY CASE WHEN size ~ '^[0-9]+$' THEN CAST(size AS INTEGER) END DESC
             LIMIT 1)
            UNION ALL
            (SELECT * FROM product_variants
             WHERE product_id = :productId
               AND (active IS NULL OR active = true)
               AND quantity > 0
               AND (CAST(:color AS varchar) IS NULL OR color = :color)
               AND CASE WHEN size ~ '^[0-9]+$' THEN CAST(size AS INTEGER) END > :size
             ORDER BY CASE WHEN size ~ '^[0-9]+$' THEN CAST(size AS INTEGER) END ASC
             LIMIT 1)
        ) t
        ORDER BY CAST(t.size AS INTEGER)
        """, nativeQuery = true)
    List<ProductVariant> findNearestSizes(@Param("productId") String productId,
                                          @Param("size") Integer size,
                                          @Param("color") String color);

}
