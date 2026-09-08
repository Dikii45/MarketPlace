package com.marketHub.marketplace.seed;

import com.marketHub.marketplace.models.Image;
import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.models.enums.Category;
import com.marketHub.marketplace.models.enums.Role;
import com.marketHub.marketplace.repositories.ProductRepository;
import com.marketHub.marketplace.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// Отдельный бин с транзакционными операциями сидирования — вынесен из DemoDataSeeder,
// т.к. self-invocation внутри одного бина обходит Spring-прокси и ломает @Transactional.
@Service
@RequiredArgsConstructor
public class SeedTx {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public List<User> ensureSellers() {
        List<User> sellers = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            String email = "seller" + i + "@gmail.com";
            User existing = userRepository.findByEmail(email);
            if (existing != null) {
                sellers.add(existing);
                continue;
            }
            User u = new User();
            u.setEmail(email);
            u.setName("Продавец " + i);
            u.setPassword(passwordEncoder.encode("Seller123!"));
            u.setActive(true);
            u.getRoles().add(Role.ROLE_USER);
            sellers.add(userRepository.save(u));
        }
        return sellers;
    }

    @Transactional
    public void createProduct(String title, String description, Category category, int price, int quantity,
                               String city, Long sellerId, List<byte[]> photos) {
        Product product = new Product();
        product.setTitle(title);
        product.setDescription(description);
        product.setPrice(price);
        product.setQuantity(quantity);
        product.setCity(city);
        product.setCategory(category);
        product.setUser(userRepository.getReferenceById(sellerId));

        for (int photoIdx = 1; photoIdx <= photos.size(); photoIdx++) {
            byte[] bytes = photos.get(photoIdx - 1);
            Image image = new Image();
            image.setName("photo" + photoIdx + ".jpg");
            image.setOriginalFileName(title + " " + photoIdx + ".jpg");
            image.setContentType("image/jpeg");
            image.setSize((long) bytes.length);
            image.setBytes(bytes);
            image.setPreviewImage(photoIdx == 1);
            product.addImageToProduct(image);
        }

        Product saved = productRepository.save(product);
        saved.setPreviewImageId(saved.getImages().get(0).getId());
        productRepository.save(saved);
    }
}
