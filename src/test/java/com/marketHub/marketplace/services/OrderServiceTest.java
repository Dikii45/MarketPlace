package com.marketHub.marketplace.services;

import com.marketHub.marketplace.models.OrderItem;
import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.models.enums.OrderStatus;
import com.marketHub.marketplace.repositories.OrderItemRepository;
import com.marketHub.marketplace.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты конечного автомата статусов OrderItem (OrderService.updateStatus).
 * Правила переходов должны совпадать 1-в-1 с тем, что скрыто disabled-опциями
 * в orders.ftlh — если тут тест упадёт после правки switch в сервисе,
 * значит забыли поправить и шаблон.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private UserService userService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderItemRepository, userService);
    }

    // создание юзера
    private User user(long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private Principal principalOf(String email) {
        return () -> email;
    }

    // updateStatus нормально реагирует на изменения статуса(главная идея нельзя присваивать предыдущий статус)
    @ParameterizedTest(name = "{0} -> {1} разрешён: {2}")
    @CsvSource({
            "NEW,       CONFIRMED, true",
            "NEW,       CANCELLED, true",
            "NEW,       NEW,       false",
            "CONFIRMED, SENT,      true",
            "CONFIRMED, CANCELLED, true",
            "CONFIRMED, NEW,       false",
            "CONFIRMED, CONFIRMED, false",
            "SENT,      RECEIVED,  true",
            "SENT,      CANCELLED, true",
            "SENT,      CONFIRMED, false",
            "RECEIVED,  CANCELLED, true",
            "RECEIVED,  SENT,      false",
            "CANCELLED, CONFIRMED, false",
            "CANCELLED, RECEIVED,  false",
    })
    void updateStatus_transitionMatrix(OrderStatus from, OrderStatus to, boolean allowed) {
        User seller = user(1);
        Product product = new Product();
        product.setUser(seller);
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProduct(product);
        item.setStatus(from);

        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(userService.getUserByPrincipal(any())).thenReturn(seller);

        orderService.updateStatus(1L, to, principalOf("seller@a.com"));

        if (allowed) {
            assertThat(item.getStatus()).isEqualTo(to);
        } else {
            assertThat(item.getStatus()).isEqualTo(from);
        }
    }

    //как реагирует updateStatus если не владелец пытается поменять статус
    @Test
    void updateStatus_notTheSellerOfThisItem_statusUnchanged() {
        User seller = user(1);
        User stranger = user(2);
        Product product = new Product();
        product.setUser(seller);
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setProduct(product);
        item.setStatus(OrderStatus.NEW);

        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(userService.getUserByPrincipal(any())).thenReturn(stranger);

        orderService.updateStatus(1L, OrderStatus.CONFIRMED, principalOf("stranger@a.com"));

        assertThat(item.getStatus()).isEqualTo(OrderStatus.NEW);
        verify(orderItemRepository, never()).save(any());
    }

    //как реагирует updateStatus если не находит user
    @Test
    void updateStatus_orderItemNotFound_doesNothingSilently() {
        when(orderItemRepository.findById(999L)).thenReturn(Optional.empty());

        orderService.updateStatus(999L, OrderStatus.CONFIRMED, principalOf("someone@a.com"));

        verify(orderItemRepository, never()).save(any());
    }
}
