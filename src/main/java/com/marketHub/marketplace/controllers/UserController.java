package com.marketHub.marketplace.controllers;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // регистрация только с популярных почтовых сервисов
    private static final Set<String> ALLOWED_EMAIL_DOMAINS = Set.of(
            "gmail.com", "yandex.ru", "ya.ru", "mail.ru", "list.ru", "bk.ru", "inbox.ru",
            "rambler.ru", "outlook.com", "hotmail.com", "live.com", "yahoo.com", "icloud.com", "protonmail.com"
    );



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

    //confirmPasswor(проверка на повторный пароль) на фротенде можно обойти на повтор пароль
    @PostMapping("/registration")
    public String createUser(User user, Model model, @RequestParam String confirmPassword) {



        if(user.getEmail() == null || user.getEmail().isEmpty()){
            model.addAttribute("errorMessage", "Нужно ввести почту");
        }

        String email = user.getEmail().trim();

        //ищу на какой позиции стоит @
        int at = email.indexOf('@');
        //убираю все что было до @ оставляю только идекс почты
        String domain = at >= 0 ? email.substring(at + 1).toLowerCase() : "";

        if (!ALLOWED_EMAIL_DOMAINS.contains(domain)) {
            model.addAttribute("errorMessage", "Регистрация доступна только с популярных почтовых сервисов: " + String.join(", ", ALLOWED_EMAIL_DOMAINS));
            return "registration";
        }

        if(!confirmPassword.equals(user.getPassword())){
            model.addAttribute("errorMessage", "Пароль не совпадает");
            return "registration";
        }

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


    @GetMapping("/account")
    public String account(Model model, Principal principal) {
        model.addAttribute("currentUser", userService.getUserByPrincipal(principal));
        return "account";
    }

    @PostMapping("/account")
    public String changeNamePhoneNumber(@RequestParam String name, @RequestParam(required = false) String phoneNumber, Principal principal,
                                        RedirectAttributes redirectAttributes) {
        User user = userService.getUserByPrincipal(principal);
        if (!userService.updateProfile(user, name, phoneNumber)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Имя не может быть пустым");
        }
        return "redirect:/account";
    }

    @PostMapping("/account/avatar")
    public String avatar(@RequestParam("avatar") MultipartFile file, Principal principal, RedirectAttributes redirectAttributes) throws IOException {
        User user = userService.getUserByPrincipal(principal);
        if(!userService.setAvatar(user, file)){
            redirectAttributes.addFlashAttribute("errorMessage", "Авартка не может быть пустой");
        }
        return "redirect:/account";
    }

    @PostMapping("/account/password")
    public String changePasswor(@RequestParam String currentPassword, @RequestParam String newPassword, @RequestParam String confirmPassword, Principal principal,Model model){
        User user = userService.getUserByPrincipal(principal);
        model.addAttribute("currentUser", userService.getUserByPrincipal(principal));

        if(!userService.passwordMatch(currentPassword, user)){
            model.addAttribute("errorMessage", "не совпадение пароля");

            return "account";
        }

        if(!confirmPassword.equals(newPassword)){
            model.addAttribute("errorMessage", "Не верно ввел текщий пароль. -_-");
            return "account";
        }

        userService.setPassword(user,newPassword);

        return "redirect:/account";
    }

    @PostMapping("/account/delete")
    public String deleteUser(Principal principal, HttpServletRequest request) throws ServletException {
        User user = userService.getUserByPrincipal(principal);
        userService.deleteUser(user);
       // подчищаю информацию кто залогинен
        request.logout();
        return "redirect:/";
    }
}
