# ADR-0001: Monólito modular com fronteiras por porta

- **Status:** Aceito
- **Data:** 2026-06-30
- **Decisores:** Time / starter base

## Contexto

Precisamos de uma arquitetura que comece simples (time pequeno, deploy único, baixo custo
operacional) sem virar um "big ball of mud" conforme cresce. Microsserviços trariam custo de
rede, observabilidade e transações distribuídas cedo demais.

## Decisão

Adotar um **monólito modular**: um único deployable, dividido em **features** (`api`,
`application`, `domain`, `infrastructure`). Uma feature **não importa** classes de outra
diretamente — a comunicação entre features se dá por **portas** (interfaces em `common`)
implementadas por **adapters** na feature dona do dado.

## Alternativas consideradas

- **Microsserviços** — overhead de infra/rede/transações distribuídas desproporcional ao estágio.
- **Monólito em camadas (sem módulos)** — simples, mas acoplamento cresce sem controle; difícil de extrair depois.

## Consequências

- **Positivas:** simplicidade de operação; transações locais ACID; acoplamento controlado;
  extração futura de uma feature em serviço fica barata (a porta já é a fronteira).
- **Negativas / trade-offs:** disciplina manual para não furar a regra de import entre features;
  o uso de portas adiciona uma indireção (interface + adapter).
- **Impacto:** estrutura de pastas por feature; toda dependência entre features passa por porta
  em `common`. Ver [ARCHITECTURE.md](../../ARCHITECTURE.md).
