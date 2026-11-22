package i_itsm.controller;

import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import i_itsm.model.User;
import i_itsm.repository.UserRepository;
import i_itsm.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserAdminController {
    private final UserRepository repository; private final PasswordEncoder encoder;
    public UserAdminController(UserRepository r, PasswordEncoder e){repository=r;encoder=e;}
    @GetMapping public List<Map<String,Object>> getAll(){return repository.findAll().stream().map(u -> Map.<String,Object>of("id",u.getId(),"username",u.getUsername(),"role",u.getRole(),"email",u.getEmail()==null?"":u.getEmail())).toList();}
    @PutMapping("/{id}") public Map<String,Object> update(@PathVariable Long id,@RequestBody Map<String,String> body){
        User u=repository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found: "+id));
        String role=body.get("role"); if(role!=null && !java.util.Set.of("EMPLOYEE","TECHNICIAN","ADMINISTRATOR").contains(role)) throw new IllegalArgumentException("Invalid role");
        if(role!=null)u.setRole(role); if(body.containsKey("email"))u.setEmail(UserService.cleanEmail(body.get("email"))); if(body.get("password")!=null && !body.get("password").isBlank())u.setPassword(encoder.encode(body.get("password")));
        User saved=repository.save(u); return Map.of("id",saved.getId(),"username",saved.getUsername(),"role",saved.getRole(),"email",saved.getEmail()==null?"":saved.getEmail());
    }
}
