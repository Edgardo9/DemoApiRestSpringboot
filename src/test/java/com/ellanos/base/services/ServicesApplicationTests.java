package com.ellanos.base.services;

//import java.net.URI;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import com.ellanos.base.services.entity.UsuarioClass;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class ServicesApplicationTests {

	@Test
	void contextLoads() {
	}

	@Autowired
    TestRestTemplate restTemplate;

	@Test
	@DirtiesContext
	void shouldCreateANewUser() {
		UsuarioClass user = new UsuarioClass();
		user.setNombre("Fabian");
		user.setApellido("Llanos");

		ResponseEntity<Void> createResponse = restTemplate.withBasicAuth("ellanos", "qwerty").postForEntity("/base/usuario", user, Void.class);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
	}

	@Test
	void shouldShowUserById() {
		ResponseEntity<String> response = restTemplate.withBasicAuth("ellanos", "abc1234").getForEntity("/base/usuario/1", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
    void shouldNotReturnACashCardWhenUsingBadCredentials() {
        ResponseEntity<String> response = restTemplate
        .withBasicAuth("BAD-USER", "qwerty")
        .getForEntity("/base/usuario/1", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        response = restTemplate
        .withBasicAuth("ellanos", "BAD-PASSWORD")
        .getForEntity("/base/usuario/1", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectUsersWhoAreNotCardOwners() {
        ResponseEntity<String> response = restTemplate
            .withBasicAuth("hank-owns-no-cards", "qrs456")
            .getForEntity("/base/usuario/1", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

	@Test
    @DirtiesContext
    void shouldUpdateAnExistingUser() {
        UsuarioClass user = new UsuarioClass(null, "Barbara", "Carreno");
        HttpEntity<UsuarioClass> request = new HttpEntity<>(user);
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth("ellanos", "qwerty")
                .exchange("/base/usuario/1", HttpMethod.PUT, request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> responseGet = restTemplate
                .withBasicAuth("ellanos", "qwerty")
                .getForEntity("/base/usuario/1",String.class);
		assertThat(responseGet.getStatusCode()).isEqualTo(HttpStatus.OK);
		
        DocumentContext context = JsonPath.parse(responseGet.getBody());
		String nombre = context.read("$.nombre");
        String apellido = context.read("$.apellido");
        assertThat(nombre).isEqualTo("Barbara");
        assertThat(apellido).isEqualTo("Carreno");
    }

    @Test
    void shouldNotUpdateUserThatDoesNotExist() {
        UsuarioClass user = new UsuarioClass(null, "Moroco", "Tejos");
        HttpEntity<UsuarioClass> request = new HttpEntity<>(user);
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth("ellanos", "qwerty")
                .exchange("/base/usuario/999999", HttpMethod.PUT, request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotUpdateUserThatIsOwnedBySomeoneElse() {
        UsuarioClass user = new UsuarioClass(null, "Moroco", "Tejos");
        HttpEntity<UsuarioClass> request = new HttpEntity<>(user);
        ResponseEntity<Void> response = restTemplate
                .withBasicAuth("mcarreno", "qwerty")
                .exchange("/base/usuario/1", HttpMethod.PUT, request, Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldDeleteAnExistingUser() {
        ResponseEntity<Void> responseDel = restTemplate
                .withBasicAuth("ellanos", "qwerty")
                .exchange("/base/usuario/1", HttpMethod.DELETE, null, Void.class);
        assertThat(responseDel.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> responseGet = restTemplate
                .withBasicAuth("ellanos", "qwerty")
                .getForEntity("/base/usuario/1", String.class);
        assertThat(responseGet.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotDeleteAUsuarioThatDoesNotExist() {
        ResponseEntity<Void> responseDel = restTemplate
                .withBasicAuth("ellanos", "qwerty")
                .exchange("/base/usuario/77777", HttpMethod.DELETE, null, Void.class);
        assertThat(responseDel.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldNotDeleteAUsuarioThatIsOwnedBySomeoneElse() {
        ResponseEntity<Void> responseDel = restTemplate
                .withBasicAuth("mcarreno", "qwerty")
                .exchange("/base/usuario/1", HttpMethod.DELETE, null, Void.class);
        assertThat(responseDel.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

}
