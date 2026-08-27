package com.marketHub.marketplace.repositories;

import com.marketHub.marketplace.models.CartItem;

import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findAllByUser(User user);

    Optional<CartItem> findByUserAndProduct(User user, Product product);

    void deleteByUserAndProduct(User user, Product product);

    void deleteAllByUser(User user);
}
