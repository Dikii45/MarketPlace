package com.marketHub.marketplace.repositories;

import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest поднимает только JPA-слой (Hibernate + H2), без веб-контекста —
 * быстрее полного @SpringBootTest и проверяет, что производные запросы
 * (derived queries) реально делают то, что подразумевает их имя.
 */
@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;

    //создание юзера
    private User seller() {

        User u = new User();
        u.setEmail("seller@a.com");
        u.setName("Seller");
        u.setPassword("x");
        u.setActive(true);

        return userRepository.save(u);
    }

    //создание товара
    private Product product(User owner, String title, int quantity, boolean deleted) {
        Product p = new Product();

        p.setUser(owner);
        p.setTitle(title);
        p.setPrice(100);
        p.setCity("Москва");
        p.setQuantity(quantity);
        p.setDeleted(deleted);

        return productRepository.save(p);
    }


    //Тест удаленность товара
    @Test
    void findByQuantityGreaterThanAndDeletedFalse_excludesOutOfStockAndDeleted() {

        User seller = seller();
        Product inStock = product(seller, "In stock", 5, false);
        product(seller, "Out of stock", 0, false);
        product(seller, "Deleted but in stock", 5, true);

        //БД quantity > 0, а deleted == false
        List<Product> result = productRepository.findByQuantityGreaterThanAndDeletedFalse(0);

        assertThat(result).containsExactly(inStock);
    }

    //проверка поиска
    @Test
    void findByTitleContainingIgnoreCaseAndQuantityGreaterThanAndDeletedFalse_isCaseInsensitive() {
        User seller = seller();
        Product phone = product(seller, "Apple iPhone 17", 5, false);
        product(seller, "Samsung Galaxy", 5, false);

        List<Product> result = productRepository
                .findByTitleContainingIgnoreCaseAndQuantityGreaterThanAndDeletedFalse("iphone", 0);

        assertThat(result).containsExactly(phone);
    }

    //проверка поиска если товар удален
    @Test
    void findByTitleContainingIgnoreCaseAndQuantityGreaterThanAndDeletedFalse_stillExcludesDeleted() {
        User seller = seller();
        product(seller, "iPhone deleted", 5, true);

        List<Product> result = productRepository
                .findByTitleContainingIgnoreCaseAndQuantityGreaterThanAndDeletedFalse("iphone", 0);

        assertThat(result).isEmpty();
    }
}
