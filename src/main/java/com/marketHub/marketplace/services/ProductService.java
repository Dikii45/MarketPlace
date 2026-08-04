package com.marketHub.marketplace.services;

import com.marketHub.marketplace.models.Product;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {
    private List<Product> products = new ArrayList<>();
    {
        products.add(new Product(
                "PlayStation 5",
                "Simple description",
                67000,
                "Krasnoyarsk",
                "tomas"));

        products.add(new Product(
                "PlayStation 5",
                "Simple description",
                67000,
                "Krasnoyarsk",
                "tomas"));
    }
}
