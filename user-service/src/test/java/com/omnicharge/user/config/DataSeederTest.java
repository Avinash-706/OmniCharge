package com.omnicharge.user.config;

import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.Role;
import com.omnicharge.user.entity.User;
import com.omnicharge.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    void run_ShouldSeedAdminAndDemoUser_WhenUsersDoNotExist() {
        // Given
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.existsByEmail("admin@omnicharge.com")).thenReturn(false);
        when(userRepository.existsByEmail("user1@omnicharge.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        dataSeeder.run();

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());

        User admin = userCaptor.getAllValues().get(0);
        assertThat(admin.getEmail()).isEqualTo("admin@omnicharge.com");
        assertThat(admin.getFullName()).isEqualTo("Admin User");
        assertThat(admin.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(admin.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(admin.getIsActive()).isTrue();
        assertThat(admin.getIsMobileVerified()).isTrue();

        User demoUser = userCaptor.getAllValues().get(1);
        assertThat(demoUser.getEmail()).isEqualTo("user1@omnicharge.com");
        assertThat(demoUser.getFullName()).isEqualTo("Demo User");
        assertThat(demoUser.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(demoUser.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(demoUser.getIsActive()).isTrue();
        assertThat(demoUser.getIsMobileVerified()).isTrue();
    }

    @Test
    void run_ShouldNotSeedAdminUser_WhenAdminAlreadyExists() {
        // Given
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.existsByEmail("admin@omnicharge.com")).thenReturn(true);
        when(userRepository.existsByEmail("user1@omnicharge.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        dataSeeder.run();

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("user1@omnicharge.com");
    }

    @Test
    void run_ShouldNotSeedDemoUser_WhenDemoUserAlreadyExists() {
        // Given
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.existsByEmail("admin@omnicharge.com")).thenReturn(false);
        when(userRepository.existsByEmail("user1@omnicharge.com")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        dataSeeder.run();

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("admin@omnicharge.com");
    }

    @Test
    void run_ShouldNotSeedAnyUser_WhenBothUsersAlreadyExist() {
        // Given
        when(userRepository.existsByEmail("admin@omnicharge.com")).thenReturn(true);
        when(userRepository.existsByEmail("user1@omnicharge.com")).thenReturn(true);

        // When
        dataSeeder.run();

        // Then
        verify(userRepository, never()).save(any(User.class));
    }
}
