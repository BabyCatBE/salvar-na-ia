# Design atual — Salvar na IA

> O aplicativo ainda não possui um design system formal separado. Este arquivo documenta o padrão visual real implementado em `MainActivity.java`, que deve ser preservado nas novas telas.

## 1. Direção visual

- Interface clara, pequena e funcional.
- Fundo off-white.
- Branco para superfícies.
- Vermelho escuro como cor principal.
- Bordas suaves, cantos muito arredondados e sombras discretas.
- Estados verde/âmbar/vermelho para sucesso, processamento e problema.
- Pouca ornamentação.
- A UI deve continuar parecendo uma ferramenta pessoal simples, não um painel complexo.

## 2. Paleta implementada

| Token | Valor |
|---|---|
| BG | `#F8F6F4` |
| SURFACE | `#FFFFFF` |
| TEXT | `#231F1E` |
| MUTED | `#706764` |
| RED | `#B71C1C` |
| RED_SOFT | `#FFEDEB` |
| BORDER | `#E8E0DD` |
| GREEN | `#267E47` |
| GREEN_SOFT | `#EBF7EF` |
| AMBER | `#97640C` |
| AMBER_SOFT | `#FFF7E0` |

Botões neutros usam também cinzas muito claros, como `rgb(247,244,243)` e `rgb(239,238,237)`.

## 3. Tipografia

Fonte efetiva: **sans-serif do Android**, normal ou bold.

Tamanhos recorrentes:
- título principal: 29sp bold;
- título de diálogo de confirmação: 24sp bold;
- título de Configurações: 22sp bold;
- título de card: 18–19sp bold;
- títulos internos: 16sp bold;
- botão: 15sp bold;
- corpo: 13–15sp;
- chips/código: 12sp bold;
- labels em caixa alta: 11sp bold.

Cabeçalho **SALVAR NA IA**:
- 12sp bold;
- vermelho;
- `letterSpacing = 0.12`.

Labels de seção:
- 11sp bold;
- muted;
- `letterSpacing = 0.08`.

## 4. Estrutura da tela principal

Container:
- `ScrollView`;
- fundo `BG`;
- padding: 20dp esquerda/direita, 28dp topo, 36dp base.

Ordem atual:
1. barra superior com marca e botão ⚙;
2. título e subtítulo;
3. chips de versão/beta;
4. card da pasta;
5. card da IA local;
6. abas Pendentes/Lixeira;
7. lista de cards.

## 5. Cards

Helper atual `card()`:
- superfície branca;
- borda 1dp;
- raio 22dp;
- padding interno 18dp;
- elevação 2dp;
- espaçamento inferior 12dp.

Novos cards devem partir desse padrão salvo decisão explícita diferente.

## 6. Chips

Helper `chip()`:
- 12sp bold;
- padding horizontal 11dp;
- padding vertical 7dp;
- raio 99dp.

Estados:
- Pronto → verde suave/verde;
- Analisando IA ou Aguardando IA → âmbar suave/âmbar;
- Arquivo ausente, Falha, Erro → vermelho suave/vermelho;
- Baixando → âmbar suave/âmbar;
- código do item → vermelho suave/vermelho.

## 7. Botões

Helper `actionButton()`:
- 15sp bold;
- conteúdo centralizado;
- padding 14dp horizontal / 13dp vertical;
- raio 18dp;
- primário: fundo vermelho + texto branco;
- secundário: fundo neutro claro + borda + texto escuro.

Ações destrutivas usam vermelho ou vermelho suave, sem parecer ação neutra.

Controles rápidos de card:
- circulares;
- 38×38dp;
- ícones simples, como copiar e ×.

## 8. Abas

- Pendentes e Lixeira lado a lado.
- Altura: 46dp.
- Raio: 18dp.
- Selecionada: `RED_SOFT` + texto `RED`.
- Não selecionada: fundo transparente, borda, texto `MUTED`.
- Espaço de 12dp entre a divisão central total (6dp de cada lado).

## 9. Diálogo de confirmação — padrão v0.4.8

Componente único para confirmações próprias do app.

Painel:
- branco;
- borda `BORDER`;
- raio 28dp;
- padding 22dp no topo/lados e 20dp embaixo;
- largura máxima 520dp ou tela menos 32dp;
- fundo externo escurecido com `dimAmount 0.48`.

Ícone:
- 28sp;
- área 68×68dp;
- círculo `RED_SOFT`;
- texto/ícone vermelho.

Título:
- 24sp bold;
- centralizado;
- margem superior 16dp.

Mensagem:
- 15sp;
- `MUTED`;
- centralizada;
- margem superior 12dp;
- line spacing adicional 3dp.

Ações:
- duas colunas de mesma largura;
- margem superior 22dp;
- 6dp de separação de cada lado;
- botão secundário branco com borda vermelha;
- ação principal no padrão primário vermelho.

Esse componente é usado por:
- Cancelar item;
- Remover registro;
- Esvaziar lixeira.

Não voltar a `AlertDialog` nativo para essas confirmações sem decisão de design.

## 10. Configurações

Diálogo próprio:
- raio 26dp;
- padding 20dp (16dp inferior);
- largura máxima 560dp ou tela menos 28dp;
- altura máxima 86% da tela ou 760dp;
- `dimAmount 0.45`.

Campo de prompt:
- 14sp;
- mínimo de 7 linhas;
- fundo branco;
- borda `BORDER`;
- raio 14dp;
- padding 12dp horizontal / 10dp vertical.

Botão **Fechar** permanece visualmente secundário.

## 11. Lixeira

Padrão aprovado na v0.4.7:
- botão **Esvaziar lixeira (N)** com tratamento destrutivo em vermelho suave;
- código do item em chip vermelho suave;
- **↻ Restaurar** neutro;
- **🗑 Excluir agora** destrutivo vermelho.

Esse redesign foi TESTADO/APROVADO no aparelho.

## 12. Responsividade

O layout usa largura total e `ScrollView`, com limites máximos nos diálogos. Não existem breakpoints complexos.

Ao criar novos elementos:
- evitar larguras fixas grandes;
- usar `MATCH_PARENT` para ações/cards;
- limitar diálogos pela largura real da tela;
- manter alvos tocáveis claros;
- não depender de uma resolução específica.

## 13. Regras de consistência

- Reutilizar cores, raios e helpers atuais antes de inventar novos padrões.
- Manter alinhamentos e margens coerentes entre cards.
- Não misturar `AlertDialog` padrão com os diálogos próprios sem necessidade.
- Estados visuais devem refletir estados reais, não apenas estética.
- Mudança visual relevante deve ser registrada aqui após aprovação.
- Compilação não equivale à aprovação visual; mudanças precisam ser vistas no aparelho quando isso for necessário.
