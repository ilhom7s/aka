package com.analytics.service.services;



import com.analytics.service.Exceptions.NotFoundException;
import com.analytics.service.dto.ProductRequestDto;
import com.analytics.service.entity.*;
import com.analytics.service.repository.OrderItemsRepository;
import com.analytics.service.repository.OrderRepository;
import com.analytics.service.repository.ProductVariantRepository;
import com.analytics.service.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class AdminService {

    public static final int MIN_IMAGES = 3;
    public static final int MAX_IMAGES = 5;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderRepository orderRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final ImageStorageService imageStorageService;


    public ProductVariant findProductById(Long id){
        return productVariantRepository.findById(id).orElseThrow(()-> {
            log.warn("Товар не нашелся выброшено исключения");
            return new NotFoundException(String.format("Товар с id = {} не найден", id));
        });
    }
    @Transactional
    public void changeProductPrice(Long id, BigDecimal price){
            log.debug("Изменить цену товара с id = {}",id);
            ProductVariant product  = findProductById(id);
            product.setPrice(price);
            log.info("Продукт с id = {} изменена цена на {}",id,price);
    }

    @Transactional
    public void changeQuantity(Long id,Integer newQuantity){
        if(newQuantity<0){
            throw new IllegalArgumentException("Количество не может быть отрицательным");
        }
        log.debug("Изменение количество товара id = {}",id);
        ProductVariant product  = findProductById(id);
        product.setQuantity(newQuantity);
    }

    public List<User> getAllUsers() {
        log.debug("Вывести всех пользователей для админа");
        log.info("Выведено все пользователи");
        return userRepository.findAll();
    }

    public List<ProductVariant> getAllProducts() {
        log.debug("Вывести все товары для админа");
        log.info("Все продукты выведены для админа");
        return productVariantRepository.findAllGrouped();
    }


    @Transactional
    public ProductVariant createProduct(ProductRequestDto newProductRequest, List<MultipartFile> images) {
        validateImageCount(images);

        ProductVariant product = new ProductVariant();
        product.setProductId(newProductRequest.productId());
        product.setProductName(newProductRequest.productName());
        product.setDescription(newProductRequest.description());
        product.setColor(newProductRequest.color());
        product.setSize(newProductRequest.size());
        product.setPrice(newProductRequest.price());
        product.setQuantity(newProductRequest.quantity());
        product.setPlotnost(newProductRequest.plotnost());
        product.setMaterial(newProductRequest.material());

        attachImages(product, images);
        return productVariantRepository.save(product);
    }

    @Transactional
    public ProductVariant editProduct(Long id, ProductRequestDto dto, List<MultipartFile> images) {
        ProductVariant foundedProduct = findProductById(id);

        foundedProduct.setProductId(dto.productId());
        foundedProduct.setProductName(dto.productName());
        foundedProduct.setDescription(dto.description());
        foundedProduct.setColor(dto.color());
        foundedProduct.setSize(dto.size());
        foundedProduct.setPrice(dto.price());
        foundedProduct.setQuantity(dto.quantity());
        foundedProduct.setPlotnost(dto.plotnost());
        foundedProduct.setMaterial(dto.material());

        if (images != null && !images.isEmpty()) {
            validateImageCount(images);
            foundedProduct.getImages().forEach(img -> imageStorageService.delete(img.getImageUrl()));
            foundedProduct.clearImages();
            attachImages(foundedProduct, images);
        }
        return productVariantRepository.save(foundedProduct);
    }

    @Transactional
    public String deleteProduct(Long id) {
        ProductVariant foundedProduct = findProductById(id);
        if (orderItemsRepository.existsByVariantId(id)) {
            foundedProduct.setActive(false);
            productVariantRepository.save(foundedProduct);
            return "Товар архивирован: он участвует в заказах и удалён из каталога, но остаётся в истории";
        }
        foundedProduct.getImages().forEach(img -> imageStorageService.delete(img.getImageUrl()));
        productVariantRepository.delete(foundedProduct);
        return "Товар удалён";
    }
    @Transactional
    public ProductVariant updateQuantity(Long id, Integer quantity) {
        if(quantity<0){
            throw new IllegalArgumentException("Количество не может быть ниже нуля");
        }
        log.debug("Изменение количество товара с id = {}",id);
        ProductVariant foundedProduct = findProductById(id);
        foundedProduct.setQuantity(quantity);
        log.info("Товар с id = {} обновлен на новое количество  = {}",id,quantity);
        return productVariantRepository.save(foundedProduct);
    }


    // ==================== ЗАКАЗЫ ====================

    public List<Order> getDeliveredOrders() {
        return orderRepository.findAllDeliveredOrders();
    }

    public List<Order> getUndeliveredOrders() {
        return orderRepository.findAllNotDeliveredOrders();
    }

    public List<Order> allOrders() {
        return orderRepository.findAllWithItems();
    }

    @Transactional
    public Order changeOrderStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new NotFoundException("Заказ не найден"));
        order.setStatus(status);
        return orderRepository.save(order);
    }

    // ==================== helpers ====================

    private void validateImageCount(List<MultipartFile> images) {
        int count = (images == null) ? 0 : (int) images.stream().filter(f -> f != null && !f.isEmpty()).count();
        if (count < MIN_IMAGES || count > MAX_IMAGES) {
            throw new IllegalArgumentException(
                    "У товара должно быть от " + MIN_IMAGES + " до " + MAX_IMAGES + " фото, а пришло: " + count);
        }
    }

    private void attachImages(ProductVariant product, List<MultipartFile> images) {
        List<String> urls = imageStorageService.storeAll(
                images.stream().filter(f -> f != null && !f.isEmpty()).toList());
        for (int i = 0; i < urls.size(); i++) {
            product.addImage(new ProductImage(urls.get(i), i, product));
        }
        product.setImageUrl(urls.get(0));
    }

    public void imageBeltCount(List<MultipartFile> images) {
        int count = (images == null) ? 0 : (int) images.stream().filter(f -> f != null && !f.isEmpty()).count();
        if (count < MIN_IMAGES || count > MAX_IMAGES) {
            throw new IllegalArgumentException(
                    "У пояса должно быть от " + MIN_IMAGES + " до " + MAX_IMAGES + " фото, а пришло: " + count);
        }
    }

    public List<Order> findUserOrdesByEmailOrPhone(String phoneOrEmail) {
        User user = userRepository.findByEmail(phoneOrEmail).or(()->userRepository.findByUserPhone(phoneOrEmail)).orElseThrow(()->new RuntimeException("Пользователь не найден"));
        return orderRepository.findUserOrders(user.getId());
    }

   }
