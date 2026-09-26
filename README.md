# Salvar na IA

Miniaplicativo Android local, simples e sem custo recorrente para capturar vídeos compartilhados do Instagram/TikTok e organizar uma fila para análise posterior por IA.

## Versão atual: v0.1.1 — Share Target + refinamento visual

A Fase 1 está validada no aparelho real:

- aparece no menu **Compartilhar** como **Salvar na IA**;
- recebe texto/URL compartilhado pelo Instagram/TikTok;
- extrai e salva localmente a URL;
- registra quantidade e horário;
- fecha imediatamente depois do compartilhamento;
- permite abrir o app para conferir o último link;
- interface com cards arredondados, melhor hierarquia visual e transições leves.

## Decisões de UX para as próximas fases

O app não ficará preso a um único serviço de IA.

O botão de saída será chamado:

**Mandar para análise**

Esse botão deverá preparar/copiar o título, resumo, link original e nome exato do arquivo para que o usuário possa usar ChatGPT, Gemini, Claude ou outro app.

### Organização dos vídeos

- Todos os vídeos prontos ficam na mesma pasta.
- Depois da análise local, o MP4 será renomeado com um código curto + título.
- Sequência planejada dos códigos:
  `1 ... 9` → `A1 ... A9` → `B1 ... B9` → ... → `Z1 ... Z9` → `AA1 ... AA9` ...
- Exemplo:
  `A3 - Como automatizar estoque com IA.mp4`

### Ciclo planejado

`Compartilhado → Baixando → IA local analisando → Pronto → Mandar para análise → Marcar como enviado → Lixeira por 7 dias → Exclusão definitiva`

Ao marcar como enviado, o item sai da fila principal e o arquivo deixa a pasta de pendentes. Ele permanece recuperável por 7 dias na lixeira local antes da exclusão definitiva.

## Próxima fase

**Fase 2 — YTDLnis**

O app deverá enviar a URL recebida ao YTDLnis para iniciar o download em segundo plano. A Fase 3 ficará responsável por associar com segurança o MP4 baixado ao item correspondente.

## Build

O workflow `Build APK` gera um APK debug e publica o arquivo como artifact do GitHub Actions.
