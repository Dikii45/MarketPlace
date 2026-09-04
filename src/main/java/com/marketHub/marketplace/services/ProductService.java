package com.marketHub.marketplace.services;

import com.marketHub.marketplace.models.Image;
import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.repositories.ProductRepository;
import com.marketHub.marketplace.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public List<Product> listProducts(String title) {
        if (title != null && !title.isBlank()) return productRepository.findByTitleContainingIgnoreCaseAndQuantityGreaterThanAndDeletedFalse(title.trim(), 0);
        return productRepository.findByQuantityGreaterThanAndDeletedFalse(0);
    }


    //пополняем запасы продукта
    public boolean restockProduct(Long productId, int amount, Principal principal) {
        if (amount <= 0) return false;

        Product product = getProductById(productId);
        if (product == null) return false;

        User user = getUserByPrincipal(principal);
        if (user == null || user.getId() == null) return false;
        if (!user.isAdmin() && !product.getUser().getId().equals(user.getId())) return false;


        product.setQuantity(product.getQuantity() + amount);
        productRepository.save(product);
        return true;
    }

    public void saveProduct(Principal principal, Product product, List<MultipartFile> files, int quantity) throws IOException {
        product.setUser(getUserByPrincipal(principal));
        product.setQuantity(quantity);

        //из MultipartFile в Image
            for(MultipartFile file : files){
                Image image;
                image = toImageEntity(file);
                product.addImageToProduct(image);
            }


        log.info("Saving new Product.Title: {}; Author: {}", product.getTitle(),principal.getName());

        // должны сохранить что бы получить id картинки
        Product productFromDb = productRepository.save(product);
        productFromDb.setPreviewImageId(productFromDb.getImages().get(0).getId());

        productRepository.save(product);
    }

    public User getUserByPrincipal(Principal principal) {
        if(principal == null) return new User();
        return userRepository.findByEmail(principal.getName());
    }

    private Image toImageEntity(MultipartFile file) throws IOException {
        Image image = new Image();
        image.setName(file.getName());
        image.setOriginalFileName(file.getOriginalFilename());
        image.setContentType(file.getContentType());
        image.setSize(file.getSize());
        image.setBytes(file.getBytes());
        return image;
    }

    public boolean deleteProducts(Long id, Principal principal) {
        Product product = getProductById(id);
        if (product == null) return false;

        User user = getUserByPrincipal(principal);
        if (user == null || user.getId() == null) return false;
        if (!user.isAdmin() && !product.getUser().getId().equals(user.getId())) return false;

        log.info("Delete {}", id);
        // ометка что товар удален
        product.setQuantity(0);
        product.setDeleted(true);
        productRepository.save(product);
        return true;
    }

    public Product getProductById(Long id) {
        return  productRepository.findById(id).orElse(null);
    }
}
