package ka.mdo.facial;

/**
 * Resultado do passo de validação facial do {@code AcessoService} (atividade 021).
 *
 * <ul>
 *     <li>{@link #AUTORIZADO} — rosto capturado bate com o enrolado (score &ge; threshold).</li>
 *     <li>{@link #CADASTRADO} — primeira leitura com sucesso; o rosto capturado
 *     foi enrolado no Frigate agora. Tratado como AUTORIZADO pelo caller.</li>
 *     <li>{@link #DIVERGENTE} — rosto bate mas com score abaixo do threshold,
 *     ou não foi encontrado match algum. Vira pendência (ver 031).</li>
 *     <li>{@link #INDISPONIVEL} — Frigate timeout/5xx/erro. A política de
 *     fallback ({@code frigate.fallback}) decide como o caller trata.</li>
 * </ul>
 */
public enum ResultadoFacial {
    AUTORIZADO,
    CADASTRADO,
    DIVERGENTE,
    INDISPONIVEL
}
