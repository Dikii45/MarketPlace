package com.marketHub.marketplace.controllers;

import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.models.enums.OrderStatus;
import com.marketHub.marketplace.services.OrderService;
import com.marketHub.marketplace.services.ProductService;
import com.marketHub.marketplace.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {
    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;

    @PostMapping("/product/order/{id}")
    public String creatOrder(@PathVariable("id") Long productID, Principal principal, RedirectAttributes redirectAttributes) {

        User user = userService.getUserByPrincipal(principal);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Необходмо зайти в аккаунт");
            return "redirect:/product/" + productID;
        }

        Product product = productService.getProductById(productID);
        // проверка на то что мы не покупаем сами у себя
        if (product.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Нельзя заказать собственный товар");
            return "redirect:/product/" + productID;
        }

        orderService.saveOrder(user,product);

        return "redirect:/product/" + productID;
    }

    @GetMapping("/orders")
    public String orders(Model model, Principal principal) {
        model.addAttribute("currentUser", userService.getUserByPrincipal(principal));
        model.addAttribute("myPurchases", orderService.getMyPurchases(principal));
        model.addAttribute("mySales", orderService.getMySales(principal));
        model.addAttribute("statuses", OrderStatus.values());
        return "orders";
    }

    @PostMapping("/orders/{id}/status")
    public String updateStatus(@PathVariable("id") Long id, @RequestParam("status") OrderStatus status, Principal principal) {
        orderService.updateStatus(id, status, principal);
        return "redirect:/orders";
    }

}
