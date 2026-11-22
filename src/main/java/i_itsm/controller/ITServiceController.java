package i_itsm.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import i_itsm.model.ITService;
import i_itsm.service.ITServiceService;

@RestController
@RequestMapping("/api/services")
public class ITServiceController {
    private final ITServiceService service;
    public ITServiceController(ITServiceService service){this.service=service;}
    @GetMapping public List<ITService> getAll(){return service.getAll();}
    @PostMapping public ITService create(@RequestBody ITService s){return service.create(s);}
    @PutMapping("/{id}") public ITService update(@PathVariable Long id,@RequestBody ITService s){return service.update(id,s);}
}
