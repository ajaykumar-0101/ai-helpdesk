package com.ajaykumar.ai_helpdesk.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ticketId;

    private String customerName;
    private String ownerUsername;

    @Column(length = 1000)
    private String issueDescription;

    private String status = "OPEN";

    @Column(columnDefinition = "TEXT")
    private String aiSuggestion;


    private String priority="MEDIUM";

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getIssueDescription() { return issueDescription; }
    public void setIssueDescription(String issueDescription) { this.issueDescription = issueDescription; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAiSuggestion() { return aiSuggestion; }
    public void setAiSuggestion(String aiSuggestion) { this.aiSuggestion = aiSuggestion; }

    public String getPriority(){
        return priority;
    }
    public void setPriority(String priority){
        this.priority=priority;
    }
    public String getOwnerUsername(){
        return ownerUsername;
    }
    public void setOwnerUsername(String ownerUsername){
this.ownerUsername=ownerUsername;
    }
}