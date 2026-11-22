package com.ajaykumar.ai_helpdesk.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ajaykumar.ai_helpdesk.model.Ticket;



public interface TicketRepository extends 
JpaRepository<Ticket, Long>{
    List<Ticket> findByStatus(String status);
    long countByStatus(String status);
    
    List<Ticket> findByOwnerUsername(String ownerUsername);
    List<Ticket> findByOwnerUsernameAndStatus(String ownerUsername,String status);
    long countByOwnerUsername(String ownerUsername);
    long countByOwnerUsernameAndStatus(String ownerUsername,String status);
}
