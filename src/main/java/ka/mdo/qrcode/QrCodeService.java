package ka.mdo.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.InternalServerErrorException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Renderiza QR Codes a partir de um conteúdo textual (tipicamente o token
 * opaco de uma credencial). O conteúdo passado aqui é considerado sensível:
 * nunca é logado pelo serviço.
 */
@ApplicationScoped
public class QrCodeService {

    public static final int TAMANHO_DEFAULT = 300;

    private Map<EncodeHintType, Object> hints() {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        return hints;
    }

    private BitMatrix encode(String conteudo, int tamanho) {
        if (conteudo == null || conteudo.isBlank()) {
            throw new IllegalArgumentException("conteudo do QR não pode ser vazio");
        }
        int dim = tamanho <= 0 ? TAMANHO_DEFAULT : tamanho;
        try {
            return new QRCodeWriter().encode(conteudo, BarcodeFormat.QR_CODE, dim, dim, hints());
        } catch (WriterException e) {
            throw new InternalServerErrorException("Falha ao gerar QR Code", e);
        }
    }

    /**
     * Gera um PNG do QR correspondente a {@code conteudo}, com {@code tamanho}
     * pixels de lado. Tamanho <= 0 usa {@link #TAMANHO_DEFAULT}.
     */
    public byte[] gerarPng(String conteudo, int tamanho) {
        BitMatrix matrix = encode(conteudo, tamanho);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        } catch (IOException e) {
            throw new InternalServerErrorException("Falha ao serializar PNG do QR", e);
        }
        return out.toByteArray();
    }

    /**
     * Gera um SVG simples do QR correspondente a {@code conteudo}. Cada módulo
     * preto vira um {@code <rect>} de 1 unidade; o viewBox acompanha a matriz.
     * O atributo {@code width}/{@code height} é preenchido com {@code tamanho}.
     */
    public byte[] gerarSvg(String conteudo, int tamanho) {
        BitMatrix matrix = encode(conteudo, tamanho);
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int render = tamanho <= 0 ? TAMANHO_DEFAULT : tamanho;

        StringBuilder svg = new StringBuilder(1024);
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
           .append("<svg xmlns=\"http://www.w3.org/2000/svg\" ")
           .append("width=\"").append(render).append("\" ")
           .append("height=\"").append(render).append("\" ")
           .append("viewBox=\"0 0 ").append(width).append(' ').append(height).append("\" ")
           .append("shape-rendering=\"crispEdges\">")
           .append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>")
           .append("<path fill=\"#000000\" d=\"");
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix.get(x, y)) {
                    svg.append('M').append(x).append(' ').append(y).append("h1v1h-1z");
                }
            }
        }
        svg.append("\"/></svg>");
        return svg.toString().getBytes(StandardCharsets.UTF_8);
    }
}
