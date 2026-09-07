package com.marketHub.marketplace.controllers.rest;

import com.marketHub.marketplace.dto.ProductDto;
import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;

// REST-версия каталога товаров поверх того же ProductService, что и обычный сайт —
// одна и та же бизнес-логика (владелец/админ, soft-delete, остаток) для веба и API
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductRestController {

    private final ProductService productService;

    // начальная страница, выгрузи все товары
    @GetMapping
    public List<ProductDto> list(@RequestParam(required = false) String title) {
        return productService.listProducts(title).stream().map(ProductDto::from).toList();
    }

    //сам продукт
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> get(@PathVariable Long id) {

        Product product = productService.getProductById(id);

        if (product == null || product.isDeleted()) {
            //вернуть ошибку 404
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ProductDto.from(product));
    }


    /*
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(Product product,
                                     @RequestParam("files") List<MultipartFile> files,
                                     @RequestParam int quantity,
                                     Principal principal) throws IOException {
        if (files.size() > 10) {
            return ResponseEntity.badRequest().body(Map.of("error", "Можно загрузить максимум 10 файлов"));
        }
        productService.saveProduct(principal, product, files, quantity);
        return ResponseEntity.status(201).body(ProductDto.from(product));
    }
*/


    //удаление товара по id
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Principal principal) {

        if (!productService.deleteProducts(id, principal)) {
            return ResponseEntity.status(403).body(Map.of("error", "Нет доступа или товар не найден"));
        }
        return ResponseEntity.noContent().build();
    }


    //пополнение товара
    @PatchMapping("/{id}/restock")
    public ResponseEntity<?> restock(@PathVariable Long id, @RequestParam int amount, Principal principal) {

        if (!productService.restockProduct(id, amount, principal)) {
            return ResponseEntity.status(403).body(Map.of("error", "Нет доступа или неверное количество"));
        }
        return ResponseEntity.ok(ProductDto.from(productService.getProductById(id)));
    }
}
