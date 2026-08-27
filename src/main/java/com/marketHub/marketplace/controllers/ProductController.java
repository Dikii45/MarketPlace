package com.marketHub.marketplace.controllers;

import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // нулевой старт сайта, title -> поиск
    @GetMapping("/")
    public String products(@RequestParam(name = "title", required = false) String title, Model model, Principal principal) {
        model.addAttribute("products", productService.listProducts(title));
        model.addAttribute("currentUser", productService.getUserByPrincipal(principal));
        return "products";
    }

    @GetMapping("/product/{id}")
    public String productInfo(@PathVariable Long id, Model model, Principal principal){
        // проверка вдруг кто то решит зайти на продукт которого нет (или удалённый)
        Product product = productService.getProductById(id);
        if(product == null || product.isDeleted()){
            return "redirect:/";
        }
        model.addAttribute("product", product);
        model.addAttribute("currentUser", productService.getUserByPrincipal(principal));
        return "product-info";
    }


    // кнопка создать
    @PostMapping("/product/create")
    public String createProduct(Product product, @RequestParam("files") List<MultipartFile> files, Principal principal, RedirectAttributes redirectAttributes, int quantity) throws IOException {

        // Проверка что файлов не более 10 шт.
        if (files.size() > 10) {
            redirectAttributes.addFlashAttribute("errorMessage", "Можно загрузить максимум 10 файлов");
            User user = productService.getUserByPrincipal(principal);
            return "redirect:/user/" + user.getId();
        }

        productService.saveProduct(principal, product, files, quantity);

        // выкинуть на главную
        return "redirect:/";
    }


    //кнопка удалить (только владелец или админ)
    @PostMapping("/product/delete/{id}")
    public String deleteProduct(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        if (!productService.deleteProducts(id, principal)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Не удалось удалить товар");
        }
        //выкинуть на главную
        return "redirect:/";
    }

    //пополнить остаток товара (только владелец или админ)
    @PostMapping("/product/restock/{id}")
    public String restockProduct(@PathVariable Long id, @RequestParam int amount, Principal principal, RedirectAttributes redirectAttributes) {
        if (!productService.restockProduct(id, amount, principal)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Не удалось изменить количество");
        }
        return "redirect:/product/" + id;
    }

}
