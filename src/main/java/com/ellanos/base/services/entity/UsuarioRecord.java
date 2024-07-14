package com.ellanos.base.services.entity;

import org.springframework.data.annotation.Id;

public record UsuarioRecord(@Id Long id, String nombre, String apellido) {} 
