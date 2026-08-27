package com.marketHub.marketplace.controllers;

import com.marketHub.marketplace.models.enums.OrderStatus;
import com.marketHub.marketplace.services.OrderService;
import com.marketHub.marketplace.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class OrderController {
    private final UserService userService;
    private final OrderService orderService;

    @GetMapping("/orders")
    public String orders(Model model, Principal principal) {
        model.addAttribute("currentUser", userService.getUserByPrincipal(principal));
        model.addAttribute("myPurchases", orderService.getMyPurchases(principal));
        model.addAttribute("mySales", orderService.getMySales(principal));
        model.addAttribute("statuses", OrderStatus.values());
        return "orders";
    }

    @PostMapping("/orders/items/{itemId}/status")
    public String updateStatus(@PathVariable("itemId") Long itemId, @RequestParam("status") OrderStatus status, Principal principal) {
        orderService.updateStatus(itemId, status, principal);
        return "redirect:/orders";
    }
}
