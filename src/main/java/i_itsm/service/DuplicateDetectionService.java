package i_itsm.service;

import java.util.*;
import org.springframework.stereotype.Service;
import i_itsm.model.Incident;
import i_itsm.notification.NotificationService;
import i_itsm.repository.IncidentRepository;

@Service
public class DuplicateDetectionService {
    private final IncidentRepository repository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    public DuplicateDetectionService(IncidentRepository r, NotificationService n, AuditService a) { repository=r; notificationService=n; auditService=a; }
    private double similarity(String a, String b) { return TextSimilarity.similarity(a, b); } // shared with suggested resolutions
    public void check(Incident created) {
        List<Incident> candidates=repository.findAll().stream().filter(i -> !i.getId().equals(created.getId())).filter(i -> i.getResolvedAt()==null).filter(i -> Objects.equals(i.getCategory(), created.getCategory())).filter(i -> i.getCreatedAt() != null && created.getCreatedAt() != null)
                .filter(i -> Math.abs(java.time.Duration.between(i.getCreatedAt(), created.getCreatedAt()).toHours()) <= 72).toList();
        Incident best=null; double score=0; for(Incident i:candidates){double s=0.65*similarity(created.getTitle(),i.getTitle())+0.35*similarity(created.getDescription(),i.getDescription()); if(s>score){score=s;best=i;}}
        if(best!=null && score>=0.45){created.setPotentialDuplicate(true);created.setDuplicateOfIncidentId(best.getId());repository.save(created);String msg="Potential duplicate detected for incident #"+created.getId()+". Similar incident: #"+best.getId()+" (score "+String.format("%.0f%%",score*100)+").";notificationService.sendNotification(created.getId(),msg,"IN_APP");auditService.recordAction(created.getId(),"POTENTIAL_DUPLICATE",msg,"system");}
    }
}
