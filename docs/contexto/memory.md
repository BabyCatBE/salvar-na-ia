# Memória técnica — Salvar na IA

Este arquivo guarda decisões e aprendizados que ainda afetam o projeto. Não é um changelog completo.

## 2026-09-28 — Seis arquivos de contexto passam a fazer parte do projeto

**Decisão:** criar `docs/contexto/` com `prd.md`, `architecture.md`, `rules.md`, `design.md`, `task.md` e `memory.md`.

**Motivo:** tornar produto, arquitetura, regras, design, trabalho atual e decisões importantes legíveis diretamente por qualquer agente que atuar no código, sem depender do histórico de chats.

**Impacto:** alterações relevantes devem manter o arquivo correspondente sincronizado. O Contexto Mestre continua sendo a continuidade consolidada e o Notion continua sendo a visão humana resumida.

**Alternativa evitada:** criar documentação genérica desconectada do repositório ou duplicar todo o Contexto Mestre.

---

## 2026-09-26 — v0.4.8 padroniza os diálogos próprios

**Decisão:** usar um único componente de confirmação arredondado para Cancelar item, Remover registro e Esvaziar lixeira.

**Motivo:** havia duas confirmações ainda usando `AlertDialog` padrão enquanto o restante do app já tinha linguagem visual própria.

**Impacto:** novas confirmações devem reutilizar o padrão atual salvo mudança deliberada de design.

**Estado:** implementada, compilada e publicada; ainda precisa de validação visual no aparelho.

---

## 2026-09-26 — v0.4.7 consolida o padrão visual da Lixeira

**Decisão:** tratar ações destrutivas em vermelho suave/vermelho e restauração como ação neutra.

**Motivo:** melhorar hierarquia e consistência sem mudar a lógica física.

**Impacto:** redesign TESTADO/APROVADO; não confundir essa aprovação visual com teste destrutivo completo de Esvaziar lixeira.

---

## Decisão vigente — o app não é preso ao ChatGPT

**Decisão:** a ação oficial é **Mandar para análise**, usando o share chooser genérico.

**Motivo:** evitar dependência de interface, fornecedor ou automação visual frágil.

**Impacto:** ChatGPT, Gemini, Claude, WhatsApp ou outro app compatível podem ser escolhidos. O MP4 continua podendo ser anexado manualmente.

**Alternativas descartadas:** AccessibilityService controlando ChatGPT, chat obrigatório, limite obrigatório de vídeos por chat e automação visual específica de uma IA.

---

## Decisão vigente — YTDLnis continua sendo o downloader

**Decisão:** não reinventar download dentro do app.

**Motivo:** YTDLnis/yt-dlp já resolve o problema sem servidor/API própria.

**Impacto:** integração atual usa perfil Command e `-t mp4 --no-playlist`.

**Alternativa descartada:** limite rígido de 720p. Ele falhou quando o formato pedido não existia; a qualidade voltou a ser automática.

---

## Decisão vigente — lixeira privada por 7 dias

**Decisão:** vídeos enviados permanecem recuperáveis por até 7 dias em armazenamento privado do app.

**Motivo:** evitar perda por toque acidental sem poluir a pasta principal.

**Impacto:** estado só muda após confirmação das operações físicas de cópia/exclusão.

**Alternativa descartada:** subpasta visível **Lixeira Salvar na IA** dentro de `Download_Videos IA`. Ela atrapalhava a localização dos vídeos e apresentou inconsistência de restauração física.

---

## Decisão vigente — infraestrutura remota não faz parte do produto

**Decisão:** banco e arquivos permanecem locais.

**Motivo:** manter o princípio GRÁTIS + LOCAL + SIMPLES + CONFIÁVEL.

**Impacto:** não há backend, login, Supabase, Netlify ou API paga.

**Alternativas descartadas:** servidor próprio, Supabase, Netlify, OpenAI API paga, Tasker e Samsung Routines.

---

## Decisão vigente — Notion não recebe credenciais do app

**Decisão:** quando a IA externa tiver acesso ao Notion, o prompt pede que ela salve a análise diretamente na página **Análises de Vídeos**.

**Motivo:** evitar API/credenciais próprias dentro do aplicativo.

**Impacto:** o app continua local e sem segredos de Notion.

---

## Aprendizado vigente — associação de download ainda é heurística

**Constatação:** o monitor tenta primeiro um token da URL no nome do arquivo e depois procura um arquivo ainda não associado compatível com o momento do download.

**Impacto:** vários compartilhamentos rápidos podem produzir associação menos confiável. A fila sequencial e uma associação robusta ainda não devem ser tratadas como concluídas.

**Próximo princípio:** antes de criar uma solução complexa, investigar mecanismos reais do YTDLnis que possam fornecer ID/evento/broadcast mais confiável.

---

## Aprendizado vigente — IA local está tecnicamente funcional, mas não “terminada”

**Decisão atual:** LiteRT-LM + Gemma 4 E2B permanece como pré-análise local automática.

**Evidência:** download/verificação do modelo e um fluxo real de pré-análise com título+resumo foram testados no aparelho.

**Limite:** fidelidade semântica, CPU/RAM, temperatura e vídeos complexos ainda precisam de avaliação.

**Impacto:** não voltar a tratar IA local como apenas uma ideia futura, mas também não declarar qualidade total já validada.

---

## Regra de limpeza desta memória

Quando uma decisão deixar de afetar o estado atual, removê-la ou consolidá-la. O GitHub guarda o histórico técnico detalhado; este arquivo só deve preservar contexto que evita regressão ou repetição de erros.
