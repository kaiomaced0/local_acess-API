package ka.mdo.frigate;

/**
 * Abstração do domínio sobre o Frigate — esconde o contrato HTTP real
 * (que muda entre versões/forks do Frigate) atrás de três operações que
 * nosso {@code FacialValidationService} precisa.
 *
 * <p>Escolhemos expor uma interface própria (e não um {@code @RegisterRestClient}
 * direto) porque:
 * <ul>
 *     <li>A API do Frigate faz upload de imagem via multipart {@code form-data}
 *     com nomes de campo que variam entre instalações (alguns plugins trocam
 *     {@code face} por {@code image}); encapsular aqui mantém os callers limpos.</li>
 *     <li>Podemos substituir a implementação por um mock/stub em testes sem
 *     depender de proxy de rest-client reativo.</li>
 *     <li>O fallback ({@code frigate.fallback}) é aplicado uniformemente no
 *     {@code FacialValidationService}, tratando qualquer exceção desta interface
 *     como "Frigate indisponível".</li>
 * </ul>
 *
 * <p>Todas as implementações devem respeitar timeouts curtos (3s connect, 10s read
 * — ver {@code application.properties}) e NUNCA logar bytes da imagem (apenas
 * tamanho).
 */
public interface FrigateService {

    /**
     * Enrola/cadastra o rosto de uma pessoa no Frigate. Idempotente do ponto
     * de vista do domínio: o serviço só chama este método quando
     * {@code DadosPessoais.rostoFrigateCadastrado} está falso.
     *
     * @param pessoaId     identificador estável do dono (normalmente
     *                     {@code "empresa/{empresaId}/usuario/{id}"}). Persistido
     *                     em {@code DadosPessoais.frigatePessoaId}.
     * @param imagemBytes  bytes da imagem JPEG/PNG já validados pelo
     *                     {@code StorageValidator}.
     * @param contentType  MIME real (ex: {@code image/jpeg}).
     * @throws FrigateException em caso de falha HTTP/timeout/indisponibilidade.
     */
    void cadastrarRosto(String pessoaId, byte[] imagemBytes, String contentType);

    /**
     * Compara um rosto capturado no aparelho com a base enrolada. Retorna o
     * melhor match encontrado. Se nenhum match com score razoável existir,
     * devolve {@code FrigateRostoMatch(null, 0.0)} (nunca {@code null}).
     *
     * <p>A decisão de autorização é do caller — este método só retorna o score.
     *
     * @throws FrigateException em caso de falha HTTP/timeout/indisponibilidade.
     */
    FrigateRostoMatch compararRosto(byte[] imagemBytes, String contentType);

    /**
     * Remove o rosto do Frigate (usado quando o dono exclui seus dados
     * pessoais — LGPD "direito ao esquecimento"). Idempotente: remoções
     * duplicadas não devem propagar erro.
     */
    void removerRosto(String pessoaId);
}
