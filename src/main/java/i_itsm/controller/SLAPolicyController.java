package i_itsm.controller;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import i_itsm.model.SLAPolicy;
import i_itsm.model.ServiceSlaPolicy;
import i_itsm.service.SLAPolicyService;

@RestController
@RequestMapping("/api/sla-policies")
public class SLAPolicyController {
    private final SLAPolicyService service;
    public SLAPolicyController(SLAPolicyService service){this.service=service;}
    @GetMapping public List<SLAPolicy> getAll(){return service.getAll();}
    @PutMapping("/{priority}") public SLAPolicy upsert(@PathVariable String priority,@RequestBody SLAPolicy policy){return service.upsert(priority,policy);}

    // Service-specific SLAs (admin only, see SecurityConfig). They replace the standard SLA for that service.
    @GetMapping("/services") public List<ServiceSlaPolicy> servicePolicies(){return service.getServicePolicies();}

    @PutMapping("/services/{serviceId}/{priority}")
    public ServiceSlaPolicy upsertServicePolicy(@PathVariable Long serviceId,@PathVariable String priority,@RequestBody SLAPolicy times){
        return service.upsertServicePolicy(serviceId,priority,times);
    }

    @DeleteMapping("/services/{serviceId}/{priority}")
    public Map<String,String> deleteServicePolicy(@PathVariable Long serviceId,@PathVariable String priority){
        service.deleteServicePolicy(serviceId,priority);
        return Map.of("message","Service SLA removed; the standard SLA applies again");
    }
}
