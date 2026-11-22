package com.ajaykumar.ai_helpdesk.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ajaykumar.ai_helpdesk.model.AppUser;
import com.ajaykumar.ai_helpdesk.repository.UserRepository;

@Service
public class AppUserDetailsService  implements UserDetailsService{
    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository){
        this.userRepository=userRepository;
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException{
        AppUser u=userRepository.findByUsername(username)
        .orElseThrow(()-> new UsernameNotFoundException("user not found"));

        return User.withUsername(u.getUsername())
        .password(u.getPassword())
        .roles(u.getRole())
        .build();
    }
}
