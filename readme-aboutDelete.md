# Pasos de ayuda teoricos sobre metodo delete

## Intro: Antes de Implementar Delete
Al igual que con la actualización (Se debia tener en cuenta si se eliminaba por completo el registro y se creaba otro  con el put o se haria una actualizacion con datos parciales con patch) se debe hacer la consulta que impicará eliminar un registro.

* Una opcion sencilla, es la llamada eleminacion definitiva (hard delete), la cual consiste en eliminar directamente en BD. Desapareciendo para siempre.
* Una alternativa a lo anterior es la eliminacion suave (soft delete), la cual marca los registros como "Eliminados" en la BD. de esta manera no se eliminan, solo se marcan. Coneste tipo de eliminacion es necesaio modificar la forma en que los repositorios interactuan con la BD. Por ejemplo, un repositorio debe respetar la columna "eliminado" y excluir los registros marcados como eliminados de las solicitudes de lectura.

* Al trabajar con bases de datos, se dará cuenta de que, a menudo, es necesario mantener un registro de las modificaciones de los registros de datos. Por ejemplo:
Es posible que un representante de servicio al cliente necesite saber cuándo un cliente eliminó su tarjeta de efectivo.
Es posible que existan regulaciones de cumplimiento de retención de datos que requieran que los datos eliminados se conserven durante un período de tiempo determinado.

* Se recomienda que se mantenga un registro de auditoría. El registro de auditoría es un registro de todas las operaciones importantes realizadas en un registro. Puede incluir no solo operaciones de eliminación, sino también de creación y actualización.

* La ventaja de un "registro de auditoría" sobre los "campos de auditoría" es que un registro registra todos los eventos, mientras que los campos de auditoría del registro capturan solo la operación más reciente. Un registro de auditoría se puede almacenar en una ubicación de base de datos diferente o incluso en archivos de registro.

## Implementando Delete

### 01 - (Opcional) Agregando test unitario de camino feliz
En el archivo de pruebas, se agrega el siguient test unitario:

* Como se observa se sigue utilizando el metodo "exchange()".
    @Test
    @DirtiesContext
    void shouldDeleteAnExistingCashCard() {
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .exchange("/cashcards/99", HttpMethod.DELETE, null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

* hasta aqui la rueba debveria fallar ya que aun no se ha creado el endpoint correspondiente.

### 02 - Implementando el metodo controlador:
Se implememta el metodo controlador, para satisfacer la prueba anterior:
    @DeleteMapping("/{id}")
    private ResponseEntity<Void> deleteCashCard(@PathVariable Long id) {
        return ResponseEntity.noContent().build();
    }

* sin embargo lo unico que esta haciendo es retornar una respuesta "sin contenido", no elimina el registro.

### 03 - (Opcional) Actualizando test unitario que compruebe la eliminacion del registro
Prueba actualizada:
    @Test
    @DirtiesContext
    void shouldDeleteAnExistingCashCard() {
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .exchange("/cashcards/99", HttpMethod.DELETE, null, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Add the following code:
        ResponseEntity<String> getResponse = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .getForEntity("/cashcards/99", String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

* esta vez se agrega una segunda validacion, consultando por el registro que deberia haber sido eliminado, pero sigue estando presente. por lo que la prueba fallara nuevamente.

### 04 - Implementando La logica de eliminacion
    @DeleteMapping("/{id}")
    private ResponseEntity<Void> deleteCashCard(@PathVariable Long id) {
        cashCardRepository.deleteById(id); // Se actualizó esta linea
        return ResponseEntity.noContent().build();
    }

### 05 - (Opcional) Se agrega nuevo test unitario: registro no existente / usuario no autorizado
Se prepara nueva prueba, para asegurar el comportamiento al tratar de eliminar un registro cuyo id sea inexistente.
    
    @Test
    void shouldNotDeleteACashCardThatDoesNotExist() {
        ResponseEntity<Void> deleteResponse = restTemplate
            .withBasicAuth("sarah1", "abc123")
            .exchange("/cashcards/99999", HttpMethod.DELETE, null, Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

* hasta este punto la prueba fallara, ya que no se esta agregando seguridad al metodo.

### 06 - Implementando La seguridad en el nuevo endpoint:
Se agrega seguridad al metodo controlador:
    @DeleteMapping("/{id}")
    private ResponseEntity<Void> deleteCashCard(@PathVariable Long id, Principal principal) {
        if (!cashCardRepository.existsByIdAndOwner(id, principal.getName())) {
            return ResponseEntity.notFound().build();
        }
        cashCardRepository.deleteById(id); // Add this line
        return ResponseEntity.noContent().build();
    }

* Codigo refactorizado:
     @DeleteMapping("/{id}")
    private ResponseEntity<Void> deleteCashCard(@PathVariable Long id, Principal principal) {
        if (cashCardRepository.existsByIdAndOwner(id, principal.getName())) {
            cashCardRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

* Hasta este punto ya las pruebas no deberian fallar.

* [nota] es correcto que los errores sean 404, para asi no entregar contexto de los registros a los posibles atacantes de cuando es un error de que el registro es inexistente o de cuando es por eque el usuario no es el correcto.


### 05 - (Opcional) Se agrega nuevo test unitario: ocultar registros no autorizados
    @Test
    void shouldNotAllowDeletionOfCashCardsTheyDoNotOwn() {
        ResponseEntity<Void> deleteResponse = restTemplate
            .withBasicAuth("sarah1", "abc123")
            .exchange("/cashcards/102", HttpMethod.DELETE, null, Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> getResponse = restTemplate
            .withBasicAuth("kumar2", "xyz789")
            .getForEntity("/cashcards/102", String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

* [Nota] Es importante considerar todos los escenarios de pruebas, a pesar de que ya en el punto anterior se habia abordado lo de la autorizacion igual se deberia agregar esta ultima validacion