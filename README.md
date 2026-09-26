# Salvar na IA

Miniaplicativo Android local para receber vídeos compartilhados, usar o YTDLnis como downloader e organizar uma fila simples para análise posterior em qualquer IA/app.

## v0.2.1 — Beta integrada

Esta beta junta as partes já validadas do fluxo:

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
- botão **Mandar para análise** copia/prepara o texto e abre o compartilhamento genérico do Android;
- botão **Marcar como enviado** remove o vídeo de `Download_Videos IA` e o guarda em uma lixeira privada do próprio app;
- a lixeira não aparece no explorador/seletor de arquivos, retém o vídeo por até 7 dias e permite restaurar ou excluir imediatamente;
- itens com mais de 7 dias são apagados quando o app volta a ser executado.

### Primeira configuração depois de instalar

Abra o app e toque em **Conectar / trocar pasta**. Escolha exatamente a pasta já usada pelo YTDLnis:

`Download_Videos IA`

Essa autorização é necessária porque o Android não compartilha automaticamente com o Salvar na IA a permissão que foi concedida ao YTDLnis.

## Fluxo da beta

`Instagram/TikTok → Compartilhar → Salvar na IA → YTDLnis baixa em segundo plano → Salvar na IA detecta → renomeia → Pronto → Mandar para análise → Marcar como enviado → Lixeira 7 dias`

## Perfil YTDLnis

O uso normal do YTDLnis continua independente.

O perfil **Salvar na IA** deve ser:
- Modelo de comando preferido: ligado;
- Comando extra: desligado;
- URL Regex: vazio;
- Pasta de comandos personalizados: `Download_Videos IA`;
- Comando: `-t mp4 --no-playlist`.

## IA local

A estrutura de dados já possui título e resumo para receber uma futura pré-análise local. A análise real de vídeo por IA local (imagem + áudio) **ainda não está ativada nesta beta**, porque o modelo/runtime local ainda precisa ser escolhido e testado no aparelho real. O app não inventa um resumo: enquanto isso, usa o título real do arquivo baixado.

## Observação da beta

A associação automática usa a chegada do arquivo novo na pasta exclusiva. Para o teste inicial, evite iniciar manualmente outro download para a mesma pasta enquanto um item do Salvar na IA estiver baixando.

## Build

O GitHub Actions gera o APK debug automaticamente.


### Ajuste v0.2.1

A primeira beta criou uma pasta visível `Lixeira Salvar na IA` dentro de `Download_Videos IA`. Isso foi substituído por uma lixeira privada do app, para não poluir a pasta usada na hora de anexar vídeos em outra IA. A restauração também passou a validar a remoção física do arquivo da lixeira antes de atualizar o status do item.
