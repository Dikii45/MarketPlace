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


 //* либо заказываем ровно то количество, либо не заказываем вообще

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

    //создание юзера
    private User user(long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    //создание продукта
    private Product product(long id, int quantity, boolean deleted) {
        Product p = new Product();
        p.setId(id);
        p.setQuantity(quantity);
        p.setPrice(100);
        p.setDeleted(deleted);
        return p;
    }

    //заполнение корзины
    private CartItem cartItem(User user, Product product, int quantity) {
        CartItem ci = new CartItem();
        ci.setUser(user);
        ci.setProduct(product);
        ci.setQuantity(quantity);
        return ci;
    }



// ---------- Проверка addProductToCart ----------


    //смотрю как отрабатывает addProductToCart
    @Test
    void addProductToCart_newProduct_createsItemWithQuantityOne() {
        User user = user(1);
        Product product = product(10, 5, false);

        //когда выполняем findByUserAndProduct верни пустой объект
        when(cartItemRepository.findByUserAndProduct(user, product)).thenReturn(Optional.empty());

        cartItemService.addProductToCart(user, product);

        //ловлю объект который был передан в save и смотрю что передалось
        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantity()).isEqualTo(1);
    }


    //смотрю как реагирует addProductToCart, если товара 0
    @Test
    void addProductToCart_outOfStock_doesNothing() {
        User user = user(1);
        Product product = product(10, 0, false);

        cartItemService.addProductToCart(user, product);

        //проверяю что save и findByUserAndProduct не вызвались
        verify(cartItemRepository, never()).save(any());
        verify(cartItemRepository, never()).findByUserAndProduct(any(), any());
    }


    //Проверяю как реагирует addProductToCart если в корзине уже 2
    @Test
    void addProductToCart_alreadyInCartBelowStock_incrementsQuantity() {
        User user = user(1);
        Product product = product(10, 5, false);
        CartItem existing = cartItem(user, product, 2);

        //когда будешь выполнять findByUserAndProduct верни existing
        when(cartItemRepository.findByUserAndProduct(user, product)).thenReturn(Optional.of(existing));

        cartItemService.addProductToCart(user, product);

        assertThat(existing.getQuantity()).isEqualTo(3);

        //проверяю что вообще save вызвался
        verify(cartItemRepository).save(existing);
    }



    //Проверяю как реагирует addProductToCart, если товара на складе 2, а покупатель еще добавляет его в корзину
    @Test
    void addProductToCart_alreadyAtStockLimit_doesNotExceedStock() {
        User user = user(1);
        Product product = product(10, 2, false);
        CartItem existing = cartItem(user, product, 2);

        //когда будешь вызвать findByUserAndProduct верни existing
        when(cartItemRepository.findByUserAndProduct(user, product)).thenReturn(Optional.of(existing));

        cartItemService.addProductToCart(user, product);

        //Проверяю что товаров в корзине не изменилось
        assertThat(existing.getQuantity()).isEqualTo(2);
        verify(cartItemRepository, never()).save(any());
    }


    

    // ---------- Проверка updateQuantity ----------

    //Проверяю как реагирует updateQuantity, если попробовать добавить больше товара чем есть на складе
    @Test
    void updateQuantity_clampsToAvailableStock() {
        User user = user(1);
        Product product = product(10, 3, false);
        CartItem existing = cartItem(user, product, 1);

        //когда будешь выполнять findByUserAndProduct, верни existing
        when(cartItemRepository.findByUserAndProduct(user, product)).thenReturn(Optional.of(existing));

        cartItemService.updateQuantity(user, product, 999);

        // должно остаться 3
        assertThat(existing.getQuantity()).isEqualTo(3);
    }


    //Проверяю как реагирует updateQuantity, если убрать из корзины отризацтельное число
    @Test
    void updateQuantity_clampsBelowOneUpToOne() {
        User user = user(1);
        Product product = product(10, 3, false);
        CartItem existing = cartItem(user, product, 2);

        //когда будешь выполнять findByUserAndProduct переедай туда existing
        when(cartItemRepository.findByUserAndProduct(user, product)).thenReturn(Optional.of(existing));

        cartItemService.updateQuantity(user, product, -5);

        //должно остаться 1 т.к. меньше 1 товара не может иначе нужно удалить товар из корзины
        assertThat(existing.getQuantity()).isEqualTo(1);
    }





    // ---------- getCartItems: самоочистка от удалённых товаров ----------


    //как реагирует getCartItems если в корзине есть удаленный товар
    @Test
    void getCartItems_purgesRowsPointingAtDeletedProducts() {
        User user = user(1);
        Product live = product(1, 5, false);
        Product deleted = product(2, 0, true);

        CartItem liveItem = cartItem(user, live, 1);
        CartItem deletedItem = cartItem(user, deleted, 1);

        //когда будешь выполять findAllByUser верни List где 2 СartItem
        when(cartItemRepository.findAllByUser(user)).thenReturn(List.of(liveItem, deletedItem));

        List<CartItem> result = cartItemService.getCartItems(user);

        // Должен вернуть только liveItem
        assertThat(result).containsExactly(liveItem);

        //Проверяю что deletedItem был удален из корзины
        verify(cartItemRepository).deleteAll(List.of(deletedItem));
    }


    // ---------- Проверка checkout----------

    //Как реагирует checkout если у корзины нет user
    @Test
    void checkout_emptyCart_returnsFalseAndCreatesNothing() {
        User user = user(1);

        //Когда вызовишь findAllByUser верни пустоту
        when(cartItemRepository.findAllByUser(user)).thenReturn(List.of());

        boolean result = cartItemService.checkout(user, "Адрес", PaymentMethod.CASH);

        //Результат должен быть false
        assertThat(result).isFalse();
        verify(orderRepository, never()).save(any());
    }


    // как реагирует checkout, если родовец покупает больше чем есть на складе
    @Test
    void checkout_insufficientStock_rejectsWholeOrderWithoutSideEffects() {
        User user = user(1);
        Product product = product(10, 2, false);
        CartItem item = cartItem(user, product, 5); // хотим 5, на складе только 2

        //Когда вызовишь findAllByUser, верни item
        when(cartItemRepository.findAllByUser(user)).thenReturn(List.of(item));

        boolean result = cartItemService.checkout(user, "Адрес", PaymentMethod.CASH);

        // результат должен быть false
        assertThat(result).isFalse();
        assertThat(product.getQuantity()).isEqualTo(2); // остаток не тронут
        verify(orderRepository, never()).save(any());
        verify(orderItemRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteAllByUser(any());
    }


    //как реагирует checkout, если все идет по плану
    @Test
    void checkout_enoughStock_createsOrderAndOrderItemsAndDecrementsStockAndClearsCart() {
        User user = user(1);
        Product product = product(10, 5, false);
        CartItem item = cartItem(user, product, 2);

        //когда вызовишь findAllByUser, верни item
        when(cartItemRepository.findAllByUser(user)).thenReturn(List.of(item));

        boolean result = cartItemService.checkout(user, "г. Москва", PaymentMethod.CARD);

        assertThat(result).isTrue();
        assertThat(product.getQuantity()).isEqualTo(3); // 5 - 2
        verify(orderRepository).save(any());
        verify(orderItemRepository).save(any());
        verify(productRepository).save(product);
        verify(cartItemRepository).deleteAllByUser(user);
    }


    //как реагирует checkout если мы покупаем все остатки
    @Test
    void checkout_exactlyAvailableStock_isAllowed() {
        User user = user(1);
        Product product = product(10, 2, false);
        CartItem item = cartItem(user, product, 2); // ровно весь остаток

        //когда вызовишь findAllByUser, верни item
        when(cartItemRepository.findAllByUser(user)).thenReturn(List.of(item));

        boolean result = cartItemService.checkout(user, "Адрес", PaymentMethod.CASH);

        assertThat(result).isTrue();
        assertThat(product.getQuantity()).isZero();
    }
}
