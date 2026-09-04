package com.marketHub.marketplace.repositories;

import com.marketHub.marketplace.models.CartItem;
import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CartItemRepositoryTest {

    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;

    //создание юзера
    private User user(String email) {
        User u = new User();
        u.setEmail(email);
        u.setName(email);
        u.setPassword("x");
        u.setActive(true);
        return userRepository.save(u);
    }

    //создание продукта
    private Product product(User owner, String title) {
        Product p = new Product();
        p.setUser(owner);
        p.setTitle(title);
        p.setPrice(100);
        p.setCity("Москва");
        p.setQuantity(5);
        return productRepository.save(p);
    }

    //проверка что корзина нормально работает.
    @Test
    void findAllByUser_returnsOnlyThatUsersCartItems() {
        User seller = user("seller@a.com");
        User buyer1 = user("buyer1@a.com");
        User buyer2 = user("buyer2@a.com");
        Product product = product(seller, "Товар");

        //корзин
        CartItem item1 = new CartItem();
        item1.setUser(buyer1);
        item1.setProduct(product);
        item1.setQuantity(1);
        cartItemRepository.save(item1);

        CartItem item2 = new CartItem();
        item2.setUser(buyer2);
        item2.setProduct(product);
        item2.setQuantity(2);
        cartItemRepository.save(item2);

        //БД
        List<CartItem> result = cartItemRepository.findAllByUser(buyer1);

        //проверка
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser()).isEqualTo(buyer1);
    }


    //проверка что из корзины удаляются товары
    @Test
    void deleteByUserAndProduct_removesOnlyTheMatchingRow() {
        User seller = user("seller@a.com");
        User buyer = user("buyer@a.com");
        Product productA = product(seller, "A");
        Product productB = product(seller, "B");

        //корзина
        CartItem itemA = new CartItem();
        itemA.setUser(buyer);
        itemA.setProduct(productA);
        itemA.setQuantity(1);
        cartItemRepository.save(itemA);

        //корзина
        CartItem itemB = new CartItem();
        itemB.setUser(buyer);
        itemB.setProduct(productB);
        itemB.setQuantity(1);
        cartItemRepository.save(itemB);

        //БД
        cartItemRepository.deleteByUserAndProduct(buyer, productA);


        List<CartItem> remaining = cartItemRepository.findAllByUser(buyer);
        assertThat(remaining).extracting(CartItem::getProduct).containsExactly(productB);
    }
}
