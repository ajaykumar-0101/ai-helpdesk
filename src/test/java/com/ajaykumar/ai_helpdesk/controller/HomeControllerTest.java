package com.ajaykumar.ai_helpdesk.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.ajaykumar.ai_helpdesk.model.Ticket;
import com.ajaykumar.ai_helpdesk.repository.TicketRepository;
import com.ajaykumar.ai_helpdesk.service.GeminiService;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private HomeController controller;

    private Authentication user(String name) {
        return new UsernamePasswordAuthenticationToken(
                name, "pw", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private Authentication admin() {
        return new UsernamePasswordAuthenticationToken(
                "admin", "pw", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private Ticket ticket(Long id, String owner, String issue) {
        Ticket t = new Ticket();
        t.setTicketId(id);
        t.setOwnerUsername(owner);
        t.setIssueDescription(issue);
        return t;
    }

    @Test
    void addTicket_blankIssue_returns400() {
        Ticket t = new Ticket();
        t.setIssueDescription("   ");

        ResponseEntity<?> response = controller.addTicket(t, user("ajay"));

        assertEquals(400, response.getStatusCode().value());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void addTicket_setsOwnerStatusAndDefaultsPriority() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket t = new Ticket();
        t.setCustomerName("hacker");
        t.setIssueDescription("Wi-Fi not working");
        t.setPriority("urgent");

        controller.addTicket(t, user("ajay"));

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        Ticket saved = captor.getValue();
        assertEquals("ajay", saved.getOwnerUsername());
        assertEquals("ajay", saved.getCustomerName());
        assertEquals("OPEN", saved.getStatus());
        assertEquals("MEDIUM", saved.getPriority());
    }

    @Test
    void addTicket_ignoresClientSentIdStatusAndAiText() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket t = new Ticket();
        t.setTicketId(99L);
        t.setStatus("CLOSED");
        t.setAiSuggestion("fake");
        t.setIssueDescription("Printer problem");
        t.setPriority("high");

        controller.addTicket(t, user("ajay"));

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        Ticket saved = captor.getValue();
        assertNull(saved.getTicketId());
        assertNull(saved.getAiSuggestion());
        assertEquals("OPEN", saved.getStatus());
        assertEquals("HIGH", saved.getPriority());
    }

    @Test
    void getTickets_asUser_returnsOnlyOwnTickets() {
        List<Ticket> own = List.of(ticket(1L, "ajay", "a"));
        when(ticketRepository.findByOwnerUsername("ajay")).thenReturn(own);

        List<Ticket> result = controller.getTickets(null, user("ajay"));

        assertEquals(1, result.size());
        verify(ticketRepository, never()).findAll();
    }

    @Test
    void getTickets_asAdmin_returnsAllTickets() {
        when(ticketRepository.findAll()).thenReturn(
                List.of(ticket(1L, "ajay", "a"), ticket(2L, "test1", "b")));

        List<Ticket> result = controller.getTickets("", admin());

        assertEquals(2, result.size());
        verify(ticketRepository, never()).findByOwnerUsername(any());
    }

    @Test
    void analyzeTicket_otherUsersTicket_returns403() {
        when(ticketRepository.findById(5L))
                .thenReturn(Optional.of(ticket(5L, "someoneElse", "problem")));

        ResponseEntity<?> response = controller.analyzeTicket(5L, user("ajay"));

        assertEquals(403, response.getStatusCode().value());
        verify(geminiService, never()).analyzeIssue(any());
    }

    @Test
    void analyzeTicket_success_savesSuggestion() {
        Ticket t = ticket(1L, "ajay", "wifi");
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(t));
        when(geminiService.analyzeIssue("wifi")).thenReturn("Restart the router");
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = controller.analyzeTicket(1L, user("ajay"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Restart the router", t.getAiSuggestion());
    }

    @Test
    void analyzeTicket_aiFails_returns500WithMessage() {
        when(ticketRepository.findById(1L))
                .thenReturn(Optional.of(ticket(1L, "ajay", "wifi")));
        when(geminiService.analyzeIssue(any()))
                .thenThrow(new IllegalStateException("AI service is busy"));

        ResponseEntity<?> response = controller.analyzeTicket(1L, user("ajay"));

        assertEquals(500, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("AI service is busy", body.get("error"));
    }

    @Test
    void deleteTicket_notFound_returns404() {
        when(ticketRepository.findById(5L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.deleteTicket(5L, user("ajay"));

        assertEquals(404, response.getStatusCode().value());
        verify(ticketRepository, never()).deleteById(any());
    }

    @Test
    void deleteTicket_owner_deletes() {
        when(ticketRepository.findById(7L))
                .thenReturn(Optional.of(ticket(7L, "ajay", "x")));

        ResponseEntity<?> response = controller.deleteTicket(7L, user("ajay"));

        assertEquals(200, response.getStatusCode().value());
        verify(ticketRepository).deleteById(7L);
    }

    @Test
    void updateStatus_blankStatus_returns400() {
        ResponseEntity<?> response =
                controller.updateStatus(1L, Map.of("status", " "), user("ajay"));

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void stats_asUser_countsOnlyOwnTickets() {
        when(ticketRepository.countByOwnerUsername("ajay")).thenReturn(3L);
        when(ticketRepository.countByOwnerUsernameAndStatus("ajay", "OPEN")).thenReturn(2L);
        when(ticketRepository.countByOwnerUsernameAndStatus("ajay", "CLOSED")).thenReturn(1L);

        Map<String, Long> stats = controller.stats(user("ajay"));

        assertEquals(3L, stats.get("total"));
        assertEquals(2L, stats.get("open"));
        assertEquals(1L, stats.get("closed"));
    }
}