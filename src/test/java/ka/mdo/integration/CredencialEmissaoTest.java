package ka.mdo.integration;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import ka.mdo.dto.IngressoDTO;
import ka.mdo.dto.IngressoResponseDTO;
import ka.mdo.service.IngressoService;
import ka.mdo.testsupport.TestDataSeeder;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Emissão de credencial (atividade 051).
 *
 * <p>{@code IngressoService.adicionarIngresso} ainda não é exposto por nenhum
 * endpoint REST, então o teste exercita o serviço diretamente. A dependência
 * de {@code JsonWebToken} (claim {@code empresaId}) é satisfeita por
 * {@link InjectMock} — por isso esta classe não faz chamadas HTTP.
 *
 * <p>Verifica o cerne da emissão: vínculo correto com o tenant e geração de um
 * token opaco persistido (que nunca é devolvido no response DTO).
 */
@QuarkusTest
class CredencialEmissaoTest {

    @Inject
    IngressoService ingressoService;

    @Inject
    TestDataSeeder seeder;

    @InjectMock
    JsonWebToken jwt;

    @Test
    void emiteCredencialComTokenParaUsuarioDoTenant() {
        Long empresa = seeder.criarEmpresa("Empresa Emissora");
        Long usuario = seeder.criarUsuarioCliente(empresa);
        Long tipoIngresso = seeder.criarTipoIngresso(empresa);

        Mockito.when(jwt.getClaim("empresaId")).thenReturn(empresa);

        IngressoDTO dto = new IngressoDTO("CHAVE-123", "lote-A", tipoIngresso, null);
        Response resp = ingressoService.adicionarIngresso(usuario, dto);

        assertEquals(200, resp.getStatus());
        IngressoResponseDTO body = (IngressoResponseDTO) resp.getEntity();
        assertNotNull(body.id(), "credencial deveria ter id");
        assertEquals("CHAVE-123", body.chaveAcesso());

        String token = seeder.tokenDoIngresso(body.id());
        assertNotNull(token, "token deveria ter sido gerado");
        assertFalse(token.isBlank(), "token não pode ser vazio");
    }
}
