package com.ellanos.base.services.services;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Service;

import com.ellanos.base.services.entity.UsuarioClass;
import com.ellanos.base.services.repository.UsuarioRepository;

@Service
public class ImplUsuarioServices implements UsuarioServices{

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UsuarioClass findByIdAndOwner(Long id, String owner) {
        UsuarioClass res = usuarioRepository.findByIdAndPropietario(id, owner);
        return res;
    }

    @Override
    public UsuarioClass save(UsuarioClass uClass, Principal principal) {
        UsuarioClass newUserWithOwner = new UsuarioClass(uClass.getId(), uClass.getNombre(), uClass.getApellido(), principal.getName());
        UsuarioClass newUser = usuarioRepository.save(newUserWithOwner);
        return newUser;
    }

    @Override
    public List<UsuarioClass> findByOwner(Pageable pageable, String owner) {
        Page<UsuarioClass> page = usuarioRepository.findByPropietario(
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.DESC, "id"))
        ), owner);
        return page.getContent();
    }

    @Override
    public Void deleteUser(Long id) {
        usuarioRepository.deleteById(id);
        return null;
    }

}
