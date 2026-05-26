package ka.mdo.integration;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import ka.mdo.model.Perfil;
import ka.mdo.testsupport.TestDataSeeder;
import ka.mdo.testsupport.TestJwt;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Aprovação de pendência (atividade 051).
 *
 * <p>Semeia uma pendência {@code ABERTA} (motivo não-facial, sem foto) e a
 * aprova como {@code ADMIN_EMPRESA} do mesmo tenant, verificando a transição
 * de status para {@code APROVADA}.
 */
@QuarkusTest
class PendenciaAprovacaoTest {

    @Inject
    TestDataSeeder seeder;

    @Test
    void gestorAprovaPendenciaAberta() {
        Long empresa = seeder.criarEmpresa("Empresa Pendencia");
        Long pendencia = seeder.criarPendenciaAberta(empresa);
        String auth = TestJwt.bearer(empresa, 99L, Perfil.ADMIN_EMPRESA.name());

        given()
                .header("Authorization", auth)
                .contentType("application/json")
                .body(Map.of("observacao", "Documento conferido presencialmente"))
                .when()
                .post("/pendencias/{id}/aprovar", pendencia)
                .then()
                .statusCode(200)
                .body("id", equalTo(pendencia.intValue()))
                .body("status", equalTo("APROVADA"));
    }
}
