package ka.mdo.service;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ka.mdo.model.Ingresso;
import ka.mdo.repository.IngressoRepository;

import java.util.List;

/**
 * Substitui tokens placeholder 'legacy-*' gerados pela migração V5 por tokens
 * cripto-seguros reais, gerados via {@link TokenService}. Executa uma única
 * vez no startup, é idempotente (só atualiza linhas cujo token ainda começa
 * com 'legacy-') e não loga valores de token.
 */
@ApplicationScoped
public class BackfillTokenService {

    @Inject
    IngressoRepository repository;

    @Inject
    TokenService tokenService;

    void onStartup(@Observes StartupEvent event) {
        executar();
    }

    @Transactional
    public void executar() {
        List<Ingresso> pendentes = repository.find("token LIKE ?1", "legacy-%").list();
        if (pendentes.isEmpty()) {
            return;
        }
        Log.infof("BackfillTokenService: regenerando %d token(s) legados", pendentes.size());
        for (Ingresso ingresso : pendentes) {
            ingresso.setToken(tokenService.gerarToken());
        }
    }
}
