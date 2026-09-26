# Salvar na IA

Miniaplicativo Android local para facilitar o envio de vídeos do Instagram/TikTok para um fluxo de análise no ChatGPT.

## Versão atual: v0.1 — Share Target

Esta versão implementa **somente a Fase 1**:

- aparece no menu **Compartilhar** como **Salvar na IA**;
- recebe texto/URL compartilhado por outros apps;
- extrai e salva localmente a URL recebida;
- registra quantidade e horário do último recebimento;
- fecha imediatamente após receber o compartilhamento;
- permite abrir o app manualmente para conferir o último link recebido.

Ainda **não** integra YTDLnis e **não** automatiza o ChatGPT. Essas etapas só serão iniciadas depois que a Fase 1 for testada no celular.

## Teste da v0.1

1. Instale o APK gerado pelo GitHub Actions.
2. Abra Instagram ou TikTok.
3. Em um vídeo, toque em **Compartilhar**.
4. Escolha **Salvar na IA**.
5. O app deve apenas mostrar uma confirmação curta e desaparecer.
6. Abra **Salvar na IA** manualmente.
7. Confira se o link aparece em **Último link recebido**.

## Build

O workflow `Build APK` gera um APK debug e o publica como artifact do GitHub Actions.

Projeto pensado para ser local, simples e sem custo recorrente.
