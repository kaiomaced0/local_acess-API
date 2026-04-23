---
id: 040
titulo: Mapa 2D do evento com polígonos coloridos por local
prioridade: media
estimativa: M
depende-de: [001]
epico: mapa
---

## Contexto
O gestor deve poder desenhar um mapa 2D do evento, com cada `EspacoEvento` representado por uma forma colorida. Serve para visualização no painel e para o app dos aparelhos.

## Objetivo
Modelar e expor o mapa do evento.

## Critérios de aceitação
- [x] Entidade `MapaEvento` (eventoId, largura, altura, unidade, imagemFundoUrl opcional).
- [x] Entidade `FormaMapa` (mapaEventoId, espacoEventoId, tipo=RETANGULO|POLIGONO|CIRCULO, geometria JSON, cor hex, rotulo).
- [x] Endpoint `GET /eventos/{id}/mapa` retorna mapa + formas.
- [x] Endpoint `PUT /eventos/{id}/mapa` salva/atualiza (gestor de evento).
- [x] Validação: toda `FormaMapa` referencia um `EspacoEvento` da mesma empresa.

## Notas técnicas
- Geometria como JSON é suficiente (não precisa PostGIS).
- Campo `geometria` como `@Column(columnDefinition="JSON")` ou TEXT com parse no service.

## Resultado

### Arquivos criados
- `src/main/java/ka/mdo/model/MapaEvento.java` — entidade 1:1 com `Evento`
  (UNIQUE em `evento_id`), FK para `Empresa` e `@Filter(tenantFilter)`,
  coleção de formas com `cascade=ALL` + `orphanRemoval=true`.
- `src/main/java/ka/mdo/model/FormaMapa.java` — entidade com FKs para
  `MapaEvento` e `EspacoEvento`, `geometriaJson TEXT`, `corHex VARCHAR(7)`.
- `src/main/java/ka/mdo/model/TipoForma.java` — enum `RETANGULO | POLIGONO
  | CIRCULO` com Javadoc descrevendo o payload de cada tipo.
- `src/main/java/ka/mdo/dto/MapaEventoDTO.java` — request com
  `largura/altura/unidade/imagemFundoObjectKey/formas` + Bean Validation.
- `src/main/java/ka/mdo/dto/FormaMapaDTO.java` — request com
  `@Pattern` na `corHex` (regex `^#[0-9A-Fa-f]{6}$`).
- `src/main/java/ka/mdo/dto/MapaEventoResponseDTO.java` e
  `FormaMapaResponseDTO.java` — response com URL assinada do fundo e
  geometria já desserializada em `Map<String,Object>`.
- `src/main/java/ka/mdo/repository/MapaEventoRepository.java` e
  `FormaMapaRepository.java`.
- `src/main/java/ka/mdo/service/MapaEventoService.java` — upsert,
  validação de tenant, validação de `corHex`, validação estrutural da
  geometria por tipo, upload da imagem de fundo.
- `src/main/java/ka/mdo/resource/MapaEventoResource.java` — GET/PUT e
  `POST /imagem-fundo` (octet-stream + `X-Content-Type`).
- `src/main/resources/db/migration/V14__mapa_evento.sql` — tabelas
  `MapaEvento` (evento_id UNIQUE, empresa_id) e `FormaMapa` com índices.

### Arquivos alterados
- `src/main/java/ka/mdo/storage/StorageBucketBootstrap.java` — injeta
  `storage.bucket.mapas` (default `"mapas"`) e inclui no loop de
  criação de buckets no startup.
- `PERMISSIONS.md` — anexadas as três rotas novas na tabela e nota
  explicativa no final (apêndice, sem reordenar).

### Estratégia de storage do fundo
Bucket dedicado `mapas` em vez de reusar `credenciais-foto`/`documentos`.
Motivos: separação lógica por domínio (cada bucket pode receber políticas
IAM/retenção próprias no futuro), evita vazamento de prefixos por erro de
listagem, e não mistura fotos pessoais (LGPD) com assets de visualização.
A chave segue o padrão `empresa-{id}/evento-{id}/fundo.{ext}`. A criação
do bucket é idempotente via `StorageBucketBootstrap` (default
`@ConfigProperty` cobre o caso de `application.properties` sem a chave).
O upload passa pelo `StorageValidator.validarImagem` que reaproveita
`storage.tipos-permitidos-imagem` e `storage.max-file-size-bytes`.

### Build status
`./mvnw.cmd compile -q` — SUCESSO (apenas warnings de ambiente do Maven
3.9.5 no Java 21, não relacionados ao código).

### Checklist
- [x] Entidades `MapaEvento` e `FormaMapa` com FK para `Empresa` + tenant filter.
- [x] Upsert do mapa com substituição total das formas (orphanRemoval).
- [x] Validação de tenant no evento E em cada `EspacoEvento` referenciado.
- [x] Validação "espaço pertence ao evento" (cross-evento → 400).
- [x] Regex estrita de cor `^#[0-9A-Fa-f]{6}$` no DTO e no service.
- [x] Validação estrutural da geometria por tipo (Jackson + checagem de chaves).
- [x] Endpoint GET aberto a todos perfis autenticados do tenant.
- [x] Endpoint PUT + upload restritos a gestor/admin.
- [x] Upload octet-stream + `X-Content-Type` (padrão 020).
- [x] Bucket `mapas` criado no `StorageBucketBootstrap`.
- [x] Migração V14 com FKs, UNIQUE(evento_id) e índices.
- [x] `PERMISSIONS.md` anexado (3 rotas + nota explicativa), sem reordenação.
- [x] Compile verde.
- [ ] Testes — delegado a 051 (conforme orientação de paralelismo).
