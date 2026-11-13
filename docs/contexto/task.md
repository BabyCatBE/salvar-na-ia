# Tarefa atual — Salvar na IA

> Atualizado em 2026-11-13.

## Fase atual

**v0.4.8 instalada — uso normal + diagnóstico de TikTok + operação pontual de backlog**

Código/versão instalada: v0.4.8.

Estado de validação relevante:
- **Cancelar no Salvar na IA?** — TESTADO / APROVADO visualmente no aparelho.
- **Remover este registro?** — A TESTAR quando surgir naturalmente.
- Configurações, prompt editável, atualizador interno, base funcional e lixeira privada individual continuam conforme validações anteriores.

## Objetivo imediato

Há duas frentes operacionais, sem mudança no aplicativo neste momento:

1. Diagnosticar falhas frequentes de download de vídeos do TikTok no YTDLnis/yt-dlp.
2. Zerar com segurança um backlog grande de vídeos pendentes usando Codex no PC + ADB, sem apagar nem alterar dados do celular durante a missão.

## TikTok — diagnóstico atual

O perfil do Salvar na IA permanece com:
- comando: `-t mp4 --no-playlist`;
- qualidade: automática.

O usuário já instalou o canal **Nightly** do yt-dlp no YTDLnis.

Há relatos recentes no upstream do yt-dlp de falhas do TikTok por resposta inesperada/impersonação. Portanto, nesta fase:
- não alterar o comando do Salvar na IA por hipótese;
- reproduzir um link problemático diretamente no YTDLnis;
- capturar o erro/log exato;
- somente se o erro corresponder ao problema conhecido, avaliar um teste incremental de `--user-agent`.

## Operação de backlog via Codex + ADB

Missão planejada e ainda não executada:

1. conectar o celular via ADB;
2. localizar a pasta pública `Download_Videos IA`;
3. inventariar os MP4s;
4. copiar para o PC com `adb pull`;
5. validar quantidade/tamanhos;
6. analisar cada vídeo separadamente;
7. gerar análises individuais, índice geral, falhas e possíveis duplicados;
8. revisar o resultado antes de qualquer limpeza.

### Restrições obrigatórias

- ADB no celular somente para leitura/listagem e cópia.
- Não apagar, mover ou renomear arquivos do celular.
- Não alterar SQLite, SharedPreferences, lixeira privada nem arquivos internos do app.
- Não marcar itens como enviados.
- Não alterar o aplicativo nesta missão.
- Não usar serviço externo/pago para processar o backlog sem nova decisão.

Foi entregue ao usuário um prompt Markdown específico para executar essa missão no Codex.

## Pendências de teste conhecidas

- validar **Remover este registro?** quando surgir naturalmente;
- controles rápidos de copiar link/× cancelar;
- reciclagem do menor código livre;
- execução destrutiva completa de **Esvaziar lixeira**;
- qualidade semântica da IA local em vídeos mais complexos;
- impacto de CPU/RAM/temperatura e desempenho da IA local.

## Próxima frente estrutural do produto

Depois das frentes operacionais acima, permanecem:
- fila sequencial verdadeira;
- associação download↔registro mais robusta.

Antes de alterar essa arquitetura, verificar se o YTDLnis oferece evento, ID, broadcast ou outro mecanismo confiável que evite heurística.

## Limitações/bloqueios atuais

- associação de download ainda heurística;
- compartilhamentos rápidos podem gerar downloads simultâneos;
- retry automático completo não existe;
- não há WorkManager para limpeza exata aos 7 dias;
- não há evidência suficiente para declarar a IA local plenamente validada em qualidade/recursos;
- falha frequente do TikTok ainda está em diagnóstico e não foi atribuída definitivamente ao app.

## Regra deste arquivo

Manter apenas o ponto operacional atual e as próximas tarefas úteis. Histórico técnico detalhado pertence ao GitHub; decisões duráveis pertencem a `memory.md` e ao Contexto Mestre.
