# AlkaAnvil

Controle completo das mecânicas da bigorna para a rede AlkaStudio (Paper 1.21.8 /
Java 21) — construído sobre o AlkaCore (GUI compartilhada). Substituto próprio
planejado para o **CustomAnvil** (terceiro).

## O que faz

- **Custo por multiplicador de encantamento** (`enchant-values` no `config.yml`,
  valores portados da [mecânica real do vanilla](https://minecraft.wiki/w/Anvil_mechanics#Costs_for_combining_enchantments))
  em vez de um custo fixo — cada encantamento combinado custa `nível × multiplicador`,
  com multiplicador diferente se a fonte é um item ou um livro, igual ao Minecraft
  original.
- **Limites e conflitos de encantamento configuráveis** — só para encantamentos
  vanilla; encantamentos do AdvancedEnchantments (quando presente) validam os
  próprios limites/conflitos internamente, evitando duplicar uma regra que o AE já
  garante.
- **Vanilla + AdvancedEnchantments** via `enchant/AlkaEnchantmentRegistry` — AE é
  soft-dependency: sem ele instalado, tudo continua funcionando normalmente, só os
  encantamentos AE ficam indisponíveis na bigorna.
- **Disenchant** (item + livro em branco → livro encantado + item limpo) e
  **Shatter** (livro com 2+ encantamentos → dois livros) — escopo vanilla-only
  (encantamentos AE já têm formato de livro próprio, fora do escopo desta versão).
- **Custo em moeda opcional** (`monetary-cost`, via AlkaEconomy) como alternativa ao
  custo em XP — cobrado no momento em que o jogador retira o resultado, não ao abrir
  a bigorna (ver limitação de ProtocolLib abaixo).
- **Reparo por unidade** (diamante em espada de diamante, etc.) configurável por
  material.
- **Estatísticas de item** via PersistentDataContainer (sem tabela no banco) —
  categorias configuráveis (armas, picaretas, machados, arcos, vara de pesca),
  filtro de rastreamento opcional por tag NBT, lore atualizada automaticamente.
- **GUI de admin** (`/alkaanvil config`) — mostra os valores atuais de cada seção
  do config, somente leitura (edição continua via YAML + `/alkaanvil reload`).
- **Cores no rename com 3 níveis de permissão** (`code`/`hex`/`minimessage`) — tags
  não autorizadas pro nível do jogador são escapadas (aparecem como texto literal),
  nunca removidas silenciosamente.

## Dependências

- **AlkaCore** (hard dependency) — GUI compartilhada (`BaseGui`).
- **AdvancedEnchantments**, **AlkaEconomy**, **PlaceholderAPI**, **LuckPerms** —
  todas soft-dependency.

## Limitações conhecidas (v1.0.1)

- `cost-limits.remove-too-expensive` só capa o custo exibido em 39 (o limite real
  do cliente) — não existe "mostrar um preço real acima de 40 em texto verde"
  sem reescrever o pacote via ProtocolLib, que hoje não é usado por nenhum plugin
  da rede em produção.
- Combinações de encantamento ilegais (conflito) são silenciosamente descartadas do
  resultado, sem cobrar uma penalidade separada (o CustomAnvil real tem uma
  `sacrifice_illegal_enchant_cost` para isso).
- `enchantment-limits.max-enchants-per-item` é checado e logado, mas não poda o
  excedente do resultado ainda.
- Disenchant/Shatter só extraem encantamentos vanilla — itens com encantamentos do
  AdvancedEnchantments misturados com vanilla só têm a parte vanilla extraída.
- A fórmula de reparo por combinação de item (durabilidade restante do segundo item
  + bônus) é uma aproximação da mecânica vanilla real, não verificada contra uma
  fonte oficial — vale conferir se os custos parecerem estranhos em teste real.

## Origem

Construído a partir de uma especificação pré-escrita (referenciando o CustomAnvil e
um plugin de disenchant de terceiros como inspiração de mecânica) e do
`ItemStatsTracker-RPG` do próprio autor da rede como referência para o módulo de
estatísticas de item — só a fatia de rastreamento/lore foi portada, não o sistema de
reincarnação/gemas/acessórios daquele plugin (fora de escopo aqui). Antes de
escrever qualquer código, a API real do AdvancedEnchantments foi verificada via
`javap` direto no jar instalado no servidor de desenvolvimento — três lugares
diferentes do ecossistema (esta especificação e dois hooks já existentes no
AlkaVips/AlkaMines) estavam adivinhando uma API incompatível entre si, nunca
validada; os dois hooks pré-existentes foram corrigidos na mesma sessão.
