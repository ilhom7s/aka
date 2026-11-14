//package com.analytics.service.services;
//
//import com.analytics.service.dto.CreateOrderRequest;
//import com.analytics.service.dto.OrderQuoteDto;
//import com.analytics.service.dto.RequestItems;
//import com.analytics.service.dto.YooPayment;
//import com.analytics.service.entity.*;
//import com.analytics.service.repository.CartItemRepository;
//import com.analytics.service.repository.OrderRepository;
//import com.analytics.service.repository.ProductVariantRepository;
//import com.analytics.service.repository.UserRepository;
//import lombok.AllArgsConstructor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.*;
//
///**
// * Все операции с заказом, которым нужна транзакция.
// * Вынесено в отдельный бин: вызов @Transactional-метода через this обходит прокси Spring,
// * и транзакция молча не открывается. Здесь нет ни одного HTTP-вызова — только БД.
// */
//@Service
//@AllArgsConstructor
//public class OrderTxService {
//
//    private static final Logger log = LoggerFactory.getLogger(OrderTxService.class);
//
//    /** Бесплатная доставка от этой суммы, иначе — стоимость доставки. */
//    public static final BigDecimal FREE_SHIPPING_FROM = new BigDecimal("5000");
//    public static final BigDecimal SHIPPING_PRICE = new BigDecimal("490");
//
//    private final ProductVariantRepository variantRepository;
//    private final CartItemRepository cartItemRepository;
//    private final UserRepository userRepository;
//    private final OrderRepository orderRepository;
//
//    // ======================= расчёт суммы =======================
//
//    /** Посчитать сумму заказа по ценам из БД — фронт только показывает результат. */
//    @Transactional(readOnly = true)
//    public OrderQuoteDto quote(List<RequestItems> list) {
//        BigDecimal goods = BigDecimal.ZERO;
//        for (Map.Entry<Long, Integer> e : groupItems(list).entrySet()) {
//            ProductVariant v = variantRepository.findById(e.getKey())
//                    .orElseThrow(() -> new RuntimeException("Товар не найден"));
//            goods = goods.add(v.getPrice().multiply(BigDecimal.valueOf(e.getValue())));
//        }
//        BigDecimal shipping = shippingFor(goods);
//        return new OrderQuoteDto(goods, shipping, goods.add(shipping));
//    }
//
//    // ======================= создание =======================
//
//    /**
//     * Создать заказ и зарезервировать товар (остаток уменьшается сразу).
//     * Резерв убирает oversell: два покупателя не смогут оплатить одну последнюю единицу.
//     */
//    @Transactional
//    public Order createOrder(CreateOrderRequest request, String login) {
//        User user = findUser(login);
//
//        if (request.address() == null || request.address().isBlank())
//            throw new IllegalArgumentException("Укажите адрес доставки");
//        if (request.phone() == null || request.phone().isBlank())
//            throw new IllegalArgumentException("Укажите телефон");
//
//        Map<Long, Integer> grouped = groupItems(request.list());
//
//        Order order = new Order();
//        order.setUserId(user.getId());
//        order.setAddress(request.address().trim());
//        order.setPhone(request.phone().trim());
//        order.setCreatedAt(LocalDateTime.now());
//        order.setStatus(OrderStatus.PENDING_PAYMENT);
//
//        Set<OrderItems> items = new HashSet<>();
//        BigDecimal goods = BigDecimal.ZERO;
//
//        // блокируем варианты в порядке id — одинаковый порядок у всех потоков исключает взаимную блокировку
//        for (Map.Entry<Long, Integer> e : grouped.entrySet()) {
//            ProductVariant v = variantRepository.findByIdForUpdate(e.getKey())
//                    .orElseThrow(() -> new RuntimeException("Товар не найден"));
//            int qty = e.getValue();
//
//            if (Boolean.FALSE.equals(v.getActive()))
//                throw new IllegalArgumentException("Товар снят с продажи: " + v.getProductName());
//            if (v.getQuantity() == null || v.getQuantity() < qty)
//                throw new IllegalArgumentException("Недостаточно товара: " + v.getProductName());
//
//            v.setQuantity(v.getQuantity() - qty);                 // резерв
//
//            OrderItems item = new OrderItems();
//            item.setOrder(order);
//            item.setProductVariant(v);
//            item.setQuantity(qty);
//            item.setAmount(v.getPrice());                         // цена из БД, не от клиента
//            items.add(item);
//
//            goods = goods.add(v.getPrice().multiply(BigDecimal.valueOf(qty)));
//        }
//
//        order.setOrderItems(items);
//        order.setTotalAmount(goods.add(shippingFor(goods)));      // доставка входит в сумму оплаты
//        return orderRepository.save(order);
//    }
//
//    @Transactional
//    public void savePaymentId(Long orderId, String paymentId) {
//        Order order = orderRepository.findByIdForUpdate(orderId)
//                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
//        order.setPaymentId(paymentId);
//    }
//
//    // ======================= оплата / отмена =======================
//
//    /**
//     * Провести успешный платёж. Все данные берутся из ответа ЮKassa, а не из webhook.
//     * Идемпотентно и без гонок: строка заказа заблокирована на время обработки.
//     */
//    @Transactional
//    public void applyPayment(YooPayment payment) {
//        if (payment.orderId() == null)
//            throw new IllegalStateException("Платёж " + payment.id() + " не привязан к заказу");
//
//        Order order = orderRepository.findByIdForUpdate(payment.orderId())
//                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
//
//        if (!payment.id().equals(order.getPaymentId()))
//            throw new IllegalStateException("Платёж " + payment.id() + " не относится к заказу " + order.getId());
//        if (!payment.isSucceeded())
//            throw new IllegalStateException("Платёж не подтверждён: " + payment.status());
//        if (payment.amount() == null || payment.amount().compareTo(order.getTotalAmount()) != 0)
//            throw new IllegalStateException("Сумма платежа не совпадает с заказом " + order.getId());
//
//        if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.DELIVERED) {
//            return;                                                // уже обработан
//        }
//
//        if (order.getStatus() == OrderStatus.CANCELLED) {
//            // деньги пришли после отмены: резерв уже возвращён — забираем товар снова
//            log.warn("Оплата пришла по отменённому заказу {} — повторно резервируем товар", order.getId());
//            for (OrderItems item : sortedItems(order)) {
//                ProductVariant v = variantRepository.findByIdForUpdate(item.getProductVariant().getId()).orElseThrow();
//                int left = (v.getQuantity() == null ? 0 : v.getQuantity()) - item.getQuantity();
//                if (left < 0) log.error("Заказ {}: товара {} не хватает, нужна ручная обработка", order.getId(), v.getId());
//                v.setQuantity(Math.max(0, left));
//            }
//        }
//
//        order.setStatus(OrderStatus.PAID);
//
//        // из корзины убираем только оплаченные позиции
//        for (OrderItems item : order.getOrderItems()) {
//            cartItemRepository.deleteByUserIdAndVariantId(order.getUserId(), item.getProductVariant().getId());
//        }
//        log.info("Заказ {} оплачен (платёж {})", order.getId(), payment.id());
//    }
//
//    /**
//     * Отменить неоплаченный заказ и вернуть резерв.
//     * @param paymentId если передан — отменяем только когда он совпадает с платежом заказа
//     */
//    @Transactional
//    public void cancel(Long orderId, String paymentId) {
//        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
//        if (order == null || order.getStatus() != OrderStatus.PENDING_PAYMENT) return;
//        if (paymentId != null && !paymentId.equals(order.getPaymentId())) return;
//
//        for (OrderItems item : sortedItems(order)) {
//            variantRepository.findByIdForUpdate(item.getProductVariant().getId()).ifPresent(v ->
//                    v.setQuantity((v.getQuantity() == null ? 0 : v.getQuantity()) + item.getQuantity()));
//        }
//        order.setStatus(OrderStatus.CANCELLED);
//        log.info("Заказ {} отменён, резерв возвращён", orderId);
//    }
//
//    // ======================= чтение =======================
//
//    /** Заказ текущего пользователя. Чужой → 403. */
//    @Transactional(readOnly = true)
//    public Order getOwnedOrder(Long orderId, String login) {
//        User user = findUser(login);
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
//        if (!order.getUserId().equals(user.getId()))
//            throw new AccessDeniedException("Это не ваш заказ");
//        return order;
//    }
//
//    @Transactional(readOnly = true)
//    public OrderStatus getStatus(Long orderId) {
//        return orderRepository.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("Заказ не найден"))
//                .getStatus();
//    }
//
//    @Transactional(readOnly = true)
//    public List<Order> findStuck(LocalDateTime before) {
//        return orderRepository.findStuckPayments(before);
//    }
//
//    @Transactional(readOnly = true)
//    public List<Order> findAbandoned(LocalDateTime before) {
//        return orderRepository.findAbandoned(before);
//    }
//
//    // ======================= helpers =======================
//
//    /** Склеить повторы одного варианта и проверить количества. TreeMap — стабильный порядок блокировок. */
//    private Map<Long, Integer> groupItems(List<RequestItems> list) {
//        if (list == null || list.isEmpty()) throw new IllegalArgumentException("Список товаров пуст");
//        Map<Long, Integer> grouped = new TreeMap<>();
//        for (RequestItems it : list) {
//            if (it.variantId() == null) throw new IllegalArgumentException("Не указан товар");
//            if (it.quantity() == null || it.quantity() < 1) throw new IllegalArgumentException("Некорректное количество");
//            grouped.merge(it.variantId(), it.quantity(), Integer::sum);
//        }
//        return grouped;
//    }
//
//    private BigDecimal shippingFor(BigDecimal goods) {
//        return goods.compareTo(FREE_SHIPPING_FROM) >= 0 ? BigDecimal.ZERO : SHIPPING_PRICE;
//    }
//
//    private List<OrderItems> sortedItems(Order order) {
//        List<OrderItems> list = new ArrayList<>(order.getOrderItems());
//        list.sort(Comparator.comparing(i -> i.getProductVariant().getId()));
//        return list;
//    }
//
//    private User findUser(String login) {
//        return userRepository.findByEmail(login)
//                .or(() -> userRepository.findByUserPhone(login))
//                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
//    }
//}
