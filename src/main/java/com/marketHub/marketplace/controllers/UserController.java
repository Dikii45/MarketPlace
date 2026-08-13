package com.marketHub.marketplace.controllers;


import org.springframework.ui.Model;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/login")
    public String login(@RequestParam(name = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Неверный email или пароль");
        }
        return "login";
    }

    @GetMapping("/registration")
    public String registration() {
        return "registration";
    }

    @PostMapping("/registration")
    public String createUser(User user, Model model) {
        if(!userService.createUser(user)){
            model.addAttribute("errorMessage", "Пользователь с email: " + user.getEmail() + "уже существует");
            return "registration";
        }
        return "redirect:/login";
    }

    @GetMapping("/hello")
    public String securityUrl(){
        return "hello";
    }

    @GetMapping("/user/{user}")
    public String userUnfo(@PathVariable("user") User user, Model model, Principal principal)
    {
        model.addAttribute("user", user);
        model.addAttribute("products",user.getProducts());
        model.addAttribute("currentUser", userService.getUserByPrincipal(principal));
        return "user-info";
    }
}
