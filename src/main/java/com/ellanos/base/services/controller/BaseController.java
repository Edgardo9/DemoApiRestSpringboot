package com.ellanos.base.services.controller;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.ellanos.base.services.entity.UsuarioClass;
import com.ellanos.base.services.repository.UsuarioRepository;
import com.ellanos.base.services.services.UsuarioServices;


@RestController
@RequestMapping("/base")
public class BaseController {

    @Autowired
    private UsuarioServices usuarioServices;

    @Autowired
    UsuarioRepository repo;

    @GetMapping("/demo")
    private String getDemo() {
        return "HolaCTM";
    }

    @GetMapping("/usuario/{id}")
    private ResponseEntity<Object> getUsuario(@PathVariable(name = "id") Long id, Principal principal){
        if (existeUsuario(id, principal)) {
            return ResponseEntity.ok(usuarioServices.findByIdAndOwner(id, principal.getName()));
        }
        return ResponseEntity.notFound().build();
        
    }    
    
    @PostMapping("/usuario")
    private ResponseEntity<Void> saveUser(@RequestBody UsuarioClass uClass, UriComponentsBuilder ucb, Principal principal){
        UsuarioClass dataUser = new UsuarioClass(null, uClass.getNombre(), uClass.getApellido(), principal.getName());
        UsuarioClass newUser = usuarioServices.save(dataUser, principal);
        URI location = ucb
            .path("usuario/{id}")
            .buildAndExpand(newUser.getId()).toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/usuario/all")
    private ResponseEntity<List<UsuarioClass>> findAll(Pageable pageable, Principal principal){
        return ResponseEntity.ok(usuarioServices.findByOwner(pageable, principal.getName()));
    }

    @PutMapping("/usuario/{id}")
    public ResponseEntity<Void> updateUser(@PathVariable(name = "id") Long id, @RequestBody UsuarioClass newDataUser, Principal principal) {
        UsuarioClass userActual = usuarioServices.findByIdAndOwner(id, principal.getName());
        if(userActual != null) {
            UsuarioClass userUpdate = new UsuarioClass(userActual.getId(), newDataUser.getNombre(), newDataUser.getApellido(), principal.getName());
            usuarioServices.save(userUpdate, principal);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.notFound().build();   
    }

    @DeleteMapping("/usuario/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable(name = "id") Long id, Principal principal){
        if (existeUsuario(id, principal)) {
            usuarioServices.deleteUser(id);
            return ResponseEntity.noContent().build();
        }
        
        return ResponseEntity.notFound().build();
    }

    public boolean existeUsuario(Long id, Principal principal){
        UsuarioClass usuario = usuarioServices.findByIdAndOwner(id, principal.getName());
        return usuario != null;
    }

}
