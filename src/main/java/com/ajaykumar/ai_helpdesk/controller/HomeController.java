package com.ajaykumar.ai_helpdesk.controller;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.ajaykumar.ai_helpdesk.model.Ticket;
import com.ajaykumar.ai_helpdesk.repository.TicketRepository;
import com.ajaykumar.ai_helpdesk.service.GeminiService;

@RestController 
@RequestMapping("/home")

public class HomeController {

    private final TicketRepository ticketRepository;
   private final GeminiService geminiService;
  

   public HomeController(TicketRepository ticketRepository,
      GeminiService geminiService){
this.ticketRepository=ticketRepository;
this.geminiService=geminiService;
      }
   
    @GetMapping("/welcome")
    public String home(){
        return "welcome to AI Helpdesk Mangement System! ";
    }
    @GetMapping("/about")
    public String about(){
        return "This project helps employees manage IT support tickets.";
    }
    
    @GetMapping("/tickets")
    public List<Ticket> getTickets(@RequestParam(required = false) String status,
    Authentication auth){
   boolean hasStatus=status !=null && !status.isBlank();
   if(isAdmin(auth)){
      return hasStatus ? ticketRepository.findByStatus(status):ticketRepository.findAll();
   }
   String me=auth.getName();
   return hasStatus
   ? ticketRepository.findByOwnerUsernameAndStatus(me,status)
   : ticketRepository.findByOwnerUsername(me);
}

     @GetMapping("/allTickets")
     public List<Ticket> getAllTickets(){
        return ticketRepository.findAll();
     }

     
     @PostMapping("/create-ticket")
     public String createTicket(@RequestBody Ticket ticket){
        return "Ticket created successfully! Customer:"
        +ticket.getCustomerName()
        +",Issue:"
        +ticket.getIssueDescription();
     }
    
     /* 
     @Autowired //it tells spring boot to provide an object of TicketRespository automatically.
     //normally,in java,we create objects using new. but spring manages this object for us.
     private TicketRepository ticketRepository;
     */
     //POST API
     @PostMapping ("/addTicket") //post accepts requests.API endpoint
     public ResponseEntity<?> addTicket(@RequestBody Ticket ticket,Authentication auth){//converts JSON into a Ticket object.
       if(isBlank(ticket.getIssueDescription())){
         return ResponseEntity.badRequest()
         .body(Map.of("error","customerName and issueDescription are required"));
       }
      ticket.setTicketId(null);
      ticket.setCustomerName(auth.getName());
      ticket.setOwnerUsername(auth.getName());
      ticket.setStatus("OPEN");
      ticket.setAiSuggestion(null);

      String p=ticket.getPriority() == null ? "MEDIUM" :ticket.getPriority().toUpperCase();
      ticket.setPriority(List.of("LOW","MEDIUM","HIGH").contains(p) ? p : "MEDIUM");
      //return ticketRepository.save(ticket);//saves ticket into mysql. returns the saved ticket.

      return ResponseEntity.ok(ticketRepository.save(ticket));
      
     }

     @PostMapping("/analyze")
public ResponseEntity<?> analyzeText(@RequestBody Map<String, String> body) {
    String issue = body.get("issueDescription");
    if (isBlank(issue)) {
        return ResponseEntity.badRequest().body(Map.of("error", "issueDescription is required"));
    }
    try {
        return ResponseEntity.ok(Map.of("aiSuggestion", geminiService.analyzeIssue(issue)));
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", String.valueOf(e.getMessage())));
    }
}

     @PutMapping("/updateStatus/{ticketId}")
     public ResponseEntity<?> updateStatus(@PathVariable long ticketId,
      @RequestBody Map<String,String> body,Authentication auth){

         String status=body.get("status");

         
         if(isBlank(status)){
            return ResponseEntity.badRequest().body(Map.of("error","status is required"));
}
Ticket ticket =ticketRepository.findById(ticketId).orElse(null);
if(ticket ==null){
   return ResponseEntity.notFound().build();
}
if(!canAccess(ticket,auth)){
   return ResponseEntity.status(403).body(Map.of("error","Not allowed"));
}
ticket.setStatus(status);
return ResponseEntity.ok(ticketRepository.save(ticket));
      }

      @PostMapping("/ticket/{ticketId}/analyze")
      public ResponseEntity<?> analyzeTicket(@PathVariable Long ticketId,Authentication auth){
         Ticket ticket=ticketRepository.findById(ticketId).orElse(null);
         if(ticket == null){
            return ResponseEntity.notFound().build();
         }
         if(!canAccess(ticket,auth)){
            return ResponseEntity.status(403).body(Map.of("error","Not allowed"));
         }
        try{ 
         ticket.setAiSuggestion(geminiService.analyzeIssue(ticket.getIssueDescription()));
return ResponseEntity.ok(ticketRepository.save(ticket));
         } catch (Exception e) {
    e.printStackTrace();
    return ResponseEntity.status(500).body(Map.of("error", String.valueOf(e.getMessage())));
}
      
     

   }

      @DeleteMapping("/ticket/{ticketId}")
   public ResponseEntity<?> deleteTicket(@PathVariable Long ticketId,Authentication auth){
      Ticket ticket=ticketRepository.findById(ticketId).orElse(null);
if(ticket == null){
   return ResponseEntity.notFound().build();
}

if(!canAccess(ticket,auth)){
   return ResponseEntity.status(403).body(Map.of("error","Not allowed"));
}
ticketRepository.deleteById(ticketId);
return ResponseEntity.ok(Map.of("deleted",ticketId));
   }
   
   @GetMapping("/stats")
   public Map<String,Long> stats(Authentication auth){
      if(isAdmin(auth)){
      return Map.of(
         "total",ticketRepository.count(),
         "open",ticketRepository.countByStatus("OPEN"),
         "closed",ticketRepository.countByStatus("CLOSED"));
      
   }String me=auth.getName();
   return Map.of(
      "total",ticketRepository.countByOwnerUsername(me),
      "open",ticketRepository.countByOwnerUsernameAndStatus(me,"OPEN"),
      "closed",ticketRepository.countByOwnerUsernameAndStatus(me,"CLOSED"));
   }

   private boolean isAdmin(Authentication auth){
      return auth.getAuthorities().stream()
      .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
   }

private boolean canAccess(Ticket ticket, Authentication auth){
   return isAdmin(auth) || auth.getName().equals(ticket.getOwnerUsername());
}







   private boolean isBlank(String s){
      return s== null || s.isBlank();
   }

}