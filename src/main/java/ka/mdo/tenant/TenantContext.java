package ka.mdo.tenant;

import jakarta.enterprise.context.RequestScoped;

/**
 * Armazena o empresaId extraído do JWT durante o ciclo de vida da requisição.
 * Request-scoped para não vazar entre threads/chamadas.
 */
@RequestScoped
public class TenantContext {

    private Long empresaId;

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public boolean temTenant() {
        return empresaId != null;
    }
}
