package com.marketHub.marketplace.integration;

import com.marketHub.marketplace.models.Order;
import com.marketHub.marketplace.models.OrderItem;
import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.models.enums.OrderStatus;
import com.marketHub.marketplace.models.enums.PaymentMethod;
import com.marketHub.marketplace.repositories.OrderItemRepository;
import com.marketHub.marketplace.repositories.OrderRepository;
import com.marketHub.marketplace.repositories.ProductRepository;
import com.marketHub.marketplace.repositories.UserRepository;
import com.marketHub.marketplace.services.CartItemService;
import com.marketHub.marketplace.services.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Полный путь "живыми" бинами поверх H2: товар -> корзина -> checkout -> заказ.
 * В отличие от unit-тестов сервисов (моки), здесь реально сохраняются и
 * перечитываются строки из БД — это тот же сценарий, что описан в
 * docs/cart-checkout-tz.md и §3 docs/architecture.md.
 *
 * @Transactional на классе — каждый @Test откатывается в конце, тесты друг
 * другу не мешают, даже если делят одну in-memory базу testdb.
 */
@SpringBootTest
@Transactional
class CheckoutFlowIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private ProductService productService;
    @Autowired
    private CartItemService cartItemService;

    //создаем юзера
    private User persistUser(String email) {

        User u = new User();
        u.setEmail(email);
        u.setName(email);
        u.setPassword("x");
        u.setActive(true);

        return userRepository.save(u);
    }

    //создаем товар
    private Product persistProduct(User seller, String title, int price, int quantity) {

        Product p = new Product();
        p.setUser(seller);
        p.setTitle(title);
        p.setPrice(price);
        p.setCity("Москва");
        p.setQuantity(quantity);

        return productRepository.save(p);
    }


    private Principal principalOf(User user) {
        return user::getEmail;
    }

    //проверка работоспособности корзины
    @Test
    void fullCheckoutPipeline_reducesStockAndCreatesOrderWithOrderItem() {

        // продавец выставил товар с остатком 5
        User seller = persistUser("seller@a.com");
        User buyer = persistUser("buyer@a.com");
        Product product = persistProduct(seller, "Тестовый товар", 1500, 5);

        // покупатель кладёт 2 штуки в корзину
        cartItemService.addProductToCart(buyer, product);
        cartItemService.updateQuantity(buyer, product, 2);
        assertThat(cartItemService.getCartItems(buyer)).hasSize(1);

        // оформляет заказ
        boolean success = cartItemService.checkout(buyer, "г. Москва, ул. Тестовая, 1", PaymentMethod.CARD);
        assertThat(success).isTrue();

        // остаток уменьшился ровно на купленное количество
        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getQuantity()).isEqualTo(3);

        // создался Order на покупателя
        List<Order> orders = orderRepository.findByBuyerId(buyer.getId());
        assertThat(orders).hasSize(1);
        Order order = orders.get(0);
        assertThat(order.getDeliveryAddress()).isEqualTo("г. Москва, ул. Тестовая, 1");
        assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);


        // ровно один OrderItem с правильным количеством и стартовым статусом NEW
        List<OrderItem> sellerSales = orderItemRepository.findByProduct_User_Id(seller.getId());
        assertThat(sellerSales).hasSize(1);
        OrderItem orderItem = sellerSales.get(0);
        assertThat(orderItem.getOrder().getId()).isEqualTo(order.getId());
        assertThat(orderItem.getQuantity()).isEqualTo(2);
        assertThat(orderItem.getStatus()).isEqualTo(OrderStatus.NEW);

        // корзина покупателя опустела
        assertThat(cartItemService.getCartItems(buyer)).isEmpty();
    }

    //
    @Test
    void checkout_exactlyAvailableStock_isAllowed() {
        User seller = persistUser("seller2@a.com");
        User buyer = persistUser("buyer2@a.com");
        Product product = persistProduct(seller, "Дефицитный товар", 500, 1);

        cartItemService.addProductToCart(buyer, product); // addProductToCart сам ставит quantity=1

        boolean success = cartItemService.checkout(buyer, "Адрес", PaymentMethod.CASH);

        assertThat(success).isTrue(); // в корзине ровно 1 шт. при остатке 1 — заказ должен пройти
        assertThat(productRepository.findById(product.getId()).orElseThrow().getQuantity()).isZero();
    }

    //проверка если 2 заказ одновременно
    @Test
    void checkout_cartQuantityAboveStock_blockedWithNoOrderCreated() {
        User seller = persistUser("seller3@a.com");
        User buyer = persistUser("buyer3@a.com");
        Product product = persistProduct(seller, "Товар с маленьким остатком", 500, 1);
        Product other = persistProduct(seller, "Второй товар", 200, 10);

        cartItemService.addProductToCart(buyer, product);
        cartItemService.addProductToCart(buyer, other);
        cartItemService.updateQuantity(buyer, other, 3);

        // остаток первого товара кто-то скупил параллельно — эмулируем гонку,
        // выставляя остаток ниже того, что уже лежит в корзине
        product.setQuantity(0);
        productRepository.save(product);

        boolean success = cartItemService.checkout(buyer, "Адрес", PaymentMethod.CASH);

        assertThat(success).isFalse();
        assertThat(orderRepository.findByBuyerId(buyer.getId())).isEmpty();
        // второй товар тоже не должен быть списан — заказ либо весь, либо ничего
        Product otherReloaded = productRepository.findById(other.getId()).orElseThrow();
        assertThat(otherReloaded.getQuantity()).isEqualTo(10);
    }

    //проверка удаление товара, а у кого то в корзине
    @Test
    void checkout_productSoftDeletedWhileInCart_isSelfHealedAndBlocked() {
        User seller = persistUser("seller4@a.com");
        User buyer = persistUser("buyer4@a.com");
        Product product = persistProduct(seller, "Скоро удалённый товар", 300, 5);

        cartItemService.addProductToCart(buyer, product);
        assertThat(cartItemService.getCartItems(buyer)).hasSize(1);

        // продавец удаляет товар прямо во время того, как он лежит в чужой корзине
        boolean deleted = productService.deleteProducts(product.getId(), principalOf(seller));
        assertThat(deleted).isTrue();

        // при следующем обращении к корзине запись должна самоочиститься
        assertThat(cartItemService.getCartItems(buyer)).isEmpty();

        boolean success = cartItemService.checkout(buyer, "Адрес", PaymentMethod.CASH);

        assertThat(success).isFalse();
        assertThat(orderRepository.findByBuyerId(buyer.getId())).isEmpty();
    }

    //Проверка что 1 ордер = 1 заказ
    @Test
    void checkout_multiSellerCart_createsOneOrderWithOneOrderItemPerSeller() {
        User seller1 = persistUser("selA@a.com");
        User seller2 = persistUser("selB@a.com");
        User buyer = persistUser("buyer5@a.com");
        Product productA = persistProduct(seller1, "Товар A", 100, 5);
        Product productB = persistProduct(seller2, "Товар B", 200, 5);

        //корзина
        cartItemService.addProductToCart(buyer, productA);
        cartItemService.addProductToCart(buyer, productB);

        boolean success = cartItemService.checkout(buyer, "Адрес", PaymentMethod.CASH);


        assertThat(success).isTrue();
        List<Order> orders = orderRepository.findByBuyerId(buyer.getId());
        assertThat(orders).hasSize(1); // один Order на всю корзину

        List<OrderItem> salesA = orderItemRepository.findByProduct_User_Id(seller1.getId());
        List<OrderItem> salesB = orderItemRepository.findByProduct_User_Id(seller2.getId());
        assertThat(salesA).hasSize(1); // каждый продавец видит только свою строку
        assertThat(salesB).hasSize(1);
        assertThat(salesA.get(0).getOrder().getId()).isEqualTo(orders.get(0).getId());
        assertThat(salesB.get(0).getOrder().getId()).isEqualTo(orders.get(0).getId());
    }
}
