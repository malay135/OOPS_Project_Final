package i_itsm.controller;

import java.util.Map;
import java.util.Optional;
import org.springframework.web.bind.annotation.*;
import i_itsm.model.Feedback;
import i_itsm.service.FeedbackService;

@RestController
@RequestMapping("/api/incidents/{incidentId}/feedback")
public class FeedbackController {
    private final FeedbackService service;
    public FeedbackController(FeedbackService service){this.service=service;}
    @GetMapping public Optional<Feedback> get(@PathVariable Long incidentId){return service.get(incidentId);}
    @PostMapping public Feedback submit(@PathVariable Long incidentId,@RequestBody Map<String,Object> body){
        int rating=((Number)body.getOrDefault("rating",0)).intValue();
        return service.submit(incidentId,rating,(String)body.get("comment"));
    }
}
