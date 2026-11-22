package i_itsm.service;

import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import i_itsm.model.Feedback;
import i_itsm.model.Incident;
import i_itsm.model.User;
import i_itsm.repository.FeedbackRepository;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.UserRepository;

@Service
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    public FeedbackService(FeedbackRepository f, IncidentRepository i, UserRepository u) { feedbackRepository=f; incidentRepository=i; userRepository=u; }
    private User current() { String n=SecurityContextHolder.getContext().getAuthentication().getName(); return userRepository.findByUsername(n).orElseThrow(() -> new AccessDeniedException("User not found")); }
    public Optional<Feedback> get(Long incidentId) { return feedbackRepository.findByIncidentId(incidentId); }
    public Feedback submit(Long incidentId, int rating, String comment) {
        User user=current(); Incident incident=incidentRepository.findById(incidentId).orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));
        if (incident.getReportedBy()==null || !incident.getReportedBy().getId().equals(user.getId())) throw new AccessDeniedException("Only the reporting employee can submit feedback");
        if (!("RESOLVED".equals(incident.getStatus()) || "CLOSED".equals(incident.getStatus()))) throw new IllegalStateException("Feedback is available only after resolution");
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be between 1 and 5");
        if (feedbackRepository.findByIncidentIdAndUserId(incidentId, user.getId()).isPresent()) throw new IllegalStateException("Feedback has already been submitted for this incident");
        Feedback f=new Feedback(); f.setIncident(incident); f.setUser(user); f.setRating(rating); f.setComment(comment == null ? null : comment.trim()); return feedbackRepository.save(f);
    }
}
