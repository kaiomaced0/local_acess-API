package ka.mdo.rest;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Converte parâmetros de query/path em tipos {@code java.time}
 * ({@link LocalDateTime}, {@link LocalDate}) usando o formato ISO-8601.
 *
 * <p>O RESTEasy Classic não traz conversor nativo para esses tipos — sem este
 * provider, recursos que recebem datas em query param (ex.:
 * {@code MetricaResource}, {@code LogAcessoResource}) falham já no startup com
 * {@code RESTEASY003875: Unable to find a constructor that takes a String param
 * or a valueOf()/fromString() method ...}.
 *
 * <p>Valores nulos ou em branco viram {@code null} (parâmetros opcionais).
 */
@Provider
public class JavaTimeParamConverterProvider implements ParamConverterProvider {

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType.equals(LocalDateTime.class)) {
            return (ParamConverter<T>) new ParamConverter<LocalDateTime>() {
                @Override
                public LocalDateTime fromString(String value) {
                    return (value == null || value.isBlank()) ? null : LocalDateTime.parse(value.trim());
                }

                @Override
                public String toString(LocalDateTime value) {
                    return value == null ? null : value.toString();
                }
            };
        }
        if (rawType.equals(LocalDate.class)) {
            return (ParamConverter<T>) new ParamConverter<LocalDate>() {
                @Override
                public LocalDate fromString(String value) {
                    return (value == null || value.isBlank()) ? null : LocalDate.parse(value.trim());
                }

                @Override
                public String toString(LocalDate value) {
                    return value == null ? null : value.toString();
                }
            };
        }
        return null;
    }
}
