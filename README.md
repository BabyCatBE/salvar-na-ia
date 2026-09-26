# Salvar na IA

Aplicativo Android local para receber vídeos compartilhados, usar o YTDLnis como downloader, fazer uma pré-análise local com IA e organizar o envio do MP4 para qualquer IA/app compatível.

## Estado atual

**Código atual:** v0.4.7  
**Versão instalada no aparelho:** v0.4.6  
**Atualizador interno:** TESTADO / APROVADO  
**Configurações arredondadas:** TESTADAS / APROVADAS  
**Retry de Arquivo ausente na v0.4.6:** TESTADO / APROVADO  
**Prompt editável/restaurável:** TESTADO / APROVADO  
**Reciclagem de códigos, Esvaziar lixeira e controles rápidos:** A TESTAR

A base v0.3.3 permanece **TESTADA/APROVADA** no fluxo completo: download, pré-análise local, Ver vídeo, Mandar para análise, análise externa e salvamento no Notion.

## Fluxo principal

`Instagram/TikTok → Compartilhar → Salvar na IA → YTDLnis baixa → app associa/renomeia → Gemma faz pré-análise → Pronto → Ver vídeo ou Mandar para análise → anexar MP4 na IA externa → salvar análise no Notion → Marcar como enviado → Lixeira privada por 7 dias`

## IA local

- Runtime: **LiteRT-LM**
- Modelo atual: **Gemma 4 E2B**
- Modelo baixado separadamente (~2,6 GB), não embutido no APK.
- Inferência local/offline depois do download.
- O app prepara até cinco frames representativos e até três minutos de áudio em WAV PCM mono 16 kHz.
- A saída local é usada como **pré-análise de apoio**: título + resumo.
- A IA externa continua responsável pela análise completa do MP4.

Estados relevantes: **Baixando**, **Analisando IA**, **Aguardando IA**, **Pronto**, **Falha na análise** e **Arquivo ausente**.

## Mandar para análise

O app prepara automaticamente:
- código;
- título;
- pré-análise local;
- nome do arquivo;
- link original;
- prompt final configurável.

O MP4 ainda é anexado manualmente pelo usuário na IA escolhida.

O prompt padrão manda a IA externa analisar o vídeo completo, usar a pré-análise apenas como contexto e salvar o resultado diretamente como uma nova subpágina filha da página raiz **Análises de Vídeos** no Notion. Ele também diz explicitamente para não criar uma análise dentro de outra análise.

## Configurações — v0.4.0

A tela principal ganhou um botão **⚙ Configurações**.

Dentro dela:
- edição manual do **Prompt de análise**;
- **Salvar prompt**;
- **Restaurar prompt padrão**;
- versão instalada;
- **Verificar atualização**;
- download e instalação de uma nova versão publicada em GitHub Releases.

O Android continua exigindo confirmação do usuário para instalar o APK. Na primeira atualização interna, o sistema também pode pedir autorização para **Instalar apps desconhecidos** a partir do Salvar na IA.

## Atualizador interno

O app consulta:

`https://api.github.com/repos/BabyCatBE/salvar-na-ia/releases/latest`

Quando encontra uma versão maior:
1. oferece **Baixar e instalar**;
2. usa o DownloadManager do Android;
3. ao terminar, mostra uma notificação de atualização pronta;
4. ao tocar, abre o instalador oficial do Android;
5. a assinatura permanente permite instalar por cima preservando os dados.

O GitHub Actions agora publica automaticamente cada versão compilada também em **GitHub Releases**, além do artifact do workflow.

## Reciclagem de códigos — v0.4.0

Os códigos visíveis não crescem indefinidamente.

Sequência:
`1..9 → A1..A9 → B1..B9 → ...`

Regras:
- enquanto o registro existir, o código permanece reservado;
- isso inclui itens na lixeira de 7 dias;
- **Marcar como enviado** não libera o código;
- **Excluir agora**, limpeza automática após 7 dias ou **Esvaziar lixeira** removem definitivamente o registro e liberam o código;
- todo vídeo novo recebe o **menor código livre**;
- o `id` interno do SQLite continua autoincremental e nunca é reciclado.

## Lixeira

- privada do app;
- não aparece dentro de `Download_Videos IA`;
- retenção de até 7 dias;
- permite **Restaurar**;
- permite **Excluir agora**;
- v0.4.0 adiciona **Esvaziar lixeira**, com confirmação explícita;
- registros só são removidos quando o arquivo físico foi apagado ou já não existe.

## Arquivo ausente

Se um registro disser que o vídeo existe mas o MP4 físico tiver desaparecido, o app mostra **Arquivo ausente** em vez de manter um falso estado Pronto.

Ações:
- **Baixar novamente**;
- **Remover registro**.

## Pasta dos vídeos

O YTDLnis e o Salvar na IA usam a pasta:

`Download_Videos IA`

Perfil YTDLnis aprovado:
- Modelo de comando preferido: ligado;
- Comando extra: desligado;
- URL Regex: vazio;
- Pasta de comandos personalizados: `Download_Videos IA`;
- comando: `-t mp4 --no-playlist`;
- qualidade: automática.

## Build e assinatura

Configuração atual:
- package: `com.babycatbe.salvarnaia`
- compileSdk 36
- targetSdk 36
- minSdk 26
- Java 21
- Gradle 9.6
- assinatura Android permanente via GitHub Repository Secrets.

O keystore e as senhas não ficam no repositório público.

O workflow:
1. recompõe temporariamente o keystore;
2. compila o APK release;
3. verifica a assinatura com `apksigner`;
4. publica artifact do workflow;
5. publica/atualiza uma **GitHub Release** com o APK assinado.

## Testes já aprovados

- lixeira privada e restauração física;
- exclusão definitiva individual;
- atualização assinada por cima preservando SQLite e autorização da pasta;
- detecção de **Arquivo ausente**;
- download e verificação do Gemma;
- pré-análise automática local;
- **Ver vídeo** abrindo o MP4 local;
- **Mandar para análise** com contexto + prompt;
- ChatGPT analisando o MP4 completo e criando a subpágina **7 — Organização** diretamente em **Análises de Vídeos** no Notion.

## Próximo teste

O atualizador interno já foi validado no aparelho no fluxo **v0.4.1 → v0.4.6**, preservando pasta, Gemma e registros. O visual arredondado das Configurações também foi aprovado.

O retry de **Arquivo ausente** foi validado na v0.4.6: com o MP4 já existente na pasta, o app localizou e reaproveitou o arquivo e retomou o processamento sem criar outro download.

O prompt editável/restaurável foi validado: alteração manual foi salva e **Restaurar prompt padrão** voltou corretamente ao texto original.

A v0.4.6 seguirá em uso normal. Os controles rápidos de copiar link/cancelar, a reciclagem do menor código livre e **Esvaziar lixeira** permanecem **PENDENTES DE TESTE específico** e serão validados conforme aparecerem naturalmente no uso real.


## Redesign da Lixeira — v0.4.7

Mudança visual somente, sem alterar a lógica de restauração/exclusão:

- **Esvaziar lixeira (N)** com tratamento destrutivo em vermelho suave;
- código dos itens da lixeira em chip vermelho suave;
- **↻ Restaurar** em ação neutra;
- **🗑 Excluir agora** em ação destrutiva vermelha;
- confirmação de **Esvaziar lixeira** deixou de usar o AlertDialog padrão do Android;
- novo Dialog próprio do app com cantos arredondados, ícone de lixeira, texto centralizado, **Cancelar** contornado e **Excluir tudo** vermelho.

Estado: **IMPLEMENTADA / EM BUILD / A TESTAR**. Para validar o visual, basta abrir a confirmação e cancelar; não é necessário excluir vídeos úteis.
