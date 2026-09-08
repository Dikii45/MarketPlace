package com.marketHub.marketplace.controllers;

import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.models.enums.Category;
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

    // нулевой старт сайта: поиск по названию + фильтры по категории/цене + сортировка по цене
    @GetMapping("/")
    public String products(@RequestParam(required = false) String title,
                            @RequestParam(required = false) Category category,
                            @RequestParam(required = false) Integer minPrice,
                            @RequestParam(required = false) Integer maxPrice,
                            @RequestParam(required = false) String sort,
                            Model model, Principal principal) {
        model.addAttribute("products", productService.listProducts(title, category, minPrice, maxPrice, sort));
        model.addAttribute("currentUser", productService.getUserByPrincipal(principal));
        model.addAttribute("searchTitle", title);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("selectedSort", sort);
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
    public String createProduct(Product product, @RequestParam("files") List<MultipartFile> files, Principal principal, RedirectAttributes redirectAttributes) throws IOException {

        if(principal == null){
            redirectAttributes.addFlashAttribute("errorMessage", "Авторизируетесь");
            return "redirect:/login";
        }

        try {
            productService.saveProduct(principal, product, files);
        } catch (IllegalArgumentException e){

            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            User user = productService.getUserByPrincipal(principal);

            return "redirect:/user/" + user.getId();
        }


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
