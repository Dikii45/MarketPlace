package com.marketHub.marketplace.services;

import com.marketHub.marketplace.models.CartItem;
import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.models.enums.PaymentMethod;
import com.marketHub.marketplace.repositories.CartItemRepository;
import com.marketHub.marketplace.repositories.OrderItemRepository;
import com.marketHub.marketplace.repositories.OrderRepository;
import com.marketHub.marketplace.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты CartItemService на моках. checkout() — самая важная бизнес-логика
 * в проекте ("либо заказываем ровно то количество, либо не заказываем вообще"),
 * поэтому ей уделено больше всего внимания.
 */
@ExtendWith(MockitoExtension.class)
class CartItemServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private ProductRepository productRepository;

    private CartItemService cartItemService;

    @BeforeEach
    void setUp() {
        cartItemService = new CartItemService(cartItemRepository, orderRepository, orderItemRepository, productRepository);
    }

    private User user(long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private Product product(long id, int quantity, boolean deleted) {
        Product p = new Product();
        p.setId(id);
        p.setQuantity(quantity);
        p.setPrice(100);
        p.setDeleted(deleted);
        return p;
    }

    private CartItem cartItem(User user, Product product, int quantity) {
        CartItem ci = new CartItem();
        ci.setUser(user);
        ci.setProduct(product);
        ci.setQuantity(quantity);
        return ci;
    }

    // ---------- addProductToCart ----------

    @Test
    void addProductToCart_newProduct_createsItemWithQuantityOne() {
        User user = user(1);
        Product product = product(10, 5, false);
        when(cartItemRepository.findByUserAndProduct(user, product)).thenReturn(Optional.empty());

        cartItemService.addProductToCart(user, product);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantity()).isEqualTo(1);
    }

    @Test
    void addProductToCart_outOfStock_doesNothing() {
        User user = user(1);
        Product product = product(10, 0, false);

        cartItemService.addProductToCart(user, product);

        verify(cartItemRepository, never()).save(any());
        verify(cartItemRepository, never()).findByUserAndProduct(any(), any());
    }

    @Test
    void addProductToCart_alreadyInCartBelowStock_incrementsQuantity() {
        User user = user(1);
        Product product = product(10, 5, false);
        CartItem existing = cartItem(user, product, 2);
        when(cartItemRepository.findByUserAndProduct(user, product)).thenReturn(Optional.of(existing));

        cartItemService.addProductToCart(user, product);

        assertThat(existing.getQuantity()).isEqualTo(3);
        verify(cartItemRepository).save(existing);
    }

    @Test
    void addProductToCart_alreadyAtStockLimit_doesNotExceedStock() {
        User user = user(1);
        Product product = product(10, 2, false);
        CartItem existing = cartItem(user, product, 2);
        when(cartItemRepository.findByUserAndProduct(user, product)).thenReturn(Optional.of(existing));

        cartItemService.addProductToCart(user, product);

        assertThat(existing.getQuantity()).isEqualTo(2);
        verify(cartItemRepository, never()).save(any());
    }

    // ---------- updateQuantity ----------

    @Test
    void updateQuantity_clampsToAvailableStock() {
        User user = user(1);
        Product product = product(10, 3, false);
        CartItem existing = cartItem(user, product, 1);
        when(cartItemRepository.findByUserAndProduct(user, product)).thenReturn(Optional.of(existing));

        cartItemService.updateQuantity(user, product, 999);

        assertThat(existing.getQuantity()).isEqualTo(3);
    }

    @Test
    void updateQuantity_clampsBelowOneUpToOne() {
        User user = user(1);
        Product product = product(10, 3, false);
        CartItem existing = cartItem(user, product, 2);
        when(cartItemRepository.findByUserAndProduct(user, product)).thenReturn(Optional.of(existing));

        cartItemService.updateQuantity(user, product, -5);

        assertThat(existing.getQuantity()).isEqualTo(1);
    }

    // ---------- getCartItems: самоочистка от удалённых товаров ----------

    @Test
    void getCartItems_purgesRowsPointingAtDeletedProducts() {
        User user = user(1);
        Product live = product(1, 5, false);
        Product deleted = product(2, 0, true);
        CartItem liveItem = cartItem(user, live, 1);
        CartItem deletedItem = cartItem(user, deleted, 1);
        when(cartItemRepository.findAllByUser(user)).thenReturn(List.of(liveItem, deletedItem));

        List<CartItem> result = cartItemService.getCartItems(user);

        assertThat(result).containsExactly(liveItem);
        verify(cartItemRepository).deleteAll(List.of(deletedItem));
    }

    @Test
    void getCartItems_nothingDeleted_doesNotTouchRepositoryForDeletion() {
        User user = user(1);
        CartItem liveItem = cartItem(user, product(1, 5, false), 1);
        when(cartItemRepository.findAllByUser(user)).thenReturn(List.of(liveItem));

        cartItemService.getCartItems(user);

        verify(cartItemRepository, never()).deleteAll(any());
    }

    // ---------- checkout: главный бизнес-сценарий ----------

    @Test
    void checkout_emptyCart_returnsFalseAndCreatesNothing() {
        User user = user(1);
        when(cartItemRepository.findAllByUser(user)).thenReturn(List.of());

        boolean result = cartItemService.checkout(user, "Адрес", PaymentMethod.CASH);

        assertThat(result).isFalse();
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_insufficientStock_rejectsWholeOrderWithoutSideEffects() {
        User user = user(1);
        Product product = product(10, 2, false);
        CartItem item = cartItem(user, product, 5); // хотим 5, на складе только 2
        when(cartItemRepository.findAllByUser(user)).thenReturn(List.of(item));

        boolean result = cartItemService.checkout(user, "Адрес", PaymentMethod.CASH);

        assertThat(result).isFalse();
        assertThat(product.getQuantity()).isEqualTo(2); // остаток не тронут
        verify(orderRepository, never()).save(any());
        verify(orderItemRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteAllByUser(any());
    }

    @Test
    void checkout_enoughStock_createsOrderAndOrderItemsAndDecrementsStockAndClearsCart() {
        User user = user(1);
        Product product = product(10, 5, false);
        CartItem item = cartItem(user, product, 2);
        when(cartItemRepository.findAllByUser(user)).thenReturn(List.of(item));

        boolean result = cartItemService.checkout(user, "г. Москва", PaymentMethod.CARD);

        assertThat(result).isTrue();
        assertThat(product.getQuantity()).isEqualTo(3); // 5 - 2
        verify(orderRepository).save(any());
        verify(orderItemRepository).save(any());
        verify(productRepository).save(product);
        verify(cartItemRepository).deleteAllByUser(user);
    }

    @Test
    void checkout_exactlyAvailableStock_isAllowed() {
        User user = user(1);
        Product product = product(10, 2, false);
        CartItem item = cartItem(user, product, 2); // ровно весь остаток
        when(cartItemRepository.findAllByUser(user)).thenReturn(List.of(item));

        boolean result = cartItemService.checkout(user, "Адрес", PaymentMethod.CASH);

        assertThat(result).isTrue();
        assertThat(product.getQuantity()).isZero();
    }
}
