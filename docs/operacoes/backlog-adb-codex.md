# Prompt para o Codex — resgatar e analisar backlog do Salvar na IA

Quero fazer uma operação segura de resgate e análise em lote dos vídeos pendentes do aplicativo Android **Salvar na IA** usando o celular conectado por ADB.

## Projeto

- GitHub técnico: `BabyCatBE/salvar-na-ia`
- branch: `main`
- package: `com.babycatbe.salvarnaia`
- pasta normalmente usada pelos vídeos: `Download_Videos IA`
- o app usa YTDLnis como downloader.
- **NÃO quero alterar o aplicativo nesta missão.**

## OBJETIVO

Localizar todos os MP4s atualmente existentes na pasta pública usada pelo Salvar na IA no celular, copiá-los para o PC com ADB e criar uma análise individual de cada vídeo, preservando o código/nome de origem.

Quero zerar meu backlog de vídeos sem risco de perder nada.

## REGRAS DE SEGURANÇA — OBRIGATÓRIAS

1. NÃO apagar nenhum arquivo do celular.
2. NÃO mover nem renomear nenhum arquivo do celular.
3. NÃO alterar SQLite, SharedPreferences, arquivos privados do app ou lixeira privada.
4. NÃO usar `adb shell rm`, `mv`, `rmdir`, limpeza de dados, uninstall ou qualquer comando destrutivo.
5. NÃO marcar vídeos como enviados no aplicativo.
6. NÃO modificar GitHub, código Android ou configuração do Salvar na IA.
7. Nesta missão, ADB é somente para leitura/listagem e cópia dos MP4s públicos.
8. Se houver dúvida sobre um comando potencialmente destrutivo, não execute.
9. Preserve os arquivos originais integralmente.
10. Nunca colocar senhas, tokens ou credenciais em arquivos ou logs.

## FASE 1 — VALIDAR ADB

Comece executando:

```bash
adb devices
```

Confirme que existe exatamente um dispositivo Android autorizado.

Se houver um único dispositivo autorizado, continue sozinho.

Se não houver dispositivo autorizado, pare e me diga apenas o que está impedindo a conexão.

## FASE 2 — LOCALIZAR A PASTA REAL

Procure com segurança a pasta `Download_Videos IA` no armazenamento compartilhado.

Comece pelas localizações Android comuns, como `/sdcard/` e `/storage/emulated/0/`.

Não presuma o caminho sem verificar.

Localize a pasta real e liste seu conteúdo.

Considere nesta operação apenas arquivos de vídeo reais, principalmente `.mp4`.

**NÃO tente acessar `/data/data/com.babycatbe.salvarnaia` nem contornar a sandbox Android.**

## FASE 3 — CRIAR ÁREA DE TRABALHO NO PC

Crie no PC uma pasta de trabalho claramente identificada, por exemplo:

```text
Salvar-na-IA-Backlog
```

Dentro dela crie:

```text
originais/
analises/
frames/
audio_temporario/
logs/
relatorio/
```

Não trabalhe diretamente sobre os arquivos do celular.

## FASE 4 — INVENTÁRIO ANTES DA CÓPIA

Antes de copiar, gere um inventário dos vídeos encontrados contendo, sempre que possível:

- nome do arquivo;
- código aparente do Salvar na IA;
- tamanho;
- extensão;
- caminho no celular.

Apresente um resumo curto com:

- quantidade total de vídeos;
- tamanho total aproximado.

Depois continue automaticamente.

## FASE 5 — CÓPIA VIA ADB

Copie os MP4s para `originais/` usando `adb pull`.

É **CÓPIA**, nunca movimentação.

Não sobrescreva silenciosamente arquivos diferentes com o mesmo nome.

Depois da cópia:

- confirme quantidade de arquivos;
- compare tamanhos quando possível;
- registre falhas;
- não exclua nada do celular mesmo após cópia bem-sucedida.

## FASE 6 — INVENTÁRIO TÉCNICO LOCAL

Use `ffprobe` ou ferramenta equivalente para obter de cada vídeo:

- duração;
- resolução;
- codec;
- FPS quando disponível;
- tamanho.

Se FFmpeg/ffprobe não estiver instalado, verifique primeiro se existe alguma instalação utilizável no computador antes de instalar algo.

Pode instalar somente dependências gratuitas, locais e razoáveis quando forem realmente necessárias para esta tarefa.

Evite infraestrutura, servidor, API paga ou upload de vídeos para serviços externos.

## FASE 7 — ANÁLISE DOS VÍDEOS

Analise **CADA vídeo separadamente**.

Não coloque dezenas de vídeos dentro de um único contexto de análise.

Para cada vídeo:

1. preserve o código/nome original;
2. extraia frames representativos ao longo do vídeo;
3. examine visualmente os frames;
4. extraia o áudio quando houver;
5. transcreva o áudio localmente quando houver ferramenta local adequada;
6. use imagem + fala/transcrição + metadados para compreender o conteúdo;
7. produza uma análise individual.

Prefira ferramentas:

**GRÁTIS + LOCAL + SIMPLES + CONFIÁVEIS.**

Se precisar de transcrição e não houver ferramenta instalada, procure primeiro uma opção local leve e confiável, como Whisper/faster-whisper ou equivalente compatível com o computador.

Não faça upload automático dos vídeos para serviços de terceiros.

Se alguma etapa de análise não puder ser feita com confiabilidade, registre essa limitação naquele vídeo em vez de inventar conteúdo.

## FASE 8 — FORMATO DA ANÁLISE INDIVIDUAL

Para cada vídeo gere um arquivo em `analises/`.

Nome preferido:

```text
CÓDIGO — TÍTULO.md
```

Quando o código puder ser identificado pelo nome do arquivo, preserve-o exatamente.

Cada análise deve conter:

```markdown
# CÓDIGO — TÍTULO

**Arquivo original:**  
**Duração:**  
**Origem/código:**  

## Resumo
Resumo objetivo do vídeo.

## Conteúdo principal
Explique o que o vídeo mostra ou ensina.

## Pontos importantes
Principais ideias, técnicas, ferramentas, produtos ou argumentos apresentados.

## O que vale guardar
Informações que podem ser úteis futuramente.

## Possíveis aplicações para mim
Quando houver relação clara com meus projetos, estudos, programação, IA, automações ou ferramentas, indique aplicações possíveis.

Não force relações inexistentes.

## Observações
Limitações da análise, áudio ruim, conteúdo incompleto, propaganda, vídeo duplicado etc.
```

## FASE 9 — DETECÇÃO DE DUPLICADOS

Verifique possíveis vídeos duplicados usando combinação de:

- hash;
- duração;
- tamanho;
- nome;
- conteúdo quando necessário.

Não apague duplicados.

Apenas marque-os no relatório.

## FASE 10 — ÍNDICE GERAL

Ao terminar, gere:

```text
relatorio/INDICE-GERAL.md
```

com uma tabela contendo:

- código;
- título;
- arquivo;
- duração;
- status da análise;
- possível duplicado;
- observação curta.

Também gere um resumo com:

- vídeos encontrados;
- vídeos copiados;
- vídeos analisados;
- falhas;
- duplicados possíveis;
- duração total;
- tamanho total.

## FASE 11 — RELATÓRIO DE FALHAS

Gere também:

```text
relatorio/FALHAS.md
```

Liste somente vídeos que:

- não copiaram;
- estão corrompidos;
- não abriram;
- não puderam ser analisados;
- tiveram áudio/transcrição insuficiente;
- apresentaram qualquer outro problema.

Para cada falha, explique exatamente o motivo e se é recuperável.

## FASE 12 — NÃO ALTERAR O CELULAR AO TERMINAR

Ao final, **NÃO limpe a pasta do celular**.

**NÃO marque nada como enviado.**

**NÃO altere registros no Salvar na IA.**

Quero primeiro revisar o resultado da operação.

## ENTREGA FINAL

Trabalhe de forma autônoma até concluir tudo que for possível.

No final me mostre apenas um resumo claro contendo:

- quantidade de vídeos encontrada;
- quantidade copiada;
- quantidade analisada;
- duração total;
- tamanho total;
- quantidade de falhas;
- possíveis duplicados;
- caminho da pasta de trabalho no PC;
- caminho do `INDICE-GERAL.md`;
- qualquer limitação que realmente exija minha intervenção.

Não fique me pedindo confirmação a cada vídeo.

Só pare antes do final se houver:

- risco de perda de dados;
- ADB sem acesso;
- necessidade de credencial;
- necessidade de enviar os vídeos para serviço externo;
- ou impossibilidade técnica real de continuar localmente.
