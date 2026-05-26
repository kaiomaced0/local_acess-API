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
 * Isolamento entre empresas (atividade 051).
 *
 * <p>A mesma credencial (token) é autorizada pelo operador do tenant dono e
 * tratada como inexistente pelo operador de outro tenant — provando que o
 * {@code tenantFilter} do Hibernate isola as consultas por empresa.
 */
@QuarkusTest
class MultitenancyIsolationTest {

    @Inject
    TestDataSeeder seeder;

    @Test
    void credencialDeUmTenantNaoEhVisivelPorOutro() {
        // Tenant A: dono da credencial.
        Long empresaA = seeder.criarEmpresa("Empresa A");
        Long aparelhoA = seeder.criarAparelho(empresaA, null);
        String tokenA = seeder.criarIngresso(empresaA);

        // Tenant B: operador e aparelho próprios, sem nenhuma credencial.
        Long empresaB = seeder.criarEmpresa("Empresa B");
        Long aparelhoB = seeder.criarAparelho(empresaB, null);

        String authA = TestJwt.bearer(empresaA, 1L, Perfil.OPERADOR_APARELHO.name());
        String authB = TestJwt.bearer(empresaB, 2L, Perfil.OPERADOR_APARELHO.name());

        // Controle: o operador do tenant dono autoriza o acesso.
        given()
                .header("Authorization", authA)
                .contentType("application/json")
                .body(req(tokenA, aparelhoA))
                .when()
                .post("/acesso/validar")
                .then()
                .statusCode(200)
                .body("resultado", equalTo("AUTORIZADO"));

        // Isolamento: o operador do outro tenant não enxerga a credencial.
        given()
                .header("Authorization", authB)
                .contentType("application/json")
                .body(req(tokenA, aparelhoB))
                .when()
                .post("/acesso/validar")
                .then()
                .statusCode(200)
                .body("resultado", equalTo("NEGADO"))
                .body("motivo", equalTo("CREDENCIAL_INEXISTENTE"));
    }

    private static Map<String, Object> req(String token, Long aparelhoId) {
        Map<String, Object> m = new HashMap<>();
        m.put("token", token);
        m.put("aparelhoId", aparelhoId);
        return m;
    }
}
