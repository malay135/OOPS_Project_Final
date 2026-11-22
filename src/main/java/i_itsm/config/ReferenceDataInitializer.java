package i_itsm.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import i_itsm.model.ITService;
import i_itsm.model.IncidentCategory;
import i_itsm.model.SLAPolicy;
import i_itsm.repository.ITServiceRepository;
import i_itsm.repository.IncidentCategoryRepository;
import i_itsm.repository.SLAPolicyRepository;

@Configuration
public class ReferenceDataInitializer {
    @Bean
    CommandLineRunner initializeReferenceData(ITServiceRepository services, IncidentCategoryRepository categories, SLAPolicyRepository policies) {
        return args -> {
            String[][] serviceData={{"Employee Portal","Internal employee service portal"},{"Payment Gateway","Payment processing service"},{"Corporate Network","Office network and connectivity"},{"Database","Shared database infrastructure"},{"Authentication","Login and identity service"},{"Cloud Storage","Managed cloud file storage"}};
            for(String[] row:serviceData) if(services.findByNameIgnoreCase(row[0]).isEmpty()) services.save(new ITService(row[0],row[1]));
            String[] cats={"NETWORK","HARDWARE","SOFTWARE","DATABASE","SECURITY","OTHER"};
            for(String c:cats) if(categories.findByNameIgnoreCase(c).isEmpty()) categories.save(new IncidentCategory(c,"Default " + c.toLowerCase() + " category"));
            int[][] sla={{15,240,30},{30,480,60},{60,1440,180},{120,4320,360}};
            String[] p={"P1","P2","P3","P4"};
            for(int i=0;i<p.length;i++) if(policies.findByPriority(p[i]).isEmpty()){SLAPolicy s=new SLAPolicy();s.setPriority(p[i]);s.setResponseTimeMinutes(sla[i][0]);s.setResolutionTimeMinutes(sla[i][1]);s.setWarningTimeMinutes(sla[i][2]);s.setActive(true);policies.save(s);}
        };
    }
}
