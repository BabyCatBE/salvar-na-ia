# Arquitetura atual — Salvar na IA

> Este documento descreve a implementação real observada na branch `main`, versão 0.4.8. Não é uma arquitetura idealizada.

## 1. Visão geral

O sistema é um aplicativo Android nativo, monomódulo, escrito principalmente em Java. Não existe backend remoto.

Fluxo de alto nível:

`Share Target → SQLite → YTDLnis → pasta SAF → DownloadMonitorService → IA local → MainActivity → app/IA externa → lixeira privada`

## 2. Stack atual

- Android nativo.
- Java 21.
- Android Gradle Plugin 9.4.0.
- Gradle 9.6 no CI.
- `compileSdk 36`, `targetSdk 36`, `minSdk 26`.
- SQLite local via `SQLiteOpenHelper`.
- Storage Access Framework / `DocumentsContract`.
- Android `DownloadManager`.
- Foreground Service para monitoramento.
- LiteRT-LM `0.17.1`.
- Gemma 4 E2B em arquivo `.litertlm`.
- GitHub Actions para build/assinatura/publicação.
- YTDLnis como aplicativo downloader externo.

## 3. Estrutura técnica observada

```text
/
├─ README.md
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle.properties
├─ .github/
│  └─ workflows/
│     └─ build-apk.yml
├─ docs/
│  └─ contexto/
│     ├─ prd.md
│     ├─ architecture.md
│     ├─ rules.md
│     ├─ design.md
│     ├─ task.md
│     └─ memory.md
└─ app/
   ├─ build.gradle.kts
   └─ src/main/
      ├─ AndroidManifest.xml
      ├─ java/com/babycatbe/salvarnaia/
      │  ├─ MainActivity.java
      │  ├─ ShareReceiverActivity.java
      │  ├─ AppStore.java
      │  ├─ FolderManager.java
      │  ├─ YtdlnisHelper.java
      │  ├─ DownloadMonitorService.java
      │  ├─ LocalAiModelManager.java
      │  ├─ LocalAiEngine.java
      │  ├─ VideoAiMedia.java
      │  ├─ AudioWavExtractor.java
      │  ├─ ModelDownloadReceiver.java
      │  ├─ AppSettings.java
      │  ├─ AppUpdateManager.java
      │  └─ AppUpdateDownloadReceiver.java
      └─ res/
         ├─ values/strings.xml
         └─ drawable/ic_launcher.xml
```

A interface principal é construída programaticamente em Java; não existe um conjunto de layouts XML que defina a UI atual.

## 4. Módulos e responsabilidades

### MainActivity
- Monta a interface.
- Exibe Pendentes/Lixeira, pasta, IA local e Configurações.
- Aciona envio, visualização, retry, restauração e exclusão.
- Executa reconciliação de arquivo ausente.
- Faz limpeza de lixeira vencida quando o app retoma.
- Centraliza os componentes visuais atuais, inclusive o diálogo de confirmação.

### ShareReceiverActivity
- Recebe `ACTION_SEND` com texto.
- Extrai a primeira URL.
- Cria item `DOWNLOADING` no SQLite.
- Verifica acesso à pasta.
- Envia a URL ao YTDLnis.
- Inicia o monitoramento e encerra rapidamente.

### AppStore
Banco local `salvar_na_ia.db`, versão 1.

Tabela `items`:
- `id INTEGER PRIMARY KEY AUTOINCREMENT`
- `code`
- `url`
- `status`
- `title`
- `summary`
- `file_uri`
- `file_name`
- `error`
- `created_at`
- `updated_at`
- `trash_at`

Estados implementados:
- `DOWNLOADING`
- `ANALYZING`
- `AI_SETUP_REQUIRED`
- `ANALYSIS_ERROR`
- `FILE_MISSING`
- `READY`
- `ERROR`
- `TRASHED`

O código visual é alocado dentro de transação pelo menor código livre. O `id` do banco não é reutilizado.

### FolderManager
- Persiste a URI da pasta escolhida via SAF.
- Lista vídeos da raiz.
- Renomeia documentos.
- Move vídeo para lixeira privada com cópia antes da exclusão da origem.
- Restaura vídeo para a pasta principal com rollback quando necessário.
- Mantém compatibilidade de restauração com a antiga lixeira visível da beta v0.2.0.

### YtdlnisHelper
Integração explícita com:
- pacote `com.deniscerri.ytdl`;
- activity `com.deniscerri.ytdl.receiver.ShareActivity`;
- `ACTION_SEND`/`text/plain`;
- extras `TYPE=command` e `BACKGROUND=true`.

O app não implementa o downloader.

### DownloadMonitorService
Foreground Service com executor de uma thread.

Responsabilidades:
- procura arquivos novos;
- tenta associar MP4 a item pendente;
- renomeia/registre URI;
- inicia a IA local;
- atualiza estados;
- timeout de download: 15 minutos;
- polling durante download: aproximadamente 2,5 s.

A associação ainda é heurística: primeiro tenta token extraído da URL no nome do arquivo e depois usa candidato ainda não associado com tolerância de horário. Isso não equivale a uma fila sequencial robusta.

### IA local
`LocalAiModelManager`:
- gerencia download do Gemma 4 E2B;
- tamanho esperado: 2.588.147.712 bytes;
- verifica SHA-256;
- armazena o modelo em diretório privado externo do app.

`VideoAiMedia`:
- extrai até cinco frames em 8%, 30%, 55%, 80% e 96% da duração;
- reduz a maior dimensão para no máximo 640 px;
- JPEG qualidade 84;
- extrai até 180 s de áudio.

`AudioWavExtractor`:
- decodifica áudio com APIs Android;
- converte para WAV PCM mono 16 kHz.

`LocalAiEngine`:
- tenta backend GPU;
- usa CPU como fallback;
- envia frames + áudio + prompt local;
- produz título e resumo.

### Configurações
`AppSettings` usa SharedPreferences para o prompt de análise. O prompt padrão orienta a IA externa a analisar o MP4 completo, usar a pré-análise como contexto e, quando tiver acesso, criar uma subpágina diretamente em **Análises de Vídeos** no Notion.

### Atualizador
`AppUpdateManager` consulta a release mais recente do GitHub, baixa o APK e abre o instalador oficial. `AppUpdateDownloadReceiver` notifica quando o download terminou.

## 5. Fluxo de dados

### Recebimento
URL compartilhada → `ShareReceiverActivity` → `AppStore.addDownloading()` → YTDLnis → `DownloadMonitorService`.

### Arquivo
YTDLnis grava em `Download_Videos IA` → SAF lista o arquivo → associação heurística → renomeação → `file_uri` e `file_name` no SQLite.

### IA
MP4 → frames + WAV → LiteRT-LM/Gemma → título + resumo → SQLite → UI.

### Envio externo
SQLite + prompt configurável → share chooser Android → usuário escolhe app → usuário anexa MP4 manualmente.

### Lixeira
MP4 SAF → cópia para armazenamento privado → exclusão confirmada da origem → `TRASHED`. Na restauração, a ordem é inversa e o estado só volta a `READY` depois de a cópia física ter sucesso.

## 6. Armazenamento

- Banco: sandbox do app, `salvar_na_ia.db`.
- Pasta de vídeos: árvore SAF escolhida pelo usuário, normalmente `Download_Videos IA`.
- Lixeira atual: armazenamento privado do app.
- Modelo IA: diretório `models` do armazenamento privado externo do app.
- Preferências: SharedPreferences para URI, prompt, downloads/atualizações e estado de verificação.

## 7. Autenticação e backend

- Não há login.
- Não há autenticação de usuário.
- Não há servidor próprio.
- Não há Supabase, Netlify ou banco remoto.
- O app não guarda credenciais do Notion.

## 8. APIs e serviços externos

- YTDLnis: integração por Intent Android.
- GitHub Releases API: consulta de versão e URL do APK.
- GitHub Actions/Releases: build, assinatura e distribuição.
- Hugging Face: origem atual do arquivo do modelo Gemma, em URL fixada pelo código.
- Notion: destino opcional da análise externa por instrução textual; não é integração direta do aplicativo.

## 9. Build e distribuição

`.github/workflows/build-apk.yml`:
- dispara em push na `main` e manualmente;
- Temurin Java 21;
- Android SDK 36 / build-tools 36.0.0;
- Gradle 9.6;
- `gradle :app:assembleRelease --stacktrace`;
- reconstrói temporariamente o keystore a partir de Repository Secrets;
- verifica assinatura com `apksigner`;
- publica artifact e GitHub Release.

Segredos nunca devem ser gravados no repositório.

## 10. Limitações arquiteturais atuais

- Associação download↔registro ainda heurística.
- Compartilhamentos rápidos podem iniciar downloads simultâneos.
- Não existe controlador de fila sequencial completo.
- Retry automático completo não existe.
- Limpeza de 7 dias depende do app voltar a executar.
- A qualidade/consumo da IA local não está totalmente validada.

Essas limitações devem continuar explícitas até implementação e teste real.
