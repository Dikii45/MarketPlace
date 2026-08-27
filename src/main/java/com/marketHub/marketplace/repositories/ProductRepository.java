package com.marketHub.marketplace.repositories;

import com.marketHub.marketplace.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByTitleContainingIgnoreCase(String title);

    List<Product> findByQuantityGreaterThanAndDeletedFalse(int quantity);

    List<Product> findByTitleContainingIgnoreCaseAndQuantityGreaterThanAndDeletedFalse(String title, int quantity);
}
