package ka.mdo.integration;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;

/**
 * Login e emissão de JWT (atividade 051).
 *
 * <p>Usa o SUPER_ADMIN de bootstrap (criado no startup pelo
 * {@code BootstrapService}) — não depende de dados semeados.
 */
@QuarkusTest
class AuthLoginTest {

    @Test
    void loginValidoRetorna200ComTokenNoHeader() {
        given()
                .contentType("application/json")
                .body(Map.of("login", "admin@local-acess.local", "senha", "admin123"))
                .when()
                .post("/auth")
                .then()
                .statusCode(200)
                .header("Authorization", not(emptyOrNullString()));
    }

    @Test
    void loginComSenhaInvalidaRetorna204() {
        given()
                .contentType("application/json")
                .body(Map.of("login", "admin@local-acess.local", "senha", "senha-errada"))
                .when()
                .post("/auth")
                .then()
                .statusCode(204);
    }
}
