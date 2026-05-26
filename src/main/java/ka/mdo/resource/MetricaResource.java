package ka.mdo.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import ka.mdo.dto.EntradaHoraDTO;
import ka.mdo.dto.OcupacaoLocalDTO;
import ka.mdo.service.MetricaService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Endpoints de métricas agregadas para o dashboard do gestor (atividade 041).
 *
 * <p>Todos os endpoints aceitam os 4 perfis com visão de gestão no tenant —
 * {@code GESTOR_LOCAL} tem a visibilidade restringida aos locais vinculados
 * (ver {@link ka.mdo.service.MetricaService}).
 *
 * <p>Cache de 30s em memória (TTL fixo, sem invalidação ativa) — ver
 * {@link MetricaService}.
 */
@Path("/metricas/evento/{eventoId}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"GESTOR_EVENTO", "ADMIN_EMPRESA", "SUPER_ADMIN", "GESTOR_LOCAL"})
@Tag(name = "Metricas", description = "Métricas agregadas para dashboard do gestor")
public class MetricaResource {

    @Inject
    MetricaService metricaService;

    @GET
    @Path("/ocupacao")
    @Operation(summary = "Ocupação atual por local",
            description = "Pessoas atualmente dentro de cada EspacoEvento do evento. "
                    + "Calculado como COUNT(ENTRADA) - COUNT(SAIDA) em LogAcesso com "
                    + "resultado=AUTORIZADO. Resposta cacheada 30s.")
    @APIResponse(responseCode = "200", description = "Lista de OcupacaoLocalDTO")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Evento pertence a outro tenant")
    @APIResponse(responseCode = "404", description = "Evento não encontrado")
    public List<OcupacaoLocalDTO> ocupacao(@PathParam("eventoId") Long eventoId) {
        return metricaService.ocupacao(eventoId);
    }

    @GET
    @Path("/entradas")
    @Operation(summary = "Série temporal de entradas",
            description = "Entradas autorizadas agrupadas por hora. "
                    + "Default: últimas 24h (se de/ate omitidos). "
                    + "Range máximo: 7 dias (400 se extrapolar). "
                    + "granularidade aceita apenas 'hora' por enquanto. "
                    + "Resposta cacheada 30s.")
    @APIResponse(responseCode = "200", description = "Lista de EntradaHoraDTO")
    @APIResponse(responseCode = "400", description = "Range inválido ou granularidade não suportada")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Evento pertence a outro tenant")
    @APIResponse(responseCode = "404", description = "Evento não encontrado")
    public List<EntradaHoraDTO> entradas(
            @PathParam("eventoId") Long eventoId,
            @QueryParam("granularidade") String granularidade,
            @QueryParam("de") LocalDateTime de,
            @QueryParam("ate") LocalDateTime ate) {
        return metricaService.entradas(eventoId, granularidade, de, ate);
    }

    @GET
    @Path("/pendencias")
    @Operation(summary = "Pendências agregadas por status e por local",
            description = "Resposta contém duas listas: 'status' (contagem por "
                    + "StatusPendencia) e 'porLocal' (contagem de ABERTAS por EspacoEvento). "
                    + "Resposta cacheada 30s.")
    @APIResponse(responseCode = "200", description = "Agregado PendenciaAgregadaResponse")
    @APIResponse(responseCode = "401", description = "Token ausente ou inválido")
    @APIResponse(responseCode = "403", description = "Evento pertence a outro tenant")
    @APIResponse(responseCode = "404", description = "Evento não encontrado")
    public MetricaService.PendenciaAgregadaResponse pendencias(
            @PathParam("eventoId") Long eventoId) {
        return metricaService.pendencias(eventoId);
    }
}
