package ka.mdo.frigate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Random;

/**
 * Implementação de {@link FrigateService} usando {@link HttpClient} do JDK.
 *
 * <p>Por que {@code HttpClient} em vez de {@code @RegisterRestClient}:
 * <ul>
 *     <li>A API do Frigate exige upload multipart ({@code form-data}) e o
 *     rest-client reativo precisa de MessageBodyWriter extra para
 *     {@code MultipartForm} — adicionar só para este caso fura o orçamento
 *     de dependências.</li>
 *     <li>Precisamos de controle fino sobre timeouts (connect + read) para
 *     disparar o fallback rapidamente; o {@code HttpClient} do JDK expõe isso
 *     nativamente ({@code connectTimeout} + {@code timeout} por request).</li>
 *     <li>Já temos o Jackson no classpath (via {@code quarkus-resteasy-jackson}),
 *     então parsear o JSON de resposta é zero ganho extra.</li>
 * </ul>
 *
 * <p>Contrato assumido (compatível com o fork stock do Frigate + plugin face):
 * <ul>
 *     <li>{@code POST {url}/api/faces/{pessoaId}} multipart, field {@code face}
 *     (imagem). 200/201 = sucesso.</li>
 *     <li>{@code POST {url}/api/faces/recognize} multipart, field {@code face}.
 *     Resposta: {@code { "matches": [{ "name": "...", "score": 0.93 }, ...] }}.
 *     Pegamos o primeiro (melhor) match.</li>
 *     <li>{@code DELETE {url}/api/faces/{pessoaId}} — 200 ou 404 (tratamos 404
 *     como idempotência).</li>
 * </ul>
 * Quando a instalação do Frigate usar endpoints diferentes, basta ajustar
 * aqui — o contrato do domínio ({@link FrigateService}) não muda.
 *
 * <p><b>Segurança/privacidade</b>: NUNCA logamos bytes da imagem, apenas
 * {@code imagemBytes.length}. O {@code pessoaId} é logado porque não é
 * informação sensível (derivado de ids internos, não do documento do usuário).
 */
@ApplicationScoped
public class FrigateServiceImpl implements FrigateService {

    private static final Logger LOG = Logger.getLogger(FrigateServiceImpl.class);

    @ConfigProperty(name = "frigate.url", defaultValue = "http://localhost:5000")
    String frigateUrl;

    @ConfigProperty(name = "frigate.timeout-connect-ms", defaultValue = "3000")
    long timeoutConnectMs;

    @ConfigProperty(name = "frigate.timeout-read-ms", defaultValue = "10000")
    long timeoutReadMs;

    @ConfigProperty(name = "frigate.token", defaultValue = "")
    String frigateToken;

    @Inject
    ObjectMapper objectMapper;

    private HttpClient httpClient;

    @PostConstruct
    void init() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutConnectMs))
                .build();
    }

    @Override
    public void cadastrarRosto(String pessoaId, byte[] imagemBytes, String contentType) {
        LOG.infof("Frigate cadastrarRosto pessoa=%s bytes=%d ct=%s",
                pessoaId, imagemBytes == null ? 0 : imagemBytes.length, contentType);
        String boundary = novoBoundary();
        byte[] body = multipartFace(boundary, imagemBytes, contentType);
        HttpRequest req = builder("/api/faces/" + urlPath(pessoaId))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<String> resp = enviar(req, "cadastrarRosto");
        if (resp.statusCode() / 100 != 2) {
            throw new FrigateException(
                    "Frigate cadastrarRosto falhou: HTTP " + resp.statusCode());
        }
    }

    @Override
    public FrigateRostoMatch compararRosto(byte[] imagemBytes, String contentType) {
        LOG.infof("Frigate compararRosto bytes=%d ct=%s",
                imagemBytes == null ? 0 : imagemBytes.length, contentType);
        String boundary = novoBoundary();
        byte[] body = multipartFace(boundary, imagemBytes, contentType);
        HttpRequest req = builder("/api/faces/recognize")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<String> resp = enviar(req, "compararRosto");
        if (resp.statusCode() / 100 != 2) {
            throw new FrigateException(
                    "Frigate compararRosto falhou: HTTP " + resp.statusCode());
        }
        return parseMatch(resp.body());
    }

    @Override
    public void removerRosto(String pessoaId) {
        LOG.infof("Frigate removerRosto pessoa=%s", pessoaId);
        HttpRequest req = builder("/api/faces/" + urlPath(pessoaId))
                .DELETE()
                .build();
        HttpResponse<String> resp = enviar(req, "removerRosto");
        int code = resp.statusCode();
        // 404 é tratado como sucesso (idempotente).
        if (code / 100 != 2 && code != 404) {
            throw new FrigateException(
                    "Frigate removerRosto falhou: HTTP " + code);
        }
    }

    /* ==================== internos ==================== */

    private HttpRequest.Builder builder(String path) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(frigateUrl + path))
                .timeout(Duration.ofMillis(timeoutReadMs));
        if (frigateToken != null && !frigateToken.isBlank()) {
            b.header("Authorization", "Bearer " + frigateToken);
        }
        return b;
    }

    private HttpResponse<String> enviar(HttpRequest req, String op) {
        try {
            return httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (java.net.http.HttpTimeoutException e) {
            throw new FrigateException("Frigate timeout em " + op, e);
        } catch (java.io.IOException e) {
            throw new FrigateException("Frigate I/O em " + op + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FrigateException("Frigate interrompido em " + op, e);
        }
    }

    /**
     * Monta um corpo multipart com um único campo {@code face} contendo a
     * imagem. Simples e suficiente para os três endpoints do Frigate que
     * nosso domínio usa — não vale a pena trazer uma lib multipart só para
     * isto.
     */
    private byte[] multipartFace(String boundary, byte[] imagem, String contentType) {
        String extensao = contentType != null && contentType.contains("png") ? "png" : "jpg";
        String header =
                "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"face\"; filename=\"captura." + extensao + "\"\r\n"
                        + "Content-Type: " + (contentType == null ? "image/jpeg" : contentType) + "\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[headerBytes.length + imagem.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, out, 0, headerBytes.length);
        System.arraycopy(imagem, 0, out, headerBytes.length, imagem.length);
        System.arraycopy(footerBytes, 0, out, headerBytes.length + imagem.length, footerBytes.length);
        return out;
    }

    private String novoBoundary() {
        byte[] buf = new byte[16];
        new Random().nextBytes(buf);
        StringBuilder sb = new StringBuilder("----LocalAcessBoundary");
        for (byte b : buf) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String urlPath(String raw) {
        // path segment safe: espaços/caracteres especiais ficam %-encoded.
        return java.net.URLEncoder.encode(raw, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * Aceita dois formatos comuns de resposta do Frigate:
     * <pre>{ "matches": [{ "name": "...", "score": 0.9 }, ...] }</pre>
     * e
     * <pre>{ "name": "...", "score": 0.9 }</pre>
     * Retorna {@code FrigateRostoMatch(null, 0.0)} se nenhum match estiver
     * presente na resposta.
     */
    private FrigateRostoMatch parseMatch(String body) {
        if (body == null || body.isBlank()) {
            return new FrigateRostoMatch(null, 0.0);
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode alvo = root;
            if (root.has("matches") && root.get("matches").isArray() && root.get("matches").size() > 0) {
                alvo = root.get("matches").get(0);
            }
            if (!alvo.has("name") && !alvo.has("score")) {
                return new FrigateRostoMatch(null, 0.0);
            }
            String pessoaId = alvo.hasNonNull("name") ? alvo.get("name").asText() : null;
            double score = alvo.hasNonNull("score") ? alvo.get("score").asDouble() : 0.0;
            return new FrigateRostoMatch(pessoaId, score);
        } catch (Exception e) {
            throw new FrigateException("Resposta inválida do Frigate: " + e.getMessage(), e);
        }
    }
}
