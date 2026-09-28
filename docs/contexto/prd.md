# PRD — Salvar na IA

> Estado de referência: v0.4.8 no código, compilada/publicada e ainda a testar no aparelho. A v0.4.7 continua instalada e testada.

## 1. Produto

**Salvar na IA** é um aplicativo Android local que funciona como uma caixa de entrada de vídeos para análise posterior por IA.

O uso principal é: encontrar um vídeo, compartilhar com **Salvar na IA** e continuar navegando. O aplicativo registra o item, usa o YTDLnis para baixar o MP4, organiza o arquivo, executa uma pré-análise local e deixa o vídeo pronto para o usuário enviar depois à IA ou aplicativo compatível que preferir.

O produto não é preso ao ChatGPT e não tenta substituir a análise externa completa.

## 2. Problema que resolve

Vídeos úteis encontrados em Instagram, TikTok e fontes semelhantes são fáceis de perder e trabalhosos de organizar manualmente. O app reduz esse atrito ao concentrar recebimento, download, identificação, pré-análise, envio posterior e descarte seguro em um fluxo local simples.

A experiência-alvo continua sendo:

**viu um vídeo → Compartilhar → Salvar na IA → acabou**

## 3. Usuário

O aplicativo é de uso pessoal do proprietário do projeto, em aparelho Android. Não existe sistema de contas, multiusuário ou autenticação remota.

## 4. Princípios do produto

- **GRÁTIS + LOCAL + SIMPLES + CONFIÁVEL.**
- Evitar infraestrutura, contas, APIs pagas e automações frágeis sem necessidade real.
- Manter o usuário livre para escolher a IA externa.
- Preservar arquivos e dados locais antes de conveniência.
- Preferir fluxo pequeno e previsível a um painel complexo.
- Só considerar um comportamento TESTADO quando ele tiver sido validado no aparelho real.

## 5. Fluxo principal atual

1. Usuário compartilha um link para **Salvar na IA**.
2. O app cria um registro local e entrega a URL ao YTDLnis.
3. O YTDLnis baixa o vídeo em `Download_Videos IA`.
4. O app monitora a pasta, tenta associar o MP4 ao item e aplica o código visual.
5. O vídeo passa pela pré-análise local quando o modelo está disponível.
6. O estado chega a **Pronto** quando o arquivo e a análise local estão concluídos.
7. O usuário pode abrir **Ver vídeo** ou tocar **Mandar para análise**.
8. O app prepara código, título, pré-análise, nome do arquivo, link original e prompt configurável.
9. O usuário escolhe a IA/app compatível e anexa manualmente o MP4.
10. A IA externa faz a análise completa. O prompt padrão pede que o resultado seja salvo no Notion quando a IA tiver acesso.
11. O usuário toca **Marcar como enviado**.
12. O MP4 entra na lixeira privada por até 7 dias, com restauração ou exclusão definitiva.

## 6. Funcionalidades atuais

### Recebimento e download
- Share Target Android para texto/URLs.
- Integração com YTDLnis por Intent explícita.
- Perfil Command do YTDLnis usando `-t mp4 --no-playlist`.
- Pasta de trabalho `Download_Videos IA`.
- Monitoramento do download por Foreground Service.
- Retry manual de download.
- Detecção de **Arquivo ausente** e tentativa de reaproveitar MP4 já presente.

### Organização
- SQLite local.
- Código visual curto: `1..9 → A1..A9 → B1..B9 → ...`.
- Novos itens recebem o menor código livre.
- O código permanece reservado enquanto o registro existir, inclusive na lixeira.
- O `id` interno do SQLite é autoincremental e nunca é reciclado.
- Renomeação do MP4 para incluir o código.

### IA local
- LiteRT-LM + Gemma 4 E2B.
- Modelo baixado separadamente (~2,6 GB), verificado por tamanho e SHA-256.
- Inferência local/offline após o download.
- Preparação de até cinco frames e áudio convertido para WAV mono 16 kHz, limitado a 3 minutos.
- Saída local: título + resumo em português.
- Funcionamento técnico básico TESTADO; qualidade semântica, desempenho e consumo em casos mais complexos ainda precisam de avaliação.

### Envio para análise
- **Mandar para análise** abre o compartilhamento genérico, sem prender o fluxo a uma IA específica.
- Prompt de análise editável, salvável e restaurável.
- MP4 anexado manualmente pelo usuário.
- **Ver vídeo** abre o arquivo local.

### Lixeira e segurança
- Lixeira privada do app por até 7 dias.
- **Restaurar**, **Excluir agora** e **Esvaziar lixeira**.
- Exclusão do registro só ocorre quando o arquivo físico foi removido ou já não existe.
- A limpeza automática ocorre quando o app volta a rodar após o prazo; não há WorkManager com execução exata.

### Atualização
- Verificação de versão pelo GitHub Releases.
- Download do APK pelo DownloadManager.
- Instalação pelo instalador oficial do Android.
- APK release assinado de forma permanente via GitHub Actions/Repository Secrets.

## 7. Escopo atual

O escopo atual é um aplicativo Android local e pessoal, com banco e arquivos locais, YTDLnis como downloader e IA local apenas como pré-análise.

Não existe backend remoto, login, sincronização em nuvem ou API própria do Notion.

## 8. Limites e decisões permanentes

Não reintroduzir sem nova decisão explícita:
- AccessibilityService controlando ChatGPT;
- IA externa ou chat específico obrigatório;
- limite obrigatório de vídeos por chat;
- automação visual dependente da interface de uma IA;
- servidor próprio, Supabase ou Netlify;
- API paga como base do produto;
- Tasker ou Samsung Routines;
- downloader próprio no lugar do YTDLnis sem necessidade real;
- lixeira visível dentro de `Download_Videos IA`;
- limitação rígida de download a 720p;
- integração do app com Notion usando credenciais próprias.

## 9. O que pertence à versão atual

No repositório, a versão atual é **0.4.8**. Ela padroniza as confirmações do aplicativo em um único diálogo arredondado reutilizável.

Estado da v0.4.8: **IMPLEMENTADA / COMPILADA / PUBLICADA / A TESTAR NO APARELHO**.

A v0.4.7 continua sendo a versão instalada e validada no aparelho até o próximo teste.

## 10. Funcionalidades futuras já aprovadas ou pendentes

- Validar visualmente a v0.4.8 no aparelho.
- Validar controles rápidos de copiar link/cancelar.
- Validar reciclagem do menor código livre.
- Validar o fluxo destrutivo completo de **Esvaziar lixeira**.
- Avaliar qualidade semântica e consumo da IA local em vídeos mais complexos.
- Implementar/validar uma fila sequencial real e uma associação de downloads mais robusta antes de tratá-las como concluídas.

Ideias que ainda não foram aprovadas não devem ser documentadas aqui como roadmap comprometido.
