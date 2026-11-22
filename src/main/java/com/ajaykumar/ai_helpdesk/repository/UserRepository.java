package com.ajaykumar.ai_helpdesk.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ajaykumar.ai_helpdesk.model.AppUser;

public interface UserRepository extends JpaRepository<AppUser,Long> {
    Optional<AppUser> findByUsername(String username);
    boolean existsByUsername(String username);
    
}
