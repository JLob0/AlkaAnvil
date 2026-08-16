# AlkaAnvil

> Controle total das mecânicas da bigorna, substituindo plugins de terceiros por uma implementação própria e fiel ao vanilla

![Java](https://img.shields.io/badge/Java-21-orange)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-green)
![Version](https://img.shields.io/badge/Version-1.0.2-blue)
![License](https://img.shields.io/badge/License-Proprietary-red)

---

## 📋 Sobre o Projeto

O **AlkaAnvil** reescreve a bigorna do zero para a rede AlkaStudio: custo de
encantamento fiel à fórmula original do Minecraft, suporte nativo a
encantamentos do AdvancedEnchantments, disenchant/shatter de livros, reparo
por unidade e estatísticas de item — tudo configurável, sem depender de
plugins externos como o CustomAnvil.

## ✨ Funcionalidades Principais

- 💰 **Custo fiel ao vanilla**: cada encantamento combinado custa
  `nível × multiplicador`, com valores diferentes para item ou livro, exatamente
  como a mecânica original de bigorna.
- ⚔️ **Suporte nativo ao AdvancedEnchantments**: encantamentos AE funcionam
  lado a lado com os vanilla na mesma bigorna.
- 🔓 **Disenchant**: transforma item + livro em branco em livro encantado e
  item limpo.
- 📖 **Shatter**: divide um livro com múltiplos encantamentos em dois livros
  separados.
- 🪙 **Custo em moeda opcional**: alternativa ao custo em XP, integrada ao
  AlkaEconomy.
- 🔧 **Reparo por unidade**: configurável por material (ex.: diamante repara
  espada de diamante).
- 📊 **Estatísticas de item**: rastreamento de uso por categoria (armas,
  picaretas, machados, arcos, vara de pesca), com lore atualizada
  automaticamente.
- 🖥️ **GUI de configuração** (`/alkaanvil config`): visualização rápida dos
  valores atuais de cada seção.
- 🎨 **Rename colorido com 3 níveis de permissão** (código de cor, hex ou
  MiniMessage completo).

## 🎮 Comandos

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/alkaanvil <reload\|info\|config\|bigorna>` | Comando principal (alias `/anvil`) | `alkaanvil.use` |
| `/encantar [tipo] <encantamento> [nivel]` | Aplica um encantamento vanilla ou AE no item segurado | `alkaanvil.encant` |

## 🔗 Integrações

Construído sobre o **AlkaCore** (GUI compartilhada). Integra-se com
**AdvancedEnchantments** (encantamentos extras), **AlkaEconomy** (custo em
moeda), **PlaceholderAPI** e **LuckPerms** — todas opcionais.

## 🔧 Tecnologias Utilizadas

- **Java 21**
- **Paper API 1.21.8**
- **AlkaCore** (GUI compartilhada)
- **MiniMessage** para formatação de texto

## ⚙️ Instalação

1. Baixe o `AlkaAnvil.jar` mais recente.
2. Coloque na pasta `plugins/` do servidor.
3. Certifique-se de ter o **AlkaCore** instalado (dependência obrigatória).
4. Reinicie o servidor.
5. Ajuste `plugins/AlkaAnvil/config.yml` conforme necessário e use
   `/alkaanvil reload` para aplicar mudanças.

## 🔐 Permissões

| Permissão | Descrição | Padrão |
|-----------|-----------|--------|
| `alkaanvil.use` | Usar o comando `/alkaanvil` | op |
| `alkaanvil.encant` | Usar o comando `/encantar` | op |
| `alkaanvil.reload` | Recarregar configuração | op |
| `alkaanvil.config` | Abrir a GUI de configuração | op |
| `alkaanvil.bypass.cost` | Bypassar limites de custo | op |
| `alkaanvil.bypass.level` | Bypassar limites de nível de encantamento | op |
| `alkaanvil.bypass.conflict` | Bypassar conflitos de encantamento | op |
| `alkaanvil.disenchant` | Usar disenchantment | true |
| `alkaanvil.shatter` | Usar shatter | true |
| `alkaanvil.rename.color` | Códigos de cor no rename | op |
| `alkaanvil.rename.hex` | Cores hex no rename | op |
| `alkaanvil.rename.mm` | Tags MiniMessage completas no rename | op |

## 📝 Licença

> ⚠️ **Projeto proprietário da AlkaStudio.**
>
> Código fonte destinado exclusivamente ao uso interno da rede `Alka*`.
> Reprodução, distribuição ou uso não autorizado não são permitidos.

## 🎯 Créditos

- **Desenvolvido por**: MestreDEV — AlkaStudio
- **Parte do ecossistema**: `Alka*`

---

<div align="center">

**Desenvolvido com ❤️ pela AlkaStudio**

[![AlkaStudio](https://img.shields.io/badge/AlkaStudio-JLob0-blue)](https://github.com/JLob0)

</div>
