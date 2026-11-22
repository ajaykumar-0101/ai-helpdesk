package com.ajaykumar.ai_helpdesk.controller;
import java.util.*;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ajaykumar.ai_helpdesk.model.AppUser;
import com.ajaykumar.ai_helpdesk.repository.UserRepository;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,PasswordEncoder passwordEncoder){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body){
        String username=body.get("username");
        String password=body.get("password");

        if(username == null || !username.matches("[A-Za-z0-9_]{3,30}")){
            return ResponseEntity.badRequest()
            .body(Map.of("error","Username must be 3-30 letters,digits or umderscire"));

        }
        if(password == null || password.length() <6){
            return ResponseEntity.badRequest()
            .body(Map.of("error","Password must be at least 6 characters"));
        }
        if(userRepository.existsByUsername(username)){
            return ResponseEntity.status(409).body(Map.of("error","Username already exists"));
        }

        AppUser user=new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("registered",true));
    }
    @GetMapping("/me")
    public Map<String,Object> me(Authentication authentication){
        boolean admin=authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return Map.of("username",authentication.getName(),"admin",admin);
    }
    
}
