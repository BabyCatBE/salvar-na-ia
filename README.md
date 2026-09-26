# Salvar na IA

Miniaplicativo Android local para receber vídeos compartilhados, usar o YTDLnis como downloader e organizar uma fila simples para análise posterior em qualquer IA/app.

## v0.3.3 — Pré-análise local + handoff para IA externa

Esta versão mantém o fluxo já validado e consolida a IA local como **pré-análise automática de apoio**, sem substituir a IA externa:

- recebe URL via menu **Compartilhar**;
- chama o YTDLnis diretamente em `TYPE=command` + `BACKGROUND=true`;
- usa o Command Template **Salvar na IA** configurado no YTDLnis;
- perfil YTDLnis validado com `-t mp4 --no-playlist`;
- qualidade automática;
- pasta exclusiva `Download_Videos IA`;
- monitora a pasta escolhida pelo usuário via Storage Access Framework;
- associa o arquivo baixado ao item;
- renomeia com código curto + título do arquivo;
- sequência de códigos: `1..9 → A1..A9 → B1..B9 → ...`;
- mostra fila de pendentes;
- botão **Mandar para análise** prepara código, título, pré-análise local, arquivo, link original e um prompt curto para a IA externa;
- botão **Ver vídeo** abre o MP4 local para conferir exatamente o arquivo que será analisado;
- botão **Marcar como enviado** remove o vídeo de `Download_Videos IA` e o guarda em uma lixeira privada do próprio app;
- a lixeira não aparece no explorador/seletor de arquivos, retém o vídeo por até 7 dias e permite restaurar ou excluir imediatamente;
- itens com mais de 7 dias são apagados quando o app volta a ser executado;
- detecta registros cujo MP4 físico desapareceu e mostra **Arquivo ausente**;
- integra uma pré-análise local automática experimental: **Baixando → Analisando IA → Pronto**;
- gera **título + resumo** com LiteRT-LM + Gemma 4 E2B depois que o modelo local for instalado.

### Primeira configuração depois de instalar

Abra o app e toque em **Conectar / trocar pasta**. Escolha exatamente a pasta já usada pelo YTDLnis:

`Download_Videos IA`

Essa autorização é necessária porque o Android não compartilha automaticamente com o Salvar na IA a permissão que foi concedida ao YTDLnis.

## Fluxo da beta

`Instagram/TikTok → Compartilhar → Salvar na IA → YTDLnis baixa em segundo plano → Salvar na IA detecta → renomeia → Analisando IA → Pronto → opcionalmente Ver vídeo → Mandar para análise → escolher IA externa e anexar MP4 → Marcar como enviado → Lixeira 7 dias`

## Perfil YTDLnis

O uso normal do YTDLnis continua independente.

O perfil **Salvar na IA** deve ser:
- Modelo de comando preferido: ligado;
- Comando extra: desligado;
- URL Regex: vazio;
- Pasta de comandos personalizados: `Download_Videos IA`;
- Comando: `-t mp4 --no-playlist`.

## IA local

A IA local foi validada tecnicamente no aparelho como **pré-análise automática de apoio**. O runtime é **LiteRT-LM** e o modelo atual é **Gemma 4 E2B**.

O modelo **não fica dentro do APK**. O aplicativo oferece um download inicial de aproximadamente **2,6 GB**, armazena o modelo na área privada do app e verifica tamanho + SHA-256 antes do primeiro uso. Depois disso, a inferência é local/offline.

Para cada vídeo novo, o app prepara até cinco frames representativos e extrai/converte até três minutos do áudio para WAV PCM mono de 16 kHz. A IA recebe imagem + áudio e deve gerar pelo menos **título + resumo** em português.

Estados novos: **Analisando IA**, **Aguardando IA**, **Falha na análise** e **Arquivo ausente**. Se a análise falhar, o MP4 permanece disponível e pode ser enviado para uma IA externa normalmente.

**Estado desta função: funcionamento técnico TESTADO no aparelho real como pré-análise local.** Ela gera título + resumo para contextualizar a etapa externa, mas não substitui a análise completa do MP4.

## Observação da beta

A associação automática usa a chegada do arquivo novo na pasta exclusiva. Para o teste inicial, evite iniciar manualmente outro download para a mesma pasta enquanto um item do Salvar na IA estiver baixando.

## Build

O GitHub Actions gera o APK **release assinado** automaticamente. A assinatura usa um keystore permanente reconstruído apenas durante o workflow a partir de **Repository secrets**; o keystore e as senhas não ficam no repositório público. O workflow também verifica a assinatura com `apksigner` antes de publicar o artefato.

A primeira instalação com essa chave exigiu substituir a antiga instalação debug. A atualização assinada da v0.2.1 para a v0.2.2 foi **TESTADA/APROVADA**, preservando SQLite e a autorização da pasta.


### Ajuste v0.2.1

A primeira beta criou uma pasta visível `Lixeira Salvar na IA` dentro de `Download_Videos IA`. Isso foi substituído por uma lixeira privada do app, para não poluir a pasta usada na hora de anexar vídeos em outra IA. A restauração também passou a validar a remoção física do arquivo da lixeira antes de atualizar o status do item.

**Teste real da v0.2.1: APROVADO.** Com um vídeo novo, foram validados download automático, status Pronto, arquivo físico na pasta principal, envio para a lixeira privada sem criar pasta visível, restauração física para `Download_Videos IA`, retorno a Pendentes e `Excluir agora` removendo registro e arquivo.


### v0.3.0 — estado de teste

O GitHub Actions compilou e verificou com sucesso o APK release assinado da v0.3.0. Isso significa **COMPILADO**, não **TESTADO**. O teste real deve começar instalando a v0.3.0 por cima da v0.2.2, depois validar **Arquivo ausente**, baixar o modelo local e usar apenas um vídeo novo para o primeiro teste da análise automática.


### v0.3.3 — handoff para análise externa

A v0.3.3 foi **IMPLEMENTADA / COMPILADA / TESTADA no aparelho para as mudanças internas**. O fluxo externo completo com uma IA + gravação no Notion ainda está **A VALIDAR**.

Mudanças:
- **Ver vídeo** abre o MP4 local antes do envio — **TESTADO/APROVADO**;
- **Mandar para análise** leva a pré-análise local como contexto e instrui a IA externa a confirmar, corrigir e complementar usando o vídeo completo — compartilhamento do texto **TESTADO**;
- o prompt final pede que, quando houver acesso ao Notion, a análise seja salva em **Análises de Vídeos**, criando uma nova subpágina para cada vídeo.

A página **Análises de Vídeos** já foi criada no Notion para centralizar os resultados. O app continua sem ficar preso ao ChatGPT: qualquer IA/app compatível pode receber o texto; a etapa de salvar no Notion só pode ser executada por uma IA que tenha acesso ao workspace.


#### Próximo teste

Executar um teste completo com um único vídeo já Pronto: tocar **Mandar para análise**, escolher uma IA com acesso ao Notion, anexar manualmente o MP4, enviar e confirmar se a análise final é salva como nova subpágina dentro de **Análises de Vídeos**. Esse resultado ainda não deve ser marcado como testado antes da confirmação real.
