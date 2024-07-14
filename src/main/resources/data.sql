CREATE TABLE USUARIO
(
    id bigint auto_increment,
    nombre varchar2(12) NOT NULL,
    apellido varchar2(12) NOT NULL,
    propietario varchar2(12) NOT NULL 
);

insert into USUARIO(nombre, apellido, propietario) values ('Edgardo', 'Llanos', 'ellanos');
insert into USUARIO(nombre, apellido, propietario) values ('Maria', 'Carreno', 'ellanos');