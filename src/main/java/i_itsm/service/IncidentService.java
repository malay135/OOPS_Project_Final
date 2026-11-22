package i_itsm.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import i_itsm.exception.IncidentAssignmentException;
import i_itsm.model.Incident;
import i_itsm.model.ITService;
import i_itsm.model.Technician;
import i_itsm.model.User;
import i_itsm.notification.NotificationService;
import i_itsm.repository.IncidentRepository;
import i_itsm.repository.ITServiceRepository;
import i_itsm.repository.TechnicianRepository;
import i_itsm.repository.UserRepository;
import i_itsm.state.IncidentState;
import i_itsm.state.IncidentStates;

@Service
public class IncidentService {
    private final IncidentRepository incidentRepository;
    private final PriorityService priorityService;
    private final AuditService auditService;
    private final SLAService slaService;
    private final NotificationService notificationService;
    private final TechnicianRepository technicianRepository;
    private final UserRepository userRepository;
    private final AssignmentService assignmentService;
    private final ITServiceRepository itServiceRepository;
    private final DuplicateDetectionService duplicateDetectionService;

    public IncidentService(IncidentRepository incidentRepository, PriorityService priorityService, AuditService auditService,
                           SLAService slaService, NotificationService notificationService, TechnicianRepository technicianRepository,
                           UserRepository userRepository) {
        this(incidentRepository, priorityService, auditService, slaService, notificationService, technicianRepository,
             userRepository, null, null, null);
    }

    @Autowired
    public IncidentService(IncidentRepository incidentRepository, PriorityService priorityService, AuditService auditService,
                           SLAService slaService, NotificationService notificationService, TechnicianRepository technicianRepository,
                           UserRepository userRepository, AssignmentService assignmentService, ITServiceRepository itServiceRepository,
                           DuplicateDetectionService duplicateDetectionService) {
        this.incidentRepository=incidentRepository; this.priorityService=priorityService; this.auditService=auditService;
        this.slaService=slaService; this.notificationService=notificationService; this.technicianRepository=technicianRepository;
        this.userRepository=userRepository; this.assignmentService=assignmentService; this.itServiceRepository=itServiceRepository;
        this.duplicateDetectionService=duplicateDetectionService;
    }

    private String currentUsername() { Authentication a=SecurityContextHolder.getContext().getAuthentication(); return a==null?"system":a.getName(); }
    private boolean hasRole(String role) { Authentication a=SecurityContextHolder.getContext().getAuthentication(); return a!=null && a.getAuthorities().stream().anyMatch(x -> ("ROLE_"+role).equals(x.getAuthority())); }
    private User currentUser() { return userRepository.findByUsername(currentUsername()).orElse(null); }

    private void checkCanUpdate(Incident incident) {
        if (hasRole("ADMINISTRATOR")) return;
        User me=currentUser();
        if (hasRole("EMPLOYEE")) {
            if (incident.getReportedBy()==null || me==null || !incident.getReportedBy().getId().equals(me.getId())) throw new AccessDeniedException("You can only update your own incidents");
            return;
        }
        if (hasRole("TECHNICIAN")) {
            Technician tech=technicianRepository.findByUsername(currentUsername()).orElseThrow(() -> new AccessDeniedException("Your login is not linked to a technician"));
            if (incident.getAssignedTechnician()==null || !incident.getAssignedTechnician().getId().equals(tech.getId())) throw new AccessDeniedException("You can only update incidents assigned to you");
            return;
        }
        throw new AccessDeniedException("You are not allowed to update this incident");
    }

    public Incident createIncident(Incident incident) {
        if(incident.getAffectedUsers()==null) incident.setAffectedUsers(1);
        checkAffectedUsers(incident.getAffectedUsers());
        incident.setPriority(priorityService.calculatePriority(incident.getImpact(), incident.getUrgency(), incident.getBusinessCriticality(), incident.getAffectedUsers()));
        incident.setStatus("OPEN"); incident.setCreatedAt(LocalDateTime.now()); incident.setResolutionNotes(null);
        User reporter=currentUser(); if(reporter!=null) incident.setReportedBy(reporter);
        if(incident.getAffectedService()!=null && itServiceRepository!=null){ ITService service=itServiceRepository.findById(incident.getAffectedService().getId()).orElseThrow(() -> new IllegalArgumentException("Affected IT service not found")); incident.setAffectedService(service); }
        slaService.calculateDeadline(incident);
        Incident saved=incidentRepository.save(incident);
        auditService.recordAction(saved.getId(),"INCIDENT_CREATED","Incident created with priority "+saved.getPriority()+" and SLA deadline "+saved.getSlaDeadline(),currentUsername());
        notificationService.sendNotification(saved.getId(),"Your incident '"+saved.getTitle()+"' has been created with priority "+saved.getPriority(),"IN_APP");
        notificationService.sendNotification(saved.getId(),"A new incident '"+saved.getTitle()+"' has been created.","EMAIL");
        if(duplicateDetectionService!=null) duplicateDetectionService.check(saved);
        try { if(assignmentService!=null) assignmentService.autoAssignIncident(saved.getId()); saved=incidentRepository.findById(saved.getId()).orElse(saved); }
        catch(IncidentAssignmentException e) { tellAdminsNotAssigned(saved, e.getMessage()); }
        return saved;
    }

    // Nobody could take the incident (no skill match, or every technician is at the workload limit): an admin must assign it
    private void tellAdminsNotAssigned(Incident incident, String reason) {
        auditService.recordAction(incident.getId(),"ASSIGNMENT_FAILED",reason,"system");
        String msg="Incident #"+incident.getId()+" '"+incident.getTitle()+"' could not be assigned automatically: "+reason+". Please assign it on the All incidents page.";
        for(User admin : userRepository.findByRole("ADMINISTRATOR")) notificationService.sendNotificationTo(incident.getId(),msg,"IN_APP",admin.getUsername());
    }

    private void checkAffectedUsers(Integer affectedUsers) {
        if(affectedUsers!=null && (affectedUsers<1 || affectedUsers>100000)) throw new IllegalArgumentException("Number of affected users must be between 1 and 100000");
    }

    public List<Incident> getAllIncidents(){return incidentRepository.findAll();}
    public Incident getIncidentById(Long id){return incidentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Incident not found: "+id));}

    public Incident updateIncident(Long id, String title, String description, String category, String impact, String urgency,
                                   String businessCriticality, Long affectedServiceId) {
        return updateIncident(id,title,description,category,impact,urgency,businessCriticality,affectedServiceId,null);
    }

    /** affectedUsers == null keeps the current number. */
    @Transactional
    public Incident updateIncident(Long id, String title, String description, String category, String impact, String urgency,
                                   String businessCriticality, Long affectedServiceId, Integer affectedUsers) {
        Incident incident=getIncidentById(id); checkCanUpdate(incident);
        if(!IncidentStates.of(incident.getStatus()).canEditDetails()) throw new IllegalStateException("Closed incidents cannot be edited");
        checkAffectedUsers(affectedUsers);
        if(title==null||title.isBlank()||description==null||description.isBlank()||category==null||category.isBlank()) throw new IllegalArgumentException("Title, description and category are required");
        if(title.length()>200||description.length()>2000) throw new IllegalArgumentException("Title or description is too long");
        String oldPriority=incident.getPriority(); Long oldServiceId=incident.getAffectedService()==null?null:incident.getAffectedService().getId();
        incident.setTitle(title.trim()); incident.setDescription(description.trim()); incident.setCategory(category.trim().toUpperCase());
        incident.setImpact(impact); incident.setUrgency(urgency); incident.setBusinessCriticality(businessCriticality);
        if(affectedServiceId!=null && itServiceRepository!=null) incident.setAffectedService(itServiceRepository.findById(affectedServiceId).orElseThrow(() -> new IllegalArgumentException("Affected IT service not found")));
        if(affectedUsers!=null) incident.setAffectedUsers(affectedUsers);
        String newPriority=priorityService.calculatePriority(impact,urgency,businessCriticality,incident.getAffectedUsers()); incident.setPriority(newPriority);
        Long newServiceId=incident.getAffectedService()==null?null:incident.getAffectedService().getId();
        if(!newPriority.equals(oldPriority)) {
            slaService.calculateDeadline(incident); auditService.recordAction(id,"PRIORITY_CHANGED",oldPriority+" -> "+newPriority,currentUsername());
            String msg="Priority of incident '"+incident.getTitle()+"' changed from "+oldPriority+" to "+newPriority+". The SLA deadline was recalculated.";
            notificationService.sendNotification(id,msg,"IN_APP"); notificationService.sendNotification(id,msg,"EMAIL");
        } else if(!java.util.Objects.equals(oldServiceId,newServiceId)) {
            // A different service can have its own SLA
            slaService.calculateDeadline(incident); auditService.recordAction(id,"SLA_RECALCULATED","Affected service changed; SLA now: "+incident.getAppliedSla(),currentUsername());
        }
        Incident saved=incidentRepository.save(incident);
        auditService.recordAction(id,"INCIDENT_UPDATED","Incident details updated",currentUsername());
        return saved;
    }

    /**
     * The assigned technician acknowledges a new incident ("I have seen it and it is mine").
     * It stays OPEN; the time is recorded and counts as the first response for the response SLA.
     */
    @Transactional
    public Incident acknowledge(Long id) {
        Incident incident = getIncidentById(id);
        if (!hasRole("TECHNICIAN")) throw new AccessDeniedException("Only the assigned technician can acknowledge an incident");
        checkCanUpdate(incident);
        if (!IncidentStates.of(incident.getStatus()).canAcknowledge()) throw new IllegalStateException("Only open incidents can be acknowledged");
        if (incident.getAcknowledgedAt() != null) throw new IllegalStateException("This incident was already acknowledged");
        LocalDateTime now = LocalDateTime.now();
        incident.setAcknowledgedAt(now);
        if (incident.getFirstResponseAt() == null) incident.setFirstResponseAt(now);
        Incident saved = incidentRepository.save(incident);
        String technician = saved.getAssignedTechnician() == null ? currentUsername() : saved.getAssignedTechnician().getName();
        auditService.recordAction(id, "ACKNOWLEDGED", technician + " acknowledged the incident", currentUsername());
        if (saved.getReportedBy() != null)
            notificationService.sendNotificationTo(id, "Your incident '" + saved.getTitle() + "' was acknowledged by " + technician + ".", "IN_APP", saved.getReportedBy().getUsername());
        return saved;
    }

    public Incident updateStatus(Long id,String newStatus){return updateStatus(id,newStatus,null);}
    /**
     * Moves an incident to another status. The rules live in the state classes (State pattern, package i_itsm.state):
     * the current state says whether the move is allowed, and the new state updates the incident's dates.
     */
    public Incident updateStatus(Long id,String newStatus,String notes){
        Incident incident=getIncidentById(id);
        IncidentState target=IncidentStates.of(newStatus); // "Status is required" / "Invalid incident status"
        String status=target.name();
        if("IN_PROGRESS".equals(status)&&hasRole("ADMINISTRATOR")) throw new AccessDeniedException("Administrators cannot start work on behalf of technicians");
        checkCanUpdate(incident);
        IncidentState current=IncidentStates.of(incident.getStatus());
        String oldStatus=current.name();
        if(!current.canMoveTo(status)) throw new IllegalStateException("Invalid status transition: "+oldStatus+" -> "+status);
        String cleanNotes=notes==null?"":notes.trim();
        if(target.needsResolutionNotes() && cleanNotes.isEmpty()) throw new IllegalArgumentException("Resolution notes are required to resolve an incident");
        if(cleanNotes.length()>2000) throw new IllegalArgumentException("Resolution notes must not exceed 2000 characters");

        target.enter(incident, LocalDateTime.now());
        incident.setStatus(status);
        if(target.isFinished() && !cleanNotes.isEmpty()) incident.setResolutionNotes(cleanNotes);
        // Finished incidents do not count towards the technician's workload; a reopened one counts again
        if(incident.getAssignedTechnician()!=null && target.isFinished()!=current.isFinished()){
            Technician t=incident.getAssignedTechnician();
            t.setActiveIncidentCount(target.isFinished() ? Math.max(0,t.getActiveIncidentCount()-1) : t.getActiveIncidentCount()+1);
            technicianRepository.save(t);
        }
        Incident saved=incidentRepository.save(incident);
        auditService.recordStatusChange(saved.getId(),"STATUS_UPDATED",oldStatus,status,cleanNotes.isEmpty()?"":cleanNotes,currentUsername());
        notificationService.sendNotification(saved.getId(),"The status of your incident '"+saved.getTitle()+"' has been updated to "+status,"IN_APP");
        notificationService.sendNotification(saved.getId(),"Incident '"+saved.getTitle()+"' status updated to "+status,"EMAIL");
        return saved;
    }

    public List<Incident> searchIncidents(Long incidentId,String priority,String status,String category,Long technicianId,String title){return incidentRepository.searchIncidents(incidentId,priority,status,category,technicianId,title);}
    public List<Incident> getMyIncidents(){User u=currentUser();return u==null?List.of():incidentRepository.findByReportedByOrderByCreatedAtDesc(u);}
}
