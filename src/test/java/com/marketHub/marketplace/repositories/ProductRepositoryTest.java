package com.marketHub.marketplace.repositories;

import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.models.enums.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Sort;

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
        return product(owner, title, quantity, deleted, 100, null);
    }

    //создание товара с ценой и категорией — нужно для тестов фильтров/сортировки
    private Product product(User owner, String title, int quantity, boolean deleted, int price, Category category) {
        Product p = new Product();

        p.setUser(owner);
        p.setTitle(title);
        p.setPrice(price);
        p.setCity("Москва");
        p.setQuantity(quantity);
        p.setDeleted(deleted);
        p.setCategory(category);

        return productRepository.save(p);
    }


    //Тест удаленность товара
    @Test
    void search_excludesOutOfStockAndDeleted() {

        User seller = seller();
        Product inStock = product(seller, "In stock", 5, false);
        product(seller, "Out of stock", 0, false);
        product(seller, "Deleted but in stock", 5, true);

        //БД quantity > 0, а deleted == false
        List<Product> result = productRepository.search(null, null, null, null, Sort.unsorted());

        assertThat(result).containsExactly(inStock);
    }

    //проверка поиска
    @Test
    void search_byTitle_isCaseInsensitive() {
        User seller = seller();
        Product phone = product(seller, "Apple iPhone 17", 5, false);
        product(seller, "Samsung Galaxy", 5, false);

        List<Product> result = productRepository.search("iphone", null, null, null, Sort.unsorted());

        assertThat(result).containsExactly(phone);
    }

    //проверка поиска если товар удален
    @Test
    void search_byTitle_stillExcludesDeleted() {
        User seller = seller();
        product(seller, "iPhone deleted", 5, true);

        List<Product> result = productRepository.search("iphone", null, null, null, Sort.unsorted());

        assertThat(result).isEmpty();
    }

    //фильтр по категории
    @Test
    void search_byCategory_returnsOnlyMatchingCategory() {
        User seller = seller();
        Product phone = product(seller, "iPhone", 5, false, 500, Category.ELECTRONICS);
        product(seller, "Футболка", 5, false, 20, Category.CLOTHES);

        List<Product> result = productRepository.search(null, Category.ELECTRONICS, null, null, Sort.unsorted());

        assertThat(result).containsExactly(phone);
    }

    //фильтр по диапазону цены
    @Test
    void search_byPriceRange_excludesOutOfRange() {
        User seller = seller();
        Product mid = product(seller, "Средний", 5, false, 300, null);
        product(seller, "Дешёвый", 5, false, 50, null);
        product(seller, "Дорогой", 5, false, 900, null);

        List<Product> result = productRepository.search(null, null, 100, 500, Sort.unsorted());

        assertThat(result).containsExactly(mid);
    }

    //сортировка по цене по возрастанию
    @Test
    void search_sortByPriceAsc_ordersFromCheapestToMostExpensive() {
        User seller = seller();
        Product cheap = product(seller, "Дешёвый", 5, false, 50, null);
        Product mid = product(seller, "Средний", 5, false, 300, null);
        Product expensive = product(seller, "Дорогой", 5, false, 900, null);

        List<Product> result = productRepository.search(null, null, null, null, Sort.by(Sort.Direction.ASC, "price"));

        assertThat(result).containsExactly(cheap, mid, expensive);
    }
}
