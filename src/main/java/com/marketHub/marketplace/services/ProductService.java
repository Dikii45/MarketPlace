package com.marketHub.marketplace.services;

import com.marketHub.marketplace.models.Image;
import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.models.enums.Category;
import com.marketHub.marketplace.repositories.ProductRepository;
import com.marketHub.marketplace.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
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

    private void validateProduct(Product product, List<MultipartFile> files){

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Файлы не загружены");
        }

        if (files.size() > 10) {
            throw new IllegalArgumentException("Можно загрузить максимум 10 файлов");
        }

        if(product.getTitle() == null || product.getTitle().isBlank()){
            throw new IllegalArgumentException("Должен быть заполнен заголовок");
        }
        product.setTitle(product.getTitle().trim());

        if(product.getDescription() == null || product.getDescription().isBlank()){
            throw new IllegalArgumentException("Должен быть заполнено описание");
        }
        product.setDescription(product.getDescription().trim());

        if(product.getPrice() <= 0){
            throw new IllegalArgumentException("Цена должна быть больше 0");
        }

        //В будущем возможно будет enum
        if(product.getCity() == null || product.getCity().isBlank()){
            throw new IllegalArgumentException("Должен быть заполнен город");
        }
        product.setCity(product.getCity().trim());

        if(product.getQuantity() <= 0){
            throw new IllegalArgumentException("Количество должно быть больше 0");
        }
        if(product.getCategory() == null){
            throw new IllegalArgumentException("Выберите категорию товара");
        }

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



    // каталог: поиск по названию + необязательные фильтры по категории/цене + сортировка по цене
    public List<Product> listProducts(String title, Category category, Integer minPrice, Integer maxPrice, String sort) {
        String normalizedTitle = (title != null && !title.isBlank()) ? title.trim() : null;

        Sort sortOrder = switch (sort == null ? "" : sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            default -> Sort.unsorted();
        };

        return productRepository.search(normalizedTitle, category, minPrice, maxPrice, sortOrder);
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

    public void saveProduct(Principal principal, Product product, List<MultipartFile> files) throws IOException {

        validateProduct(product, files);

        product.setUser(getUserByPrincipal(principal));

        //из MultipartFile в Image
            for(MultipartFile file : files){
                Image image;
                image = toImageEntity(file);
                product.addImageToProduct(image);
            }

        // должны сохранить что бы получить id картинки
        Product productFromDb = productRepository.save(product);
        productFromDb.setPreviewImageId(productFromDb.getImages().get(0).getId());

        productRepository.save(product);
    }

    public User getUserByPrincipal(Principal principal) {
        if(principal == null) return new User();
        return userRepository.findByEmail(principal.getName());
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
