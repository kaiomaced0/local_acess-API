package ka.mdo.facial;

/**
 * Evento CDI disparado quando a validação facial devolve um resultado que
 * exige revisão por um gestor (hoje: {@code DIVERGENTE}).
 *
 * <p>O {@code FacialValidationService} NÃO chama o {@code NotificacaoService}
 * diretamente — a atividade 031 (workflow de pendências) observará este evento,
 * persistirá a pendência em sua própria entidade e só então notificará.
 * Mantemos assim o acoplamento mínimo e o {@code FacialValidationService} livre
 * do conhecimento sobre canais/gestores.
 *
 * <p>Todos os campos são IDs por valor (nada de entidades anexadas) — o
 * observer roda em sua própria transação.
 *
 * @param empresaId                tenant do aparelho/credencial.
 * @param ingressoId               credencial que gerou a pendência.
 * @param aparelhoId               aparelho que leu (para auditoria do local).
 * @param localId                  {@code EspacoEvento} (pode ser null).
 * @param capturaObjectKey         chave da foto capturada no bucket
 *                                 {@code capturas-acesso} — gestor vê ao abrir.
 * @param motivo                   código curto (ex: {@code ROSTO_DIVERGENTE}).
 * @param scoreFrigate             score retornado pelo Frigate; 0 quando sem match.
 */
public record PendenciaRequerida(
        Long empresaId,
        Long ingressoId,
        Long aparelhoId,
        Long localId,
        String capturaObjectKey,
        String motivo,
        double scoreFrigate
) {
}
