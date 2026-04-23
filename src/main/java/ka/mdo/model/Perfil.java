package ka.mdo.model;

/**
 * Perfis formais do sistema. O nome do enum (`name()`) é usado como valor
 * persistido em banco (via {@link jakarta.persistence.EnumType#STRING}) e
 * também como nome do "group" emitido no JWT para uso com {@code @RolesAllowed}.
 *
 * Hierarquia conceitual:
 *  - SUPER_ADMIN: administra a instância, cross-tenant (pode criar empresas).
 *  - ADMIN_EMPRESA: administra uma empresa (tenant).
 *  - GESTOR_EVENTO: gerencia eventos e espaços de sua empresa.
 *  - GESTOR_LOCAL: gerencia locais/espaços de sua empresa.
 *  - OPERADOR_APARELHO: opera dispositivos de validação (totens, leitores).
 *  - CLIENTE: usuário final (comprador/portador de ingresso).
 */
public enum Perfil {
    SUPER_ADMIN("Super Admin"),
    ADMIN_EMPRESA("Admin Empresa"),
    GESTOR_EVENTO("Gestor Evento"),
    GESTOR_LOCAL("Gestor Local"),
    OPERADOR_APARELHO("Operador Aparelho"),
    CLIENTE("Cliente");

    private final String label;

    Perfil(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
