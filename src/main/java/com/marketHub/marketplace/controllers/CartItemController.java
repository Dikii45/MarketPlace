package com.marketHub.marketplace.controllers;

import com.marketHub.marketplace.models.Product;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.models.enums.PaymentMethod;
import com.marketHub.marketplace.services.CartItemService;
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
public class CartItemController {

    private final CartItemService cartItemService;
    private final UserService userService;
    private final ProductService productService;


    //Корзина
    @GetMapping("/cart")
    public String cart(Model model, Principal principal) {
        User user = userService.getUserByPrincipal(principal);
        model.addAttribute("currentUser", user);
        model.addAttribute("cartItems", cartItemService.getCartItems(user));
        model.addAttribute("cartTotal", cartItemService.getCartTotal(user));
        return "cart";
    }


    @PostMapping("/cart/add/{productId}")
    public String addProductInCart(@PathVariable Long productId, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.getUserByPrincipal(principal);
        // проверка на user
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Необходимо зайти в аккаунт");
            return "redirect:/login";
        }

        Product product = productService.getProductById(productId);

        //проверка есть ли вообще продукт, если нет то выкинуть на главную
        if (product == null) {
            return "redirect:/";
        }

        // проверка на то что мы не добавляем в корзину свой же товар
        if (product.getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Нельзя добавить собственный товар в корзину");
            return "redirect:/product/" + productId;
        }

        cartItemService.addProductToCart(user, product);
        return "redirect:/product/" + productId;
    }

    //удаление из корзины
    @PostMapping("/cart/remove/{productId}")
    public String removeProductFromCart(@PathVariable Long productId, Principal principal) {
        User user = userService.getUserByPrincipal(principal);
        //проверка на user
        if (user == null) {
            return "redirect:/login";
        }

        Product product = productService.getProductById(productId);

        //проверка что вообще есть такой продукт
        if (product != null) {
            cartItemService.removeFromCart(user, product);
        }
        return "redirect:/cart";
    }

    //изменить количество в корзине
    @PostMapping("/cart/update/{productId}")
    public String updateCartItemQuantity(@PathVariable Long productId, @RequestParam int quantity, Principal principal) {
        User user = userService.getUserByPrincipal(principal);
        if (user == null) {
            return "redirect:/login";
        }
        Product product = productService.getProductById(productId);

        if (product != null) {
            cartItemService.updateQuantity(user, product, quantity);
        }
        return "redirect:/cart";
    }

    //офоррмить заказ
    @GetMapping("/checkout")
    public String checkout(Model model, Principal principal) {
        User user = userService.getUserByPrincipal(principal);
        //проверка на user
        if (user == null) {
            return "redirect:/login";
        }
        List<?> cartItems = cartItemService.getCartItems(user);
        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartTotal", cartItemService.getCartTotal(user));
        return "checkout";
    }


    //оформляем заказ
    @PostMapping("/checkout")
    public String confirmCheckout(@RequestParam String address, @RequestParam PaymentMethod paymentMethod, Principal principal,
                                  RedirectAttributes redirectAttributes) {
        User user = userService.getUserByPrincipal(principal);
        if (user == null) {
            return "redirect:/login";
        }

        if (!cartItemService.checkout(user, address, paymentMethod)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Недостаточно товара на складе — проверьте количество в корзине");
            return "redirect:/cart";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Заказ оформлен");
        return "redirect:/orders";
    }
}
