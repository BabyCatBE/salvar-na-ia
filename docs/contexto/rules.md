# Regras permanentes — Salvar na IA

Estas regras são obrigatórias para qualquer IA ou desenvolvedor que trabalhar neste repositório.

## 1. Fontes e continuidade

1. Não recomeçar o projeto.
2. Antes de alteração técnica relevante, ler os arquivos aplicáveis em `docs/contexto/` e consultar o GitHub atual.
3. Para decisões funcionais/arquiteturais consolidadas, consultar também o Contexto Mestre e, quando necessário, o Notion do projeto.
4. Não presumir que uma decisão antiga continua válida quando uma fonte mais recente a substituiu.
5. Não ressuscitar ideias descartadas.
6. Não confundir documentação com implementação real.
7. Em conflito entre fontes, identificar a divergência, verificar a autoridade adequada e sincronizar as fontes permanentes; nunca escolher silenciosamente.

## 2. Estados de desenvolvimento

Usar quando fizer sentido:

`IDEIA → A DEFINIR → APROVADO → EM IMPLEMENTAÇÃO → IMPLEMENTADO → COMPILADO → A TESTAR → TESTADO`

Também podem existir `A VERIFICAR` e `DESCARTADO / SUPERADO`.

Nunca chamar:
- ideia de decisão;
- aprovado de implementado;
- implementado de compilado;
- compilado de testado;
- mudança visual de mudança física;
- item restaurado na UI de arquivo restaurado fisicamente.

Teste real no aparelho é a autoridade para afirmar que um comportamento funciona.

## 3. Forma de desenvolvimento

- Preferir: ChatGPT analisa → altera GitHub → GitHub Actions compila → usuário instala → usuário testa → resultado volta à documentação.
- Fazer mudanças incrementais e focadas.
- Preservar comportamentos já testados, salvo quando a tarefa for explicitamente substituí-los.
- Investigar antes de sobrescrever código.
- Não alterar arquivos sem relação com a tarefa.
- Reutilizar helpers/componentes existentes quando isso preservar consistência.
- Não exigir programação manual, Android Studio ou procedimentos evitáveis do usuário.
- Não trocar stack, Gradle, SDK, workflow ou estrutura funcional apenas por preferência técnica.

## 4. Produto

- O app não pode ficar preso ao ChatGPT.
- A ação oficial é **Mandar para análise**.
- O usuário deve poder escolher qualquer IA/app compatível.
- O MP4 pode continuar sendo anexado manualmente.
- YTDLnis permanece o downloader enquanto essa arquitetura estiver aprovada.
- Lixeira privada por 7 dias é uma decisão de produto importante.
- IA local é pré-análise de apoio; não substitui a análise externa completa.
- A implementação local LiteRT-LM + Gemma existe e o fluxo técnico básico foi testado, mas qualidade semântica, recursos e casos complexos não devem ser declarados plenamente validados.

## 5. Práticas proibidas sem nova decisão

Não reintroduzir automaticamente:
- AccessibilityService controlando uma IA;
- chat específico obrigatório;
- limite obrigatório de vídeos por chat;
- automação visual dependente da interface de uma IA;
- servidor próprio, Supabase ou Netlify;
- API paga como base do produto;
- contas/login;
- Tasker ou Samsung Routines;
- lixeira visível dentro da pasta principal;
- limitação rígida de download a 720p;
- downloader próprio substituindo YTDLnis sem necessidade real;
- credenciais do Notion dentro do app.

## 6. Banco, arquivos e dados

- `id` SQLite é interno, autoincremental e não deve ser reciclado.
- Código visual pode ser reciclado somente depois da exclusão definitiva do registro.
- Um código continua reservado enquanto o registro existir, inclusive em `TRASHED`.
- Mudanças em SQLite, SAF, renomeação, lixeira, restauração, exclusão ou migração exigem atenção especial à recuperação.
- Não alterar status para indicar sucesso antes de a operação física necessária ter sido confirmada.
- Ao mover para lixeira: confirmar cópia antes de apagar origem.
- Ao restaurar: confirmar recriação na pasta principal antes de apagar cópia privada e voltar para `READY`.
- Não apagar arquivo útil para simplificar teste.
- Mudança de schema futura deve ter estratégia de migração; `onUpgrade` vazio não autoriza quebra de dados.

## 7. Download e fila

- Não tratar a associação atual como robusta: ela é heurística.
- Não tratar a fila sequencial como pronta até implementação e teste.
- Antes de substituir a integração com YTDLnis, pesquisar mecanismo real mais confiável oferecido pelo próprio YTDLnis.
- Preservar o perfil Command e o comando aprovado `-t mp4 --no-playlist` enquanto não houver nova decisão.

## 8. Segurança

- Repositório é público.
- Nunca commitar senhas, tokens, chaves privadas, keystore, credenciais ou códigos secretos.
- Assinatura Android deve continuar usando Repository Secrets.
- Não inserir credenciais do Notion, IA externa ou outros serviços no código/documentação.
- `android:allowBackup="false"` é parte da configuração atual e não deve ser mudado sem razão analisada.

## 9. Código e nomenclatura

- Package/namespace: `com.babycatbe.salvarnaia`.
- Manter Java como linguagem principal enquanto não houver decisão de migração.
- Respeitar os nomes e responsabilidades atuais das classes.
- Preferir nomes descritivos e pequenos helpers a duplicação.
- UI atual é programática; novas telas/componentes devem reutilizar os padrões de `MainActivity` ou documentar uma mudança deliberada em `design.md`.
- Mensagens de erro para o usuário devem ser compreensíveis e não fingir sucesso quando a operação falhou.

## 10. Build e compatibilidade

Configuração vigente:
- Java 21.
- AGP 9.4.0.
- Gradle 9.6 no CI.
- `compileSdk/targetSdk 36`.
- `minSdk 26`.
- release assinado via GitHub Actions.

Não alterar esses elementos sem necessidade real e validação do impacto.

## 11. Critério mínimo de conclusão

Uma mudança técnica relevante só pode ser considerada concluída nesta sequência:

1. implementação coerente com fontes atuais;
2. revisão do impacto em código/dados;
3. build concluído quando aplicável;
4. APK instalado quando o comportamento depende do aparelho;
5. teste real do cenário;
6. resultado registrado com estado correto;
7. arquivos de contexto/documentação atualizados quando a mudança afetar produto, arquitetura, regra, design, tarefa ou decisão.

Build aprovado significa **COMPILADO**, não **TESTADO**.

## 12. Manutenção dos arquivos de contexto

- produto/escopo/funcionalidade → `prd.md`;
- estrutura/tecnologia/fluxo/banco → `architecture.md`;
- regra permanente → `rules.md`;
- padrão visual → `design.md`;
- fase/prioridade/tarefa atual → `task.md`;
- decisão/aprendizado/erro relevante persistente → `memory.md`.

Não duplicar o Contexto Mestre inteiro. Cada arquivo deve conter apenas o contexto necessário à sua função.
