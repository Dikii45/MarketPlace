package com.marketHub.marketplace.controllers;

import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.models.enums.Role;
import com.marketHub.marketplace.repositories.ProductRepository;
import com.marketHub.marketplace.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Проверяет через реальный HTTP-слой (MockMvc + Spring Security) фикс
 * Broken Access Control на /product/delete/{id}: раньше удалить чужой товар
 * мог кто угодно, даже не залогинившись — см. §9 docs/architecture.md.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;

    private User persistUser(String email, boolean admin) {
        User u = new User();
        u.setEmail(email);
        u.setName(email);
        u.setPassword("x");
        u.setActive(true);
        if (admin) u.getRoles().add(Role.ROLE_ADMIN);
        return userRepository.save(u);
    }

    private Product persistProduct(User owner) {
        Product p = new Product();
        p.setUser(owner);
        p.setTitle("Товар");
        p.setPrice(100);
        p.setCity("Москва");
        p.setQuantity(5);
        return productRepository.save(p);
    }

    @Test
    void anonymousDelete_isRejectedAndProductSurvives() throws Exception {
        User owner = persistUser("owner@a.com", false);
        Product product = persistProduct(owner);

        mockMvc.perform(post("/product/delete/" + product.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(productRepository.findById(product.getId()).orElseThrow().isDeleted()).isFalse();
    }

    @Test
    @WithMockUser(username = "stranger@a.com")
    void loggedInButNotOwner_deleteIsRejected() throws Exception {
        User owner = persistUser("owner2@a.com", false);
        persistUser("stranger@a.com", false);
        Product product = persistProduct(owner);

        mockMvc.perform(post("/product/delete/" + product.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(productRepository.findById(product.getId()).orElseThrow().isDeleted()).isFalse();
    }

    @Test
    @WithMockUser(username = "owner3@a.com")
    void owner_canSoftDeleteOwnProduct() throws Exception {
        User owner = persistUser("owner3@a.com", false);
        Product product = persistProduct(owner);

        mockMvc.perform(post("/product/delete/" + product.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(productRepository.findById(product.getId()).orElseThrow().isDeleted()).isTrue();
    }

    @Test
    @WithMockUser(username = "admin@a.com")
    void admin_canDeleteAnyonesProduct() throws Exception {
        User owner = persistUser("owner4@a.com", false);
        persistUser("admin@a.com", true);
        Product product = persistProduct(owner);

        mockMvc.perform(post("/product/delete/" + product.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(productRepository.findById(product.getId()).orElseThrow().isDeleted()).isTrue();
    }
}
