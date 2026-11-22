package i_itsm.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import i_itsm.model.IncidentCategory;
import i_itsm.service.IncidentCategoryService;

@RestController
@RequestMapping("/api/categories")
public class IncidentCategoryController {
    private final IncidentCategoryService service;
    public IncidentCategoryController(IncidentCategoryService service){this.service=service;}
    @GetMapping public List<IncidentCategory> getAll(){return service.getAll();}
    @PostMapping public IncidentCategory create(@RequestBody IncidentCategory c){return service.create(c);}
    @PutMapping("/{id}") public IncidentCategory update(@PathVariable Long id,@RequestBody IncidentCategory c){return service.update(id,c);}
}
