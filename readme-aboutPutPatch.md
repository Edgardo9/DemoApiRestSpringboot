# Pasos de ayuda teoricos sobre metodos put y patch

## 01 - Intro: Sobre put y patch
Ambos PUT y PATCH pueden usarse para actualizar, pero funcionan de diferentes maneras. Básicamente, PUTsignifica "crear o reemplazar el registro completo", mientras que PATCHsignifica "actualizar sólo algunos campos del registro existente"; en otras palabras, una actualización parcial.

¿Por qué querrías hacer una actualización parcial? Las actualizaciones parciales liberan al cliente de tener que cargar el registro completo y luego transmitirlo nuevamente al servidor. Si el registro es lo suficientemente grande, esto puede tener un impacto no trivial en el rendimiento.

* [Nota] ¡el estándar HTTP no especifica si se prefiere el verbo POST o PUT para una operación Crear! Esto es relevante porque si usaramos el verbo "PUT" para nuestro endpoint de Actualización, debemos decidir si nuestra API admitirá el uso PUT para Crear o Actualizar un recurso.

## 02 - Intro: ¿Por qué querríamos utilizar una operación PUT para crear un recurso? 
Esto tiene que ver con la definición HTTP de los dos verbos. La diferencia es sutil. Vamos a explicarlo comparando dos sistemas diferentes: nuestra API de tarjeta de efectivo y otra API que presentaremos con fines explicativos, llamada API de factura . La API de factura acepta el número de factura como identificador único. Este es un ejemplo del uso de una clave natural (proporcionada por el cliente a la API) en lugar de una clave sustituta (normalmente generada por el servidor, que es lo que estamos haciendo en nuestra API de tarjeta de efectivo).

La diferencia importante es si el servidor debe generar el URI (que incluye el ID del recurso) o no. Así es como lo piensan PUT y POST:

Si necesita que el servidor devuelva el URI del recurso creado (o los datos que usa para construir el URI), entonces debe usar POST.

En resumen:
* POST crea un recurso; y dentro del encabezado de la respuesta, el recurso creado contiene una ID generada.
* PUT Crea o reemplaza (actualiza) un recurso en un URI de solicitud específico.


# Implementacion en Laboratorio:

## 01 - (Opcional) Implementando un test unitario en donde se solo se espera un 204
Se debe tener en cuenta que como usará el metodo ut se debe eserar un 204, lo cual indica que el cliente no necesita realizar ninguna otra accion. Prueba realizada:
    @Test
    @DirtiesContext
    void shouldUpdateAnExistingCashCard() {
        CashCard cashCardUpdate = new CashCard(null, 19.99, null);
        HttpEntity<CashCard> request = new HttpEntity<>(cashCardUpdate);
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .exchange("/cashcards/99", HttpMethod.PUT, request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

* Nota, en las pruebas enteriores cuando se utilizaba restTemplate, se utilizada con los metodos getForEntity() o postForEntity(). Para el metodo put no es asi, no existe un putForEntity(), en su lugar se debera utilizar un "exchange()".

## 02 - Implementando endpoint de tipo @PutMapping()
Para generar este tipo de endpoints, primeramente se debe usar la anotacion @PutMapping(), la cual recibe como parametro el id del recurso a modificar. Por ende el metodo controlador debera recibir como argumento el mismo id, ademas del objeto con los cambios que serán actualizados. Ejemplo del metodo:
    @PutMapping("/{requestedId}")
    private ResponseEntity<Void> putCashCard(@PathVariable Long requestedId, @RequestBody CashCard cashCardUpdate) {
        // just return 204 NO CONTENT for now.
        return ResponseEntity.noContent().build();
    }

* Nota, hasta este punto solo se esta retornando un 204, la actualizacion aun no ha sido implementada.

## 03 - (Opcional) actualizacion del test unitario: se espera un 204 y que el recurso sea actualizado.
Esta vez si se agrega una validacion, en donde se vuelve a llamar al recurso a traves del metodo get, en donde se verifica que la data haya sido modificada:
    @Test
    @DirtiesContext
    void shouldUpdateAnExistingCashCard() {
        CashCard cashCardUpdate = new CashCard(null, 19.99, null);
        HttpEntity<CashCard> request = new HttpEntity<>(cashCardUpdate);
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .exchange("/cashcards/99", HttpMethod.PUT, request, Void.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Lo Actualizado
        ResponseEntity<String> getResponse = restTemplate
          .withBasicAuth("sarah1", "abc123")
          .getForEntity("/cashcards/99", String.class);
        
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext documentContext = JsonPath.parse(getResponse.getBody());
        Number id = documentContext.read("$.id");
        Double amount = documentContext.read("$.amount");
        assertThat(id).isEqualTo(99);
        assertThat(amount).isEqualTo(19.99);
    }

* hasta este punto la prueba deberia fallar, ya que aun no se ha actualizado el metodo controlador.


## 04 - Actualizando endpoint
Primeramente se agregará seguridad al metodo, por lo tanto solamente el owner correcto podrá hacer modificaciones del registro. Para lograr esto, se debe agregar un argumento de tipo "Principal" al metodo. Luego se deberá llamar al metodo de actualizacion desde el repository, desde este se obtendran los datos correspondientes al objeto que será actualizado. Luego a partir de los datos obtenidos (id encontrado, nuevo objeto con la actualizacion de los datos y el objeto proincipal) se crea un nuevo objeto, el cual debe ser usado en el metodo save() del repository, quedando:
    @PutMapping("/{requestedId}")
    private ResponseEntity<Void> putCashCard(@PathVariable Long requestedId, @RequestBody CashCard cashCardUpdate, Principal principal) {
        
        CashCard cashCard = cashCardRepository.findByIdAndOwner(requestedId, principal.getName());
        CashCard updatedCashCard = new CashCard(cashCard.id(), cashCardUpdate.amount(), principal.getName());
        cashCardRepository.save(updatedCashCard);

        return ResponseEntity.noContent().build();
    }

## 05 - (Opcional) Pruebas adicionales e influencia de Spring Security

### intento de actualizacion cuando un recurso no existe
Prueba unitaria:
    @Test
    void shouldNotUpdateACashCardThatDoesNotExist() {
        CashCard unknownCard = new CashCard(null, 19.99, null);
        HttpEntity<CashCard> request = new HttpEntity<>(unknownCard);
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .exchange("/cashcards/99999", HttpMethod.PUT, request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

* Nota: Al generar una prueba con el intento de actualizacion de un recurso que no existe, actualemente esta dando un error 403 en vez de un 404 o un error 500 (ya que es por nullpointer exception). Es basicamente por que springSecurity se hace cargo del manejo de errores.

* Para manejar el error "manualmente", se deberá utilizar un if en donde evalue si el objeto consultado es nulo, devuelva un 404 not_found:
    @PutMapping("/{requestedId}")
    private ResponseEntity<Void> putCashCard(@PathVariable Long requestedId, @RequestBody CashCard cashCardUpdate, Principal principal) {
        CashCard cashCard = cashCardRepository.findByIdAndOwner(requestedId, principal.getName());
        if(cashCard != null) {
            CashCard updatedCashCard = new CashCard(cashCard.id(), cashCardUpdate.amount(), principal.getName());
            cashCardRepository.save(updatedCashCard);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

### intento de actualizacion de un recurso que pertenece a otro propietario
Prueba unitaria hasta este punto esta dando el mismo resultado que la prueba anterior. Y ya que son escenarios diferentes, aunq con el mismo resultado, es bueno mantenerlos separados por si en el futuro alguien decide implementar un comportamiento diferente. Prueba:
    @Test
    void shouldNotUpdateACashCardThatIsOwnedBySomeoneElse() {
        CashCard kumarsCard = new CashCard(null, 333.33, null);
        HttpEntity<CashCard> request = new HttpEntity<>(kumarsCard);
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth("sarah1", "abc123")
                .exchange("/cashcards/102", HttpMethod.PUT, request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

## Recordar Refactorizar el codigo!!!

