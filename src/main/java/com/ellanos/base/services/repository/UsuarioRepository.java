package com.ellanos.base.services.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.ellanos.base.services.entity.UsuarioClass;

public interface UsuarioRepository extends CrudRepository<UsuarioClass, Long>, PagingAndSortingRepository<UsuarioClass, Long> {

    UsuarioClass findByIdAndPropietario(Long id, String owner);
    Page<UsuarioClass> findByPropietario(Pageable pageable, String owner);

}