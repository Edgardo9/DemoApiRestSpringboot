# Pasos de ayuda sobre Spring Security

## 01 - Agregar la dependencia SpringSecurity
Se debe agregar la siguiente dependencia en el archivo build.gradle: org.springframework.boot:spring-boot-starter-security

* Una vez agregada la dependecia (independientemente si aun no se configura nada), por defecto Spring agrega seguridad al proyecto. Por ende si se tenia configurados endpoints con anterioridad, las solicitudes a estos fallaran, ya que spring security lo bloqueara.

## 02 - Configuracion para seguridad minima
Se crea una nueva clase en la raiz del proyecto (a nivel del archivo principal de spring), referenciando a que es la configuracion de spring security: "SecurityConfig.java". el cual sera el Java Bean donde configuraremos Spring Security para nuestra aplicacion.

* Agregar a nivel de clase la anotacion "@Configuration". Con esto convirtamos "SecurityConfig" en nuestro Bean de configuracion para Spring Security.
* Se debe crear un metodo de tipo "SecurityFilterChain" el cual debe tener un argumento de tipo "HttpSecurity". Este metodo deberá retornar lo siguiente "return http.build()". Este metodo se debe anotar con "@Bean".Ademas debe lanzar la excepcion "Exception".
* [Nota] La anotación @Configuration le dice a Spring que use esta clase para configurar Spring y Spring Boot. Cualquier Bean especificado en esta clase ahora estará disponible para el motor de configuración automática de Spring.
* [Nota] Spring Security espera que un Bean configure su cadena de filtros. Anotar un método que devuelve SecurityFilterChain con @Bean satisface esta expectativa.

## 03 - Configuracion basica de autenticacion
Hasta ahora hemos iniciado Spring Security, pero en realidad no hemos asegurado nuestra aplicación. se debe actualizar el metodo "SecurityFilterChain", aplicando diferentes metodos pertenecientes el armumento "http". Algunos de estos son:

* authorizeHttpRequests()
* httpBasic()
* csrf()
* El metodo "SecurityFilterChain" inicialmente quedaria asi:
    http
        .authorizeHttpRequests(request -> request
                .requestMatchers("/cashcards/**")
                .authenticated())
        .httpBasic(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable());
    return http.build();

* Con esta configuracion hemos habilitado la autenticación básica, requiriendo que las solicitudes proporcionen un nombre de usuario y contraseña.

## 04 - Usuarios de pruebas
Spring Security tiene la funcionalidad de generar usuarios de prueba, a traves de un metodo "UserDetailsService" en la clase de configuracion "SecurityConfig". Esto simulara las credenciales de un usuario existente en el sistema. Quedando algo asi:
    @Bean
    UserDetailsService testOnlyUsers(PasswordEncoder passwordEncoder) {
        User.UserBuilder users = User.builder();
        UserDetails sarah = users
            .username("sarah1")
            .password(passwordEncoder.encode("abc123"))
            .roles() // No roles for now
            .build();
        return new InMemoryUserDetailsManager(sarah);
    }

* Luego para las pruebas unitarias, las credenciales podrian ser incluidas en el objeto "restTemplate" y su metodo "withBasicAuth()" quedando algo asi:
    ResponseEntity<String> response = restTemplate.withBasicAuth("sarah1", "abc123").getForEntity("/cashcards", String.class);

* [Nota] Valor del usuario deberia estar presente en la bd.

* [Nota] Si no se ha implementado un metodo de tipo "PasswordEncoder", spring security solicitara crear uno, no dejando complira el proyecto. Un metodo basico de este tipo seria algo asi:
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

## 05 - Autorizacion
Aquí implementaremos el control de acceso basado en roles (RBAC).

* Para agregar los roles en la creacion de las credenciales de prueba ficticias del metodo "testOnlyUsers()", se utiliza el el metodo "roles()". Para una correcta comparacion de los roles se crea mas de un usuario. Quedando algo asi el metodo "testOnlyUsers()":
    @Bean
    UserDetailsService testOnlyUsers(PasswordEncoder passwordEncoder) {
        User.UserBuilder users = User.builder();
        UserDetails sarah = users
            .username("sarah1")
            .password(passwordEncoder.encode("abc123"))
            .roles("CARD-OWNER") // new role
            .build();
        UserDetails hankOwnsNoCards = users
            .username("hank-owns-no-cards")
            .password(passwordEncoder.encode("qrs456"))
            .roles("NON-OWNER") // new role
            .build();
        return new InMemoryUserDetailsManager(sarah, hankOwnsNoCards);
    }

* Sin embargo aun falta habilitar el rol señalado en el metodo "SecurityFilterChain()", esto se hace reemplazando el metodo "authenticated()" por el metodo "hasRole()", quedando:
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
             .authorizeHttpRequests(request -> request
                     .requestMatchers("/cashcards/**")
                     .hasRole("CARD-OWNER")) // enable RBAC: Replace the .authenticated() call with the hasRole(...) call.
             .httpBasic(Customizer.withDefaults())
             .csrf(csrf -> csrf.disable());
        return http.build();
    }

* con esta simple configuracion se genera la habilitacion de la autorizacion de tipo RBAC.

## 06 - Ejemplos de pruebas unitarias
A continuacion se presentan ejemplos de la construccion de pruebas unitarias, basandose en la autenticacion y autorizacion:

    @Test
    void shouldNotReturnACashCardWhenUsingBadCredentials() {
        ResponseEntity<String> response = restTemplate
        .withBasicAuth("BAD-USER", "abc123")
        .getForEntity("/cashcards/99", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        response = restTemplate
        .withBasicAuth("sarah1", "BAD-PASSWORD")
        .getForEntity("/cashcards/99", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectUsersWhoAreNotCardOwners() {
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("hank-owns-no-cards", "qrs456")
            .getForEntity("/cashcards/99", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

## 07 - Actualizando el repositorio: Usuarios solo deben ver la informacion que le corresponde.
Como esta el proyecto hasta aqui, si un usuario tiene los privilegios de tipo "CARD-OWNER", podria ver la informacion de tarjeta de otro usuario. Para evitar esto:

* En la interface repositorio, se deberian crear nuevos metodos, los cuales traerán los datos solamente del usuario correspondiente. Aparte de la forma que se muestra a continuacion, tambien se podria hacer a traves de @Query.
* La mayoría de las operaciones de acceso a datos que normalmente activa en un repositorio dan como resultado la ejecución de una consulta en las bases de datos. Definir dicha consulta es cuestión de declarar un método en la interfaz del repositorio

    CashCard findByIdAndOwner(Long id, String owner);
    Page<CashCard> findByOwner(String owner, PageRequest pageRequest);

## 08 - Actualizando el controlador: obtencion de informacion
Para pasar los datos de los usuarios, se utiliza un objeto de tipo "Principal", de esta forma se deberia actualizar los endpoints del controlador, incluyendo este objeto en las solicitudes. Esto objeto se pasa como argumento. La nueva actualizacion del controlador pasandole la referencia a la clase repositorio seria algo asi:

* Para un id en especifico:
    @GetMapping("/{requestedId}")
    private ResponseEntity<CashCard> findById(@PathVariable Long requestedId, Principal principal) {
        Optional<CashCard> cashCardOptional = Optional.ofNullable(cashCardRepository.findByIdAndOwner(requestedId, principal.getName()));
        if (cashCardOptional.isPresent()) {
            return ResponseEntity.ok(cashCardOptional.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

* Para una lista:
    @GetMapping
    private ResponseEntity<List<CashCard>> findAll(Pageable pageable, Principal principal) {
        Page<CashCard> page = cashCardRepository.findByOwner(principal.getName(), 
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC, "amount"))
                ));
        return ResponseEntity.ok(page.getContent());
    }

## 09 - Actualizando el controlador: creacion de registros
Si enviamos manualmente el valor de un propietario, Corremos el riesgo de permitir que los usuarios creen registros para otra persona. Asegurémonos de que solo el "Principal" autorizado y autenticado sea propietario de los registros que está creando:

    @PostMapping
    private ResponseEntity<Void> createCashCard(@RequestBody CashCard newCashCardRequest, UriComponentsBuilder ucb, Principal principal) {
        CashCard cashCardWithOwner = new CashCard(null, newCashCardRequest.amount(), principal.getName());
        CashCard savedCashCard = cashCardRepository.save(cashCardWithOwner);
        URI locationOfNewCashCard = ucb
                .path("cashcards/{id}")
                .buildAndExpand(savedCashCard.id())
                .toUri();
        return ResponseEntity.created(locationOfNewCashCard).build();
    }

* [Nota] El objeto "Principal" será el usuario que ha iniciado sesión.

## 10 - Sobre CSRF
Como aprendimos en la lección adjunta, la protección contra la falsificación de solicitudes entre sitios (CSRF o "sea-surf") es un aspecto importante de las API basadas en HTTP utilizadas por las aplicaciones basadas en web. Sin embargo, hemos desactivado CSRF mediante el código csrf.disable() en SecurityConfig.filterChain:

* [Nota] ¿Cuándo debería utilizar la protección CSRF? La recomendación es utilizar protección CSRF para cualquier solicitud que un usuario normal pueda procesar mediante un navegador. Si solo está creando un servicio que utilizan clientes que no son de navegador, es probable que desee desactivar la protección CSRF.










