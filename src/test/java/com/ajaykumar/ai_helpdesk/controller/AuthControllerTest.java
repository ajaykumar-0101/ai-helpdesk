package com.ajaykumar.ai_helpdesk.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ajaykumar.ai_helpdesk.model.AppUser;
import com.ajaykumar.ai_helpdesk.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController controller;

    @Test
    void register_badUsername_returns400() {
        ResponseEntity<?> response =
                controller.register(Map.of("username", "a!", "password", "secret1"));

        assertEquals(400, response.getStatusCode().value());
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void register_shortPassword_returns400() {
        ResponseEntity<?> response =
                controller.register(Map.of("username", "ajay", "password", "123"));

        assertEquals(400, response.getStatusCode().value());
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void register_duplicateUsername_returns409() {
        when(userRepository.existsByUsername("ajay")).thenReturn(true);

        ResponseEntity<?> response =
                controller.register(Map.of("username", "ajay", "password", "secret1"));

        assertEquals(409, response.getStatusCode().value());
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void register_success_hashesPasswordAndSetsUserRole() {
        when(userRepository.existsByUsername("ajay")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("HASHED");

        ResponseEntity<?> response =
                controller.register(Map.of("username", "ajay", "password", "secret1"));

        assertEquals(200, response.getStatusCode().value());
        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertEquals("ajay", saved.getUsername());
        assertEquals("HASHED", saved.getPassword());
        assertEquals("USER", saved.getRole());
    }
}