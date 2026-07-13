import React from 'react';
import { Droplets, PlugZap, Waves, Zap } from 'lucide-react';
import { InstalacoesCalculator } from './InstalacoesCalculator';
import type { InstalacaoConfig } from './InstalacoesCalculator';

// Catálogos de materiais extraídos da planilha Sienge (abas de instalações).
// A planilha orienta "troque conforme seu projeto" — os itens abaixo são o
// ponto de partida padrão.

// ---------- Elétrica: Distribuição ----------

const eletricaDistribuicaoConfig: InstalacaoConfig = {
  titulo: 'Elétrica — Distribuição',
  subtitulo: 'Levantamento da instalação elétrica de distribuição por local/circuito.',
  localLabel: 'Local / Circuito',
  grupos: [
    {
      nome: 'Eletrodutos (m)',
      unidade: 'm',
      itens: ['PVC Ø 3/4"', 'PVC Ø 1"', 'PVC Ø 1 1/4"', 'PVC Ø 1 1/2"', 'PVC Ø 4"', 'Ferro galvanizado Ø 1"'],
    },
    {
      nome: 'Eletrocalhas e Perfilados (m)',
      unidade: 'm',
      itens: ['Eletrocalha 300x100mm', 'Eletrocalha 600x1200mm', 'Perfilado liso 38 x 38 mm'],
    },
    {
      nome: 'Fios e cabos elétricos (m)',
      unidade: 'm',
      itens: ['2,5 mm²', '4 mm²', '6 mm²', '10 mm²', '16 mm²', '120 mm²'],
    },
    {
      nome: 'Conduletes (un)',
      unidade: 'un',
      itens: ['L Ø 1"', 'I Ø 3/4"', 'L Ø 3/4"', 'T Ø 3/4"'],
    },
    {
      nome: 'Tomadas (un)',
      unidade: 'un',
      itens: ['300 W', '4500 W (chuveiro)', '1200 W (ar condicionado)'],
    },
    {
      nome: 'Interruptores (un)',
      unidade: 'un',
      itens: ['1 seção', '2 seções', 'Cigarra', 'Campainha'],
    },
    {
      nome: 'Caixas (un)',
      unidade: 'un',
      itens: ['4x2', '4x4', 'Octogonal', 'Cx chapa aço 25x140 mm', 'Cx chapa aço 152x152x82 mm'],
    },
    {
      nome: 'Luminárias (un)',
      unidade: 'un',
      itens: [
        'Fluorescente 2 lâmp. 20 W, calha sobrepor',
        'Fluorescente 2 lâmp. 40 W, calha sobrepor',
        'Fluorescente 2 lâmp. 65 W, calha sobrepor',
        'Fluorescente 4 lâmp. 20 W, calha sobrepor',
        'Fluorescente 4 lâmp. 40 W, calha embutir',
        'Plafonier com globo leitoso e lâmpada de 60 W',
      ],
    },
  ],
};

export const EletricaDistribuicaoCalculator: React.FC = () => (
  <InstalacoesCalculator config={eletricaDistribuicaoConfig} icon={<Zap color="var(--accent-primary)" />} />
);

// ---------- Elétrica: Prumadas ----------

const eletricaPrumadasConfig: InstalacaoConfig = {
  titulo: 'Elétrica — Prumadas',
  subtitulo: 'Levantamento da instalação elétrica de prumadas.',
  localLabel: 'Prumada',
  grupos: [
    {
      nome: 'Eletrodutos (m)',
      unidade: 'm',
      itens: ['PVC Ø 1"', 'PVC Ø 1 1/4"', 'PVC Ø 1 1/2"', 'PVC Ø 4"', 'Ferro galvanizado Ø 1"'],
    },
    {
      nome: 'Eletrocalhas e Perfilados (m)',
      unidade: 'm',
      itens: ['Eletrocalha 300x100mm', 'Eletrocalha 600x1200mm', 'Perfilado liso 38 x 38 mm'],
    },
    {
      nome: 'Fios e cabos elétricos (m)',
      unidade: 'm',
      itens: ['2,5 mm²', '4 mm²', '6 mm²', '10 mm²', '16 mm²', '120 mm²'],
    },
    {
      nome: 'Caixas (un)',
      unidade: 'un',
      itens: ['Cx chapa aço 25x140 mm', 'Cx chapa aço 152x152x82 mm', 'Octogonal'],
    },
  ],
};

export const EletricaPrumadasCalculator: React.FC = () => (
  <InstalacoesCalculator config={eletricaPrumadasConfig} icon={<PlugZap color="var(--accent-primary)" />} />
);

// ---------- Hidráulica ----------

const hidraulicaConfig: InstalacaoConfig = {
  titulo: 'Hidráulica',
  subtitulo: 'Levantamento da instalação hidráulica (água fria/quente).',
  localLabel: 'Descrição / Ambiente',
  grupos: [
    {
      nome: 'Tubulação PVC (m)',
      unidade: 'm',
      itens: ['Soldável Ø 20 mm', 'Soldável Ø 25 mm', 'Soldável Ø 40 mm', 'Soldável Ø 60 mm', 'Soldável Ø 75 mm'],
    },
    {
      nome: 'Conexões PVC (un)',
      unidade: 'un',
      itens: ['Tê soldável Ø 20 mm', 'Luva soldável Ø 25 mm', 'Adaptador Ø 40 mm', 'Curva Ø 60 mm', 'União Ø 75 mm'],
    },
    {
      nome: 'Tubulação cobre (m)',
      unidade: 'm',
      itens: [
        'Soldável Ø 15 mm (1/2")',
        'Soldável Ø 22 mm (3/4")',
        'Soldável Ø 28 mm (1")',
        'Soldável Ø 35 mm (1 1/4")',
        'Soldável Ø 42 mm (1 1/2")',
      ],
    },
    {
      nome: 'Conexões cobre (un)',
      unidade: 'un',
      itens: [
        'Conector Ø 22 mm x 3/4"',
        'Tampão soldável de bronze bolsa Ø 35 mm (1 1/4")',
        'Luva soldável de bronze bolsa x bolsa Ø 66 mm (2 1/2")',
        'Tê de redução central soldável cobre/bronze Ø 22 x 15 x 22 mm',
        'Curva 45° soldável de cobre bolsa x bolsa Ø 22 mm (3/4")',
      ],
    },
    {
      nome: 'Registros e válvulas (un)',
      unidade: 'un',
      itens: [
        'Registro de gaveta bruto Ø 20 mm (3/4")',
        'Registro de gaveta bruto c/ adaptador soldável PVC Ø 20 mm (3/4")',
        'Registro de pressão com canopla Ø 15 mm (1/2")',
        'Válvula de retenção horizontal/vertical Ø 20 mm (3/4")',
        'Válvula de retenção de pé com crivo Ø 25 mm (1")',
      ],
    },
    {
      nome: 'Diversos (un)',
      unidade: 'un',
      itens: [
        "Reservatório d'água fibra de vidro cilíndrico 1000 L",
        'Aquecedor de acumulação (boiler) a gás 400 L',
        'Torneira de boia Ø 25 mm (1")',
      ],
    },
  ],
};

export const HidraulicaCalculator: React.FC = () => (
  <InstalacoesCalculator config={hidraulicaConfig} icon={<Droplets color="var(--accent-primary)" />} />
);

// ---------- Esgoto e Pluvial ----------

const esgotoPluvialConfig: InstalacaoConfig = {
  titulo: 'Esgoto e Pluvial',
  subtitulo: 'Levantamento das redes de esgoto e de água pluvial.',
  localLabel: 'Descrição / Trecho',
  grupos: [
    {
      nome: 'Tubulação PVC (m)',
      unidade: 'm',
      itens: [
        'Tubo PVC branco ponta e bolsa soldável Ø 40 mm',
        'Tubo PVC branco ponta, bolsa e virola Ø 50 mm',
        'Tubo PVC branco ponta, bolsa e virola Ø 75 mm',
        'Tubo PVC branco ponta, bolsa e virola Ø 100 mm',
        'Tubo PVC branco ponta, bolsa e virola Ø 150 mm',
      ],
    },
    {
      nome: 'Conexões PVC (un)',
      unidade: 'un',
      itens: [
        'Joelho 45° PVC branco soldável Ø 40 mm',
        'Curva 45° longa PVC branco Ø 100 mm',
        'Joelho 90° PVC branco Ø 50 mm',
        'Junção 45° PVC branco com redução Ø 75 x 50 mm',
        'Luva simples PVC branco Ø 50 mm',
      ],
    },
    {
      nome: 'Tubulação ferro fundido (m)',
      unidade: 'm',
      itens: [
        'Tubo FoFo junta elástica Ø 50 mm (2")',
        'Tubo FoFo junta elástica Ø 75 mm (3")',
        'Tubo FoFo junta elástica Ø 100 mm (4")',
        'Tubo FoFo junta elástica Ø 150 mm (6")',
      ],
    },
    {
      nome: 'Conexões ferro fundido (un)',
      unidade: 'un',
      itens: [
        'Joelho 45° FoFo junta elástica Ø 100 mm (4")',
        'Junção 45° FoFo junta elástica Ø 75 x 75 mm (3 x 3")',
        'Luva FoFo junta elástica bipartida Ø 75 mm (3")',
        'Placa cega FoFo Ø 75 mm (3")',
        'Tê 90° sanitário FoFo junta elástica Ø 75 x 75 mm (3 x 3")',
      ],
    },
    {
      nome: 'Acessórios para esgoto',
      unidade: 'un',
      itens: [
        'Caixa sifonada PVC grelha branca 100 x 100 x 50 mm',
        'Caixa sifonada PVC grelha branca 150 x 150 x 50 mm',
        'Caixa sifonada PVC grelha de alumínio 100 x 100 x 50 mm',
        'Ralo FoFo seco saída vertical grelha cromada Ø 100 mm',
        'Ralo PVC rígido seco 100 x 50 x 40 mm',
        'Ralo PVC rígido sifonado 100 x 70 x 40 mm',
        'Caixa seca PVC rígido 100 x 100 x 40 mm',
        'Caixa de inspeção de polietileno Ø 100 mm',
        'Grelha ferro fundido p/ canaleta largura 20 cm (m)',
        'Grelha ferro fundido p/ canaleta largura 25 cm (m)',
      ],
    },
  ],
};

export const EsgotoPluvialCalculator: React.FC = () => (
  <InstalacoesCalculator config={esgotoPluvialConfig} icon={<Waves color="var(--accent-primary)" />} />
);
