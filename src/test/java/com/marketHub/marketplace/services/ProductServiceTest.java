package com.marketHub.marketplace.services;

import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.models.enums.Role;
import com.marketHub.marketplace.repositories.ProductRepository;
import com.marketHub.marketplace.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты ProductService на моках — без Spring-контекста и без БД.
 * Проверяем именно бизнес-правила: кто может удалить/пополнить товар,
 * и что удаление — soft-delete, а не физическое.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, userRepository);
    }

    private User user(long id, String email, boolean admin) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        if (admin) u.getRoles().add(Role.ROLE_ADMIN);
        return u;
    }

    private Product product(long id, User owner, int quantity) {
        Product p = new Product();
        p.setId(id);
        p.setUser(owner);
        p.setQuantity(quantity);
        p.setDeleted(false);
        return p;
    }

    private Principal principalOf(String email) {
        return () -> email;
    }

    // ---------- listProducts ----------

    @Test
    void listProducts_blankTitle_returnsInStockNonDeletedProducts() {
        List<Product> expected = List.of(product(1, user(1, "a@a.com", false), 5));
        when(productRepository.findByQuantityGreaterThanAndDeletedFalse(0)).thenReturn(expected);

        List<Product> result = productService.listProducts("  ");

        assertThat(result).isEqualTo(expected);
        verify(productRepository, never()).findByTitleContainingIgnoreCaseAndQuantityGreaterThanAndDeletedFalse(any(), anyInt());
    }

    @Test
    void listProducts_withTitle_delegatesToSearchQuery() {
        when(productRepository.findByTitleContainingIgnoreCaseAndQuantityGreaterThanAndDeletedFalse("phone", 0))
                .thenReturn(List.of());

        productService.listProducts("  phone  ");

        verify(productRepository).findByTitleContainingIgnoreCaseAndQuantityGreaterThanAndDeletedFalse("phone", 0);
    }

    // ---------- deleteProducts (см. security-фикс: только владелец/админ) ----------

    @Test
    void deleteProducts_owner_softDeletesAndZeroesQuantity() {
        User owner = user(1, "owner@a.com", false);
        Product product = product(10, owner, 5);
        when(productRepository.findById(10L)).thenReturn(java.util.Optional.of(product));
        when(userRepository.findByEmail("owner@a.com")).thenReturn(owner);

        boolean result = productService.deleteProducts(10L, principalOf("owner@a.com"));

        assertThat(result).isTrue();
        assertThat(product.isDeleted()).isTrue();
        assertThat(product.getQuantity()).isZero();
        verify(productRepository).save(product);
    }

    @Test
    void deleteProducts_admin_canDeleteAnyonesProduct() {
        User owner = user(1, "owner@a.com", false);
        User admin = user(2, "admin@a.com", true);
        Product product = product(10, owner, 5);
        when(productRepository.findById(10L)).thenReturn(java.util.Optional.of(product));
        when(userRepository.findByEmail("admin@a.com")).thenReturn(admin);

        boolean result = productService.deleteProducts(10L, principalOf("admin@a.com"));

        assertThat(result).isTrue();
        assertThat(product.isDeleted()).isTrue();
    }

    @Test
    void deleteProducts_notOwnerNotAdmin_rejectedAndProductUntouched() {
        User owner = user(1, "owner@a.com", false);
        User stranger = user(2, "stranger@a.com", false);
        Product product = product(10, owner, 5);
        when(productRepository.findById(10L)).thenReturn(java.util.Optional.of(product));
        when(userRepository.findByEmail("stranger@a.com")).thenReturn(stranger);

        boolean result = productService.deleteProducts(10L, principalOf("stranger@a.com"));

        assertThat(result).isFalse();
        assertThat(product.isDeleted()).isFalse();
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProducts_productDoesNotExist_returnsFalse() {
        when(productRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        boolean result = productService.deleteProducts(999L, principalOf("someone@a.com"));

        assertThat(result).isFalse();
        verify(productRepository, never()).save(any());
    }

    // ---------- restockProduct ----------

    @Test
    void restockProduct_owner_increasesQuantityByAmount() {
        User owner = user(1, "owner@a.com", false);
        Product product = product(10, owner, 5);
        when(productRepository.findById(10L)).thenReturn(java.util.Optional.of(product));
        when(userRepository.findByEmail("owner@a.com")).thenReturn(owner);

        boolean result = productService.restockProduct(10L, 3, principalOf("owner@a.com"));

        assertThat(result).isTrue();
        assertThat(product.getQuantity()).isEqualTo(8);
    }

    @Test
    void restockProduct_nonPositiveAmount_rejectedWithoutTouchingRepository() {
        boolean result = productService.restockProduct(10L, 0, principalOf("owner@a.com"));

        assertThat(result).isFalse();
        verify(productRepository, never()).findById(any());
    }

    @Test
    void restockProduct_notOwner_rejectedAndQuantityUnchanged() {
        User owner = user(1, "owner@a.com", false);
        User stranger = user(2, "stranger@a.com", false);
        Product product = product(10, owner, 5);
        when(productRepository.findById(10L)).thenReturn(java.util.Optional.of(product));
        when(userRepository.findByEmail("stranger@a.com")).thenReturn(stranger);

        boolean result = productService.restockProduct(10L, 3, principalOf("stranger@a.com"));

        assertThat(result).isFalse();
        assertThat(product.getQuantity()).isEqualTo(5);
        verify(productRepository, never()).save(any());
    }
}
