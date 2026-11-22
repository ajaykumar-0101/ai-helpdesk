package com.ajaykumar.ai_helpdesk.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.ajaykumar.ai_helpdesk.model.AppUser;
import com.ajaykumar.ai_helpdesk.repository.UserRepository;

@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.password:}")
    private String adminPassword;

    public DataSeeder(UserRepository userRepository,PasswordEncoder passwordEncoder){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
    }

    @Override
    public void run(String... args){
        if(adminPassword ==null || adminPassword.isBlank()){
            System.out.println("ADMIN_PASSWORD is not set, so no admin account was created.");
            return;
        }
        if(!userRepository.existsByUsername("admin")){
            AppUser admin=new AppUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole("ADMIN");
            userRepository.save(admin);
            System.out.println("Admin account created.");
        }
    }
    
}
