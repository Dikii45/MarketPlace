package com.marketHub.marketplace.controllers;

import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.services.CartItemService;
import com.marketHub.marketplace.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final UserService userService;
    private final CartItemService cartItemService;

    // нужен для common, что бы во верхнем меню отражалось количество товаров добавленые в корзину
    @ModelAttribute("cartCount")
    public int cartCount(Principal principal) {
        User user = userService.getUserByPrincipal(principal);
        if (user == null) return 0;
        // возрващаем кличетсво товаров которые в корзине
        return cartItemService.getCartItems(user).size();
    }

    // отмечаем пользователя активным на каждый запрос — на этом строится индикатор "онлайн" в чате
    @ModelAttribute
    public void touchLastActive(Principal principal) {
        User user = userService.getUserByPrincipal(principal);
        if (user != null) {
            userService.touch(user);
        }
    }
}
