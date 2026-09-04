package com.marketHub.marketplace.services;

import com.marketHub.marketplace.models.User;
import com.marketHub.marketplace.models.enums.Role;
import com.marketHub.marketplace.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    //создание нового юзера
    private User newUser(String email, String rawPassword) {
        User u = new User();
        u.setEmail(email);
        u.setPassword(rawPassword);
        return u;
    }


    //как реагирует если restockProduct, если пароли не совпадают
    @Test
    void createUser_emailAlreadyTaken_rejectedWithoutSaving() {
        User existing = newUser("dup@a.com", "whatever");
        when(userRepository.findByEmail("dup@a.com")).thenReturn(existing);

        boolean result = userService.createUser(newUser("dup@a.com", "plain"));

        assertThat(result).isFalse();
        verify(userRepository, never()).save(any());
    }

    //как реагирует если restockProduct, имя отправить пустым
    @Test
    void updateProfile_blankName_rejected() {
        User user = newUser("a@a.com", "x");

        boolean result = userService.updateProfile(user, "   ", "+7999");

        assertThat(result).isFalse();
        verify(userRepository, never()).save(any());
    }


    //бан самого себя
    @Test
    void userBan_activeUser_getsBanned() {
        User user = newUser("a@a.com", "x");
        user.setId(1L);
        user.setActive(true);
        Long id = 1L;
        when(userRepository.findById(id)).thenReturn(java.util.Optional.of(user));

        userService.userBan(id);

        assertThat(user.isActive()).isFalse();
    }

}
