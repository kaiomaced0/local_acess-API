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
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Emissão de credencial via HTTP (atividades 051 e 013).
 *
 * <p>Exercita o endpoint {@code POST /usuarios/{idUsuario}/ingressos}
 * adicionado na atividade 013: o JWT é mintado com {@code ADMIN_EMPRESA}
 * (papel autorizado pelo {@code @RolesAllowed} do resource) e o tenant é
 * carregado pelo {@code TenantRequestFilter} a partir do claim
 * {@code empresaId}. Cobre o caminho feliz (201 + DTO sem token bruto) e o
 * isolamento entre tenants (403 quando o usuário-alvo pertence a outra
 * empresa — regra do {@link ka.mdo.service.IngressoService}).
 */
@QuarkusTest
class CredencialEmissaoTest {

    @Inject
    TestDataSeeder seeder;

    @Test
    void emiteCredencialComTokenParaUsuarioDoTenant() {
        Long empresa = seeder.criarEmpresa("Empresa Emissora");
        Long usuario = seeder.criarUsuarioCliente(empresa);
        Long tipoIngresso = seeder.criarTipoIngresso(empresa);
        String auth = TestJwt.bearer(empresa, 1L, Perfil.ADMIN_EMPRESA.name());

        Integer ingressoId = given()
                .header("Authorization", auth)
                .contentType("application/json")
                .body(body("CHAVE-123", "lote-A", tipoIngresso))
                .when()
                .post("/usuarios/{id}/ingressos", usuario)
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("chaveAcesso", equalTo("CHAVE-123"))
                .body("lote", nullValue()) // lote não é persistido no Ingresso
                .body("escopoGlobal", nullValue())
                .body("token", nullValue()) // 013: token bruto nunca volta no DTO
                .extract().path("id");

        // O token foi gerado e persistido — mas só é visível direto no banco;
        // o response DTO esconde o valor opaco (a chave do QR).
        String token = seeder.tokenDoIngresso(Long.valueOf(ingressoId));
        assertNotNull(token, "token deveria ter sido gerado");
        assertFalse(token.isBlank(), "token não pode ser vazio");
    }

    @Test
    void emissaoParaUsuarioDeOutroTenantRetorna403() {
        Long empresaA = seeder.criarEmpresa("Empresa A");
        Long empresaB = seeder.criarEmpresa("Empresa B");
        Long usuarioB = seeder.criarUsuarioCliente(empresaB);
        Long tipoA = seeder.criarTipoIngresso(empresaA);
        String authA = TestJwt.bearer(empresaA, 1L, Perfil.ADMIN_EMPRESA.name());

        // ADMIN_EMPRESA do tenant A tenta emitir credencial para usuário do
        // tenant B — o IngressoService aborta com ForbiddenException antes de
        // persistir qualquer coisa.
        int status = given()
                .header("Authorization", authA)
                .contentType("application/json")
                .body(body("CROSS-TENANT", "lote-X", tipoA))
                .when()
                .post("/usuarios/{id}/ingressos", usuarioB)
                .then()
                .extract().statusCode();

        assertEquals(403, status, "deveria recusar emissão cross-tenant");
    }

    private static Map<String, Object> body(String chave, String lote, Long tipoIngressoId) {
        Map<String, Object> m = new HashMap<>();
        m.put("chaveAcesso", chave);
        m.put("lote", lote);
        m.put("idTipoIngresso", tipoIngressoId);
        return m;
    }
}
