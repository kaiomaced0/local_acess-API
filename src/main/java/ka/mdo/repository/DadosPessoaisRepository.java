package ka.mdo.repository;

import java.util.Optional;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import ka.mdo.model.DadosPessoais;
import ka.mdo.model.TipoDocumento;

/**
 * Repositório de {@link DadosPessoais}.
 *
 * <p>O filtro {@code tenantFilter} definido na entidade é aplicado
 * automaticamente pelo {@code TenantRequestFilter} — todas as queries aqui
 * já vêm limitadas à empresa do JWT (exceto para SUPER_ADMIN).
 */
@ApplicationScoped
public class DadosPessoaisRepository implements PanacheRepository<DadosPessoais> {

    /**
     * Busca por documento normalizado dentro do tenant atual. Não loga o
     * documento em claro.
     */
    public Optional<DadosPessoais> findByDocumento(TipoDocumento tipo, String documento) {
        if (tipo == null || documento == null || documento.isBlank()) {
            return Optional.empty();
        }
        return find("tipoDocumento = ?1 AND documento = ?2", tipo, documento).firstResultOptional();
    }

    /**
     * Retorna os dados pessoais vinculados ao usuário informado, navegando
     * pela FK {@code Usuario.dadosPessoais}. Respeita o filtro de tenant.
     */
    public Optional<DadosPessoais> findByUsuarioId(Long usuarioId) {
        if (usuarioId == null) {
            return Optional.empty();
        }
        return find("SELECT dp FROM DadosPessoais dp JOIN Usuario u ON u.dadosPessoais = dp WHERE u.id = ?1",
                usuarioId).firstResultOptional();
    }
}
