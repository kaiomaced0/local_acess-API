---
id: 034
titulo: Push notifications via FCM (real)
prioridade: baixa
estimativa: G
depende-de: [032]
epico: locais
---

## Contexto
Atividade 032 deixou o `PushChannel` como **stub** — apenas loga. A integração
real com Firebase Cloud Messaging foi adiada. Sem isso, usuários offline (app
fechado) não recebem aviso de pendência aprovada/recusada, perdendo a
proposta de notificação multi-canal.

## Objetivo
Substituir o stub por envio real via FCM HTTP v1 API, com tokens de device
por usuário.

## Critérios de aceitação
- [ ] Entidade `DispositivoUsuario` (`usuario_id`, `fcmToken`, `plataforma`,
      `criadoEm`, `ultimoUsoEm`). Filtro de tenant.
- [ ] `POST /usuario-logado/dispositivos` registra/atualiza token FCM do device.
- [ ] `DELETE /usuario-logado/dispositivos/{id}` para logout.
- [ ] `PushChannel.enviar` autentica via service account JSON e POSTa em
      `https://fcm.googleapis.com/v1/projects/{projectId}/messages:send`.
- [ ] Configuração: `firebase.project-id` e `firebase.credentials-path` (ou
      base64 inline via env var). Sem credenciais ⇒ canal volta a comportar
      como stub e loga warning.
- [ ] Token inválido (resposta `UNREGISTERED` ou `INVALID_ARGUMENT`) ⇒ remove
      automaticamente do banco.
- [ ] Inclui `CanalNotificacao.PUSH` no default do `Usuario.canaisNotificacao`
      via migração.

## Notas técnicas
- Lib: `com.google.firebase:firebase-admin`.
- Rate limit do FCM ≈ 600k msg/min — atender com folga.
- Sem service account ⇒ no-op silencioso, não falha o startup.
