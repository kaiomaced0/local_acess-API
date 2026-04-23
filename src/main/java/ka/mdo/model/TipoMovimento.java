package ka.mdo.model;

/**
 * Tipo de movimento em uma leitura de credencial (atividade 041).
 *
 * <p>Introduzido para viabilizar o cálculo de ocupação em tempo real nos
 * dashboards. Até 040 só registrávamos {@link #ENTRADA}; {@link #SAIDA} é
 * opt-in e preenchida pelos aparelhos que controlam o fluxo de saída. A
 * ocupação de um local é derivada de {@code COUNT(ENTRADA) - COUNT(SAIDA)}
 * filtrando por {@link ResultadoAcesso#AUTORIZADO}.
 *
 * <p>Default {@link #ENTRADA} — leitores que não enviam o campo preservam a
 * semântica anterior (toda leitura era entrada).
 */
public enum TipoMovimento {
    ENTRADA,
    SAIDA
}
