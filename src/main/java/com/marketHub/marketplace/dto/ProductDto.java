package com.marketHub.marketplace.dto;

import com.marketHub.marketplace.models.Product;

// плоское представление Product для JSON — без ленивых JPA-связей и пароля продавца,
// которые попали бы в ответ при сериализации самой сущности напрямую
public record ProductDto(
        Long id,
        String title,
        String description,
        int price,
        String city,
        int quantity,
        boolean deleted,
        Long previewImageId,
        Long sellerId,
        String sellerName
) {


    public static ProductDto from(Product product) {

        return new ProductDto(
                product.getId(),
                product.getTitle(),
                product.getDescription(),
                product.getPrice(),
                product.getCity(),
                product.getQuantity(),
                product.isDeleted(),
                product.getPreviewImageId(),
                product.getUser().getId(),
                product.getUser().getName()
        );
    }
}
