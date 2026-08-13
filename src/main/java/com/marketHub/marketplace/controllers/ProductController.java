package com.marketHub.marketplace.controllers;

import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // нулевой старт сайта
    @GetMapping("/")
    public String products(@RequestParam(name = "title", required = false) String title, Model model, Principal principal) {
        model.addAttribute("products", productService.listProducts(title));
        model.addAttribute("currentUser", productService.getUserByPrincipal(principal));
        return "products";
    }

    @GetMapping("/product/{id}")
    public String productInfo(@PathVariable Long id, Model model, Principal principal){
        model.addAttribute("product", productService.getProductById(id));
        model.addAttribute("currentUser", productService.getUserByPrincipal(principal));
        return "product-info";
    }


    // кнопка создать
    @PostMapping("/product/create")
    public String createProduct(Product product, @RequestParam("file1") MultipartFile file1, @RequestParam("file2") MultipartFile file2, @RequestParam("file3") MultipartFile file3, Principal principal) throws IOException {
        productService.saveProduct(principal, product, file1, file2, file3);
        // выкинуть на главную
        return "redirect:/";
    }


    //кнопка удалить
    @PostMapping("/product/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteProducts(id);
        //выкинуть на главную
        return "redirect:/";
    }

}
