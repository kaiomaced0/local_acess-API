---
id: 013
titulo: Endpoint REST de emissão de credencial
prioridade: alta
estimativa: P
depende-de: [010, 033]
epico: credencial
---

## Contexto
**Gap real**: `IngressoService.adicionarIngresso(usuarioId, dto)` existe e está
testado (atividade 051), mas não há resource REST que o exponha. PERMISSIONS.md
documenta `POST /usuarios/{id}/ingressos` como se existisse, mas não está
implementado — o frontend não consegue emitir credencial via HTTP.

## Objetivo
Expor a emissão como endpoint REST que respeite os perfis e o gate de
credenciais globais já implementados no service.

## Critérios de aceitação
- [ ] `POST /usuarios/{idUsuario}/ingressos` (ADMIN_EMPRESA / GESTOR_EVENTO /
      SUPER_ADMIN) recebe `IngressoDTO` no body, delega ao service.
- [ ] Aceita o campo opcional `escopoGlobal` (gate em `IngressoService` já existe).
- [ ] Responde 201 Created com `IngressoResponseDTO` (sem token bruto).
- [ ] 403 quando o usuário-alvo é de outro tenant.
- [ ] Atualiza o teste `CredencialEmissaoTest` para usar HTTP em vez de
      `@InjectMock JsonWebToken` no service.
- [ ] PERMISSIONS.md (já cita o endpoint) passa a refletir realidade.

## Notas técnicas
- Pode ir em `UsuarioResource` (sub-resource) ou em `IngressoResource`. O nome
  do path já está documentado: `/usuarios/{id}/ingressos`.
