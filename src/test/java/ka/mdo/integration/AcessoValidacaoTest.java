package ka.mdo.integration;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import ka.mdo.model.Perfil;
import ka.mdo.testsupport.TestDataSeeder;
import ka.mdo.testsupport.TestJwt;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Validação de acesso pelo aparelho cobrindo os três resultados possíveis
 * ({@code AUTORIZADO}, {@code NEGADO}, {@code PENDENTE}) — atividade 051.
 *
 * <p>O token JWT é mintado com o papel {@code OPERADOR_APARELHO} e a empresa
 * semeada, exercitando {@code @RolesAllowed} e o {@code TenantRequestFilter}.
 */
@QuarkusTest
class AcessoValidacaoTest {

    @Inject
    TestDataSeeder seeder;

    @Test
    void acessoSemEventoNemLocalRetornaAutorizado() {
        Long empresa = seeder.criarEmpresa("Empresa Autorizado");
        Long aparelho = seeder.criarAparelho(empresa, null);
        String token = seeder.criarIngresso(empresa);
        String auth = TestJwt.bearer(empresa, 1L, Perfil.OPERADOR_APARELHO.name());

        given()
                .header("Authorization", auth)
                .contentType("application/json")
                .body(req(token, aparelho))
                .when()
                .post("/acesso/validar")
                .then()
                .statusCode(200)
                .body("resultado", equalTo("AUTORIZADO"))
                .body("motivo", equalTo("AUTORIZADO"));
    }

    @Test
    void tokenInexistenteRetornaNegado() {
        Long empresa = seeder.criarEmpresa("Empresa Negado");
        Long aparelho = seeder.criarAparelho(empresa, null);
        String auth = TestJwt.bearer(empresa, 1L, Perfil.OPERADOR_APARELHO.name());

        given()
                .header("Authorization", auth)
                .contentType("application/json")
                .body(req("token-que-nao-existe", aparelho))
                .when()
                .post("/acesso/validar")
                .then()
                .statusCode(200)
                .body("resultado", equalTo("NEGADO"))
                .body("motivo", equalTo("CREDENCIAL_INEXISTENTE"));
    }

    @Test
    void eventoComValidacaoFacialSemFotoRetornaPendente() {
        Long empresa = seeder.criarEmpresa("Empresa Pendente");
        Long evento = seeder.criarEventoFacial(empresa);
        Long aparelho = seeder.criarAparelho(empresa, evento);
        String token = seeder.criarIngresso(empresa);
        String auth = TestJwt.bearer(empresa, 1L, Perfil.OPERADOR_APARELHO.name());

        given()
                .header("Authorization", auth)
                .contentType("application/json")
                .body(req(token, aparelho))
                .when()
                .post("/acesso/validar")
                .then()
                .statusCode(200)
                .body("resultado", equalTo("PENDENTE"))
                .body("motivo", equalTo("FOTO_FACIAL_AUSENTE"));
    }

    private static Map<String, Object> req(String token, Long aparelhoId) {
        Map<String, Object> m = new HashMap<>();
        m.put("token", token);
        m.put("aparelhoId", aparelhoId);
        return m;
    }
}
