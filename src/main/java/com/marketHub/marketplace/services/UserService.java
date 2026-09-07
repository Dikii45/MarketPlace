package com.marketHub.marketplace.services;

import com.marketHub.marketplace.models.Image;
import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.models.enums.Role;
import com.marketHub.marketplace.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // российский номер: 11 цифр, начинается с 7 или 8 (+7XXXXXXXXXX / 8XXXXXXXXXX),
    // пробелы/скобки/дефисы допускаются — сверяем только по цифрам
    private static final Pattern RUSSIAN_PHONE = Pattern.compile("^[78]\\d{10}$");

    // регистрация только с популярных почтовых сервисов — отсекаем разовые/мусорные адреса
    private static final Set<String> ALLOWED_EMAIL_DOMAINS = Set.of(
            "gmail.com", "yandex.ru", "ya.ru", "mail.ru", "list.ru", "bk.ru", "inbox.ru",
            "rambler.ru", "outlook.com", "hotmail.com", "live.com", "yahoo.com", "icloud.com", "protonmail.com"
    );


    // номер необязателен; если введён — должен быть российским
    public boolean isValidRussianPhone(String phone) {

        if (phone == null || phone.isBlank()) return true;
        String digits = phone.replaceAll("[^0-9]", "");

        return RUSSIAN_PHONE.matcher(digits).matches();
    }

    //проверка что введен Российский номер
    public boolean isAllowedEmailDomain(String email) {

        if (email == null) return false;

        String trimmed = email.trim();

        //Поиск '@' на какой он месте стоит
        int at = trimmed.indexOf('@');
        //убираем все что было до '@'
        String domain = at >= 0 ? trimmed.substring(at + 1).toLowerCase() : "";

        return ALLOWED_EMAIL_DOMAINS.contains(domain);
    }

    public String allowedEmailDomainsList() {
        return String.join(", ", ALLOWED_EMAIL_DOMAINS);
    }


    public boolean createUser(User user) {
        // проверка уникальности
        if (userRepository.findByEmail(user.getEmail())!=null) return false;
        user.setActive(true);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.getRoles().add(Role.ROLE_USER);
        log.info("Saving new User with email: {}", user.getEmail());
        userRepository.save(user);
        return true;
    }

    public List<User> list() {
        return userRepository.findAll();
    }

    public User getUserByPrincipal(Principal principal) {
        if (principal == null) return null;
        return userRepository.findByEmail(principal.getName());
    }

    public void userBan(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            if (user.isActive()) {
                user.setActive(false);
            } else {
                user.setActive(true);
            }
        }
        userRepository.save(user);
    }

    public void changeUserRoles(User user, Map<String, String> form) {
        Set<String> roles = Arrays.stream(Role.values())
                .map(Role::name)
                .collect(Collectors.toSet());

        user.getRoles().clear();

        for (String key : form.keySet()) {
            if (roles.contains(key)) {
                user.getRoles().add(Role.valueOf(key));
            }
        }

        userRepository.save(user);
    }

    public boolean updateProfile(User user, String name, String phoneNumber) {
        if (name == null || name.isBlank()) return false;
        if(phoneNumber != null && phoneNumber.isBlank()){
            phoneNumber = null;
        }
        user.setName(name.trim());
        user.setPhoneNumber(phoneNumber != null ? phoneNumber.trim() : null);
        userRepository.save(user);
        return true;
    }

    public boolean setAvatar(User user, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return false;
            user.setAvatar(toImageEntity(file));
            userRepository.save(user);
            return true;
    }


    private Image toImageEntity(MultipartFile file) throws IOException {
        Image image = new Image();
        image.setName(file.getName());
        image.setOriginalFileName(file.getOriginalFilename());
        image.setContentType(file.getContentType());
        image.setSize(file.getSize());
        image.setBytes(file.getBytes());
        return image;
    }


    public boolean passwordMatch(String currentPassword, User user) {
        return passwordEncoder.matches(currentPassword, user.getPassword())? true : false;
    }

    public void setPassword(User user,String newPassword){
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void deleteUser(User user){
        userRepository.delete(user);
    }


    public User getUserByID(long id){
       return userRepository.findById(id);
    }

    // обновляем отметку активности не чаще раза в 30 секунд, чтобы не писать в БД на каждый запрос
    public void touch(User user) {
        LocalDateTime now = LocalDateTime.now();
        if (user.getLastActiveAt() == null || user.getLastActiveAt().isBefore(now.minusSeconds(30))) {
            user.setLastActiveAt(now);
            userRepository.save(user);
        }
    }
}
