# Tarefa atual — Salvar na IA

> Atualizado em 2026-09-28.

## Fase atual

**v0.4.8 — validação no aparelho real**

Código: v0.4.8.  
Instalada no aparelho: v0.4.7.  
v0.4.8: **IMPLEMENTADA / COMPILADA / PUBLICADA / A TESTAR**.

A implantação dos seis arquivos de contexto em `docs/contexto/` faz parte da documentação técnica permanente e foi concluída nesta etapa.

## Objetivo imediato

Instalar a v0.4.8 pelo atualizador interno já validado e confirmar visualmente que todas as confirmações próprias do app seguem o mesmo padrão arredondado.

## Próxima sequência de teste

1. Atualizar v0.4.7 → v0.4.8 pelo atualizador interno.
2. Abrir **Cancelar no Salvar na IA?** e validar apenas o visual; sair por Voltar/cancelamento, sem ação destrutiva.
3. Abrir **Esvaziar lixeira** e validar o visual; cancelar sem excluir.
4. Validar **Remover este registro?** quando um item em Arquivo ausente aparecer naturalmente.
5. Se todos estiverem coerentes, registrar a v0.4.8 como TESTADA/APROVADA visualmente.

## Pendências de teste já conhecidas

- controles rápidos de copiar link/× cancelar;
- reciclagem do menor código livre;
- execução destrutiva completa de **Esvaziar lixeira**;
- qualidade semântica da IA local em vídeos mais complexos;
- impacto de CPU/RAM/temperatura e desempenho da IA local.

Esses itens podem ser validados no uso normal quando não houver necessidade de teste destrutivo artificial.

## Próxima frente estrutural

Depois da validação da v0.4.8, continuar a investigação/implementação da:
- fila sequencial verdadeira;
- associação download↔registro mais robusta.

Antes de alterar essa arquitetura, verificar se o YTDLnis oferece evento, ID, broadcast ou outro mecanismo confiável que evite heurística.

## Limitações/bloqueios atuais

- Associação de download ainda heurística.
- Compartilhamentos rápidos podem gerar downloads simultâneos.
- Retry automático completo não existe.
- Não há WorkManager para limpeza exata aos 7 dias.
- Não há evidência suficiente para declarar a IA local plenamente validada em qualidade/recursos.

Nenhum desses pontos deve ser marcado como concluído apenas por intenção ou build.

## Concluído recentemente e ainda relevante

- v0.4.8: componente único para confirmações — COMPILADO/PUBLICADO, teste pendente.
- v0.4.7: redesign visual da Lixeira — TESTADO/APROVADO.
- Configurações arredondadas — TESTADAS/APROVADAS.
- Prompt editável/restaurável — TESTADO/APROVADO.
- Atualizador interno — TESTADO/APROVADO.
- Retry de Arquivo ausente reaproveitando MP4 já existente — TESTADO/APROVADO.
- Fluxo completo base da v0.3.3 — TESTADO/APROVADO.
- Lixeira privada com restauração/exclusão física individual — TESTADA/APROVADA.

## Regra deste arquivo

Manter apenas o ponto operacional atual e as próximas tarefas úteis. Histórico técnico detalhado pertence ao GitHub; decisões duráveis pertencem a `memory.md` e ao Contexto Mestre.
