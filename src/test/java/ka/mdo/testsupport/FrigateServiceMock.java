package ka.mdo.testsupport;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import ka.mdo.frigate.FrigateRostoMatch;
import ka.mdo.frigate.FrigateService;

/**
 * Mock do {@link FrigateService} para os testes de integração (atividade 051):
 * substitui a implementação HTTP real e garante que nenhum teste toque a rede.
 * Os fluxos faciais não são exercitados pela suíte mínima — basta um stub.
 */
@Mock
@ApplicationScoped
public class FrigateServiceMock implements FrigateService {

    @Override
    public void cadastrarRosto(String pessoaId, byte[] imagemBytes, String contentType) {
        // no-op
    }

    @Override
    public FrigateRostoMatch compararRosto(byte[] imagemBytes, String contentType) {
        return new FrigateRostoMatch(null, 0.0);
    }

    @Override
    public void removerRosto(String pessoaId) {
        // no-op
    }
}
