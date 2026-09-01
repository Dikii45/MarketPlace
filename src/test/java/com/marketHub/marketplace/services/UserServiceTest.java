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

    private User newUser(String email, String rawPassword) {
        User u = new User();
        u.setEmail(email);
        u.setPassword(rawPassword);
        return u;
    }

    @Test
    void createUser_newEmail_encodesPasswordAndAssignsUserRole() {
        User user = newUser("new@a.com", "plain");
        when(userRepository.findByEmail("new@a.com")).thenReturn(null);
        when(passwordEncoder.encode("plain")).thenReturn("ENCODED");

        boolean result = userService.createUser(user);

        assertThat(result).isTrue();
        assertThat(user.getPassword()).isEqualTo("ENCODED");
        assertThat(user.isActive()).isTrue();
        assertThat(user.getRoles()).containsExactly(Role.ROLE_USER);
        verify(userRepository).save(user);
    }

    @Test
    void createUser_emailAlreadyTaken_rejectedWithoutSaving() {
        User existing = newUser("dup@a.com", "whatever");
        when(userRepository.findByEmail("dup@a.com")).thenReturn(existing);

        boolean result = userService.createUser(newUser("dup@a.com", "plain"));

        assertThat(result).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_blankName_rejected() {
        User user = newUser("a@a.com", "x");

        boolean result = userService.updateProfile(user, "   ", "+7999");

        assertThat(result).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateProfile_validData_trimsAndSaves() {
        User user = newUser("a@a.com", "x");

        boolean result = userService.updateProfile(user, "  Иван  ", "  +79990001122  ");

        assertThat(result).isTrue();
        assertThat(user.getName()).isEqualTo("Иван");
        assertThat(user.getPhoneNumber()).isEqualTo("+79990001122");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_blankPhone_storedAsNull() {
        User user = newUser("a@a.com", "x");

        userService.updateProfile(user, "Иван", "   ");

        assertThat(user.getPhoneNumber()).isNull();
    }

    @Test
    void userBan_activeUser_getsBanned() {
        User user = newUser("a@a.com", "x");
        user.setId(1L);
        user.setActive(true);
        // userBan(Long) вызывает унаследованный JpaRepository#findById(Long), а не
        // соседний кастомный findById(long) — поэтому здесь важно замокать именно
        // Optional-версию через переменную типа Long, а не примитивный литерал.
        Long id = 1L;
        when(userRepository.findById(id)).thenReturn(java.util.Optional.of(user));

        userService.userBan(id);

        assertThat(user.isActive()).isFalse();
    }

    @Test
    void userBan_bannedUser_getsUnbanned() {
        User user = newUser("a@a.com", "x");
        user.setId(1L);
        user.setActive(false);
        Long id = 1L;
        when(userRepository.findById(id)).thenReturn(java.util.Optional.of(user));

        userService.userBan(id);

        assertThat(user.isActive()).isTrue();
    }

    @Test
    void passwordMatch_delegatesToPasswordEncoder() {
        User user = newUser("a@a.com", "ENCODED_IN_DB");
        when(passwordEncoder.matches("raw-input", "ENCODED_IN_DB")).thenReturn(true);

        boolean result = userService.passwordMatch("raw-input", user);

        assertThat(result).isTrue();
    }

    @Test
    void touch_neverActiveBefore_setsLastActiveAtAndSaves() {
        User user = newUser("a@a.com", "x");
        assertThat(user.getLastActiveAt()).isNull();

        userService.touch(user);

        assertThat(user.getLastActiveAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void touch_activeLessThanThirtySecondsAgo_doesNotHitRepository() {
        User user = newUser("a@a.com", "x");
        user.setLastActiveAt(java.time.LocalDateTime.now().minusSeconds(5));

        userService.touch(user);

        verify(userRepository, never()).save(any());
    }
}
