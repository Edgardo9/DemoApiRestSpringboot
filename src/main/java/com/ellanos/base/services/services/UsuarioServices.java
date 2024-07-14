package com.ellanos.base.services.services;

import java.security.Principal;
import java.util.List;

import org.springframework.data.domain.Pageable;

import com.ellanos.base.services.entity.UsuarioClass;

public interface UsuarioServices {

    UsuarioClass findByIdAndOwner(Long id, String owner);
    UsuarioClass save(UsuarioClass usuario, Principal principal);
    List<UsuarioClass> findByOwner(Pageable pageable, String owner);
    Void deleteUser(Long id);
    
}