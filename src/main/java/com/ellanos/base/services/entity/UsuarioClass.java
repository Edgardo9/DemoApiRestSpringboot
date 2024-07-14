package com.ellanos.base.services.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "USUARIO")
public class UsuarioClass {
    
    @Id
    private Long id;

    private String nombre;

    private String apellido;

    private String propietario;

    public UsuarioClass(){

    }

    public UsuarioClass(Long id, String nombre, String apellido){
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
    }

    public UsuarioClass(Long id, String nombre, String apellido, String propietario){
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.propietario = propietario;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getPropietario() {
        return propietario;
    }

    public void setPropietario(String propietario) {
        this.propietario = propietario;
    }

}
