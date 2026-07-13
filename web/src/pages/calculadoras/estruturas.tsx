import React from 'react';
import { Anchor, Blocks, Columns3, Layers, LayoutGrid, PanelTop, Ruler, Square } from 'lucide-react';
import { EstruturaCalculator } from './EstruturaCalculator';
import type { EstruturaConfig } from './EstruturaCalculator';

// Fórmulas replicadas da planilha Sienge de levantamento de quantitativos.
// Validadas contra a linha de exemplo da aba Baldrame:
// L=0,15 H=0,20 C=46,35 P=0,30 -> concreto 1,3905 | forma 18,54 | escav. 10,42875
// reaterro 8,458875 | bota-fora 11,5875 | lastro 0,579375

const cores = {
  concreto: 'var(--success)',
  escavacao: 'var(--warning)',
};

const resultadosFundacao = [
  { key: 'concreto', label: 'Concreto', unidade: 'm³', cor: cores.concreto },
  { key: 'forma', label: 'Forma', unidade: 'm²' },
  { key: 'escavacao', label: 'Escavação', unidade: 'm³', cor: cores.escavacao },
  { key: 'reaterro', label: 'Reaterro', unidade: 'm³' },
  { key: 'botaFora', label: 'Bota-fora', unidade: 'm³' },
  { key: 'lastro', label: 'Lastro', unidade: 'm³' },
];

const paramLastro = [{ key: 'eLastro', label: 'Espessura do lastro (m)', valorInicial: 0.05 }];

// ---------- Baldrame ----------

const baldrameConfig: EstruturaConfig = {
  titulo: 'Baldrame',
  subtitulo: 'Vigas baldrame: concreto, forma, movimento de terra, lastro e aço.',
  nomeLinha: 'Viga',
  placeholderNome: 'baldrame01',
  campos: [
    { key: 'L', label: 'Largura L (m)' },
    { key: 'H', label: 'Altura H (m)' },
    { key: 'C', label: 'Comprim. C (m)' },
    { key: 'P', label: 'Prof. escav. P (m)' },
  ],
  parametros: paramLastro,
  resultados: resultadosFundacao,
  resultadoDestaque: 'concreto',
  comAco: true,
  calcular: ({ qnt, L, H, C, P }, { eLastro }) => {
    const concreto = qnt * L * H * C;
    const escavacao = qnt * (L + 0.6) * P * C; // vala com folga de 30 cm por lado
    const lastro = qnt * (L + 0.1) * eLastro * C; // lastro com sobra de 5 cm por lado
    return {
      concreto,
      forma: qnt * 2 * H * C,
      escavacao,
      lastro,
      reaterro: Math.max(escavacao - concreto - lastro, 0),
      botaFora: escavacao / 0.9, // empolamento do solo escavado
    };
  },
};

export const BaldrameCalculator: React.FC = () => (
  <EstruturaCalculator config={baldrameConfig} icon={<Layers color="var(--accent-primary)" />} />
);

// ---------- Blocos de fundação ----------

const blocosConfig: EstruturaConfig = {
  titulo: 'Blocos de Fundação',
  subtitulo: 'Blocos de coroamento: concreto, forma, movimento de terra, lastro e aço.',
  nomeLinha: 'Bloco',
  placeholderNome: 'B1',
  campos: [
    { key: 'L', label: 'Largura L (m)' },
    { key: 'H', label: 'Altura H (m)' },
    { key: 'C', label: 'Comprim. C (m)' },
    { key: 'P', label: 'Prof. escav. P (m)' },
  ],
  parametros: paramLastro,
  resultados: resultadosFundacao,
  resultadoDestaque: 'concreto',
  comAco: true,
  calcular: ({ qnt, L, H, C, P }, { eLastro }) => {
    const concreto = qnt * L * H * C;
    const escavacao = qnt * (L + 0.6) * (C + 0.6) * P;
    const lastro = qnt * (L + 0.1) * (C + 0.1) * eLastro;
    return {
      concreto,
      forma: qnt * 2 * (L + C) * H,
      escavacao,
      lastro,
      reaterro: Math.max(escavacao - concreto - lastro, 0),
      botaFora: escavacao / 0.9,
    };
  },
};

export const BlocosCalculator: React.FC = () => (
  <EstruturaCalculator config={blocosConfig} icon={<Blocks color="var(--accent-primary)" />} />
);

// ---------- Radier ----------

const radierConfig: EstruturaConfig = {
  titulo: 'Radier',
  subtitulo: 'Placas de fundação direta: concreto, forma, escavação, lastro e aço/tela.',
  nomeLinha: 'Radier',
  placeholderNome: 'R1',
  campos: [
    { key: 'L', label: 'Largura L (m)' },
    { key: 'H', label: 'Espessura H (m)' },
    { key: 'C', label: 'Comprim. C (m)' },
    { key: 'P', label: 'Prof. escav. P (m)' },
  ],
  parametros: paramLastro,
  resultados: resultadosFundacao,
  resultadoDestaque: 'concreto',
  comAco: true,
  calcular: ({ qnt, L, H, C, P }, { eLastro }) => {
    const concreto = qnt * L * H * C;
    const escavacao = qnt * L * C * P;
    const lastro = qnt * L * C * eLastro;
    return {
      concreto,
      forma: qnt * 2 * (L + C) * H,
      escavacao,
      lastro,
      reaterro: Math.max(escavacao - concreto - lastro, 0),
      botaFora: escavacao / 0.9,
    };
  },
};

export const RadierCalculator: React.FC = () => (
  <EstruturaCalculator config={radierConfig} icon={<LayoutGrid color="var(--accent-primary)" />} />
);

// ---------- Sapatas ----------

const sapatasConfig: EstruturaConfig = {
  titulo: 'Sapatas',
  subtitulo: 'Sapatas isoladas com tronco piramidal: concreto, forma e movimento de terra.',
  nomeLinha: 'Sapata',
  placeholderNome: 'S1',
  campos: [
    { key: 'Lb', label: 'Larg. base Lb (m)' },
    { key: 'Cb', label: 'Compr. base Cb (m)' },
    { key: 'Lf', label: 'Larg. fuste Lf (m)' },
    { key: 'Cf', label: 'Compr. fuste Cf (m)' },
    { key: 'H', label: 'Alt. tronco H (m)' },
    { key: 'B', label: 'Alt. base B (m)' },
    { key: 'P', label: 'Profund. P (m)' },
  ],
  parametros: paramLastro,
  resultados: resultadosFundacao,
  resultadoDestaque: 'concreto',
  calcular: ({ qnt, Lb, Cb, Lf, Cf, H, B, P }, { eLastro }) => {
    const areaBase = Lb * Cb;
    const areaFuste = Lf * Cf;
    // Tronco de pirâmide entre a base e o fuste
    const tronco = (H / 3) * (areaBase + areaFuste + Math.sqrt(areaBase * areaFuste));
    const concreto = qnt * (areaBase * B + tronco);
    const escavacao = qnt * (Lb + 0.6) * (Cb + 0.6) * P;
    const lastro = qnt * (Lb + 0.1) * (Cb + 0.1) * eLastro;
    return {
      concreto,
      forma: qnt * 2 * (Lb + Cb) * B,
      escavacao,
      lastro,
      reaterro: Math.max(escavacao - concreto - lastro, 0),
      botaFora: escavacao / 0.9,
    };
  },
};

export const SapatasCalculator: React.FC = () => (
  <EstruturaCalculator config={sapatasConfig} icon={<Anchor color="var(--accent-primary)" />} />
);

// ---------- Tubulões ----------

const tubuloesConfig: EstruturaConfig = {
  titulo: 'Tubulões',
  subtitulo: 'Tubulões a céu aberto: volumes de concreto, escavação e bota-fora.',
  nomeLinha: 'Tubulão',
  placeholderNome: 'T1',
  campos: [
    { key: 'Df', label: 'Ø fuste Df (m)' },
    { key: 'Hf', label: 'Alt. fuste Hf (m)' },
    { key: 'Db', label: 'Ø base Db (m)' },
    { key: 'Hb', label: 'Alt. base Hb (m)' },
    { key: 'b', label: 'Rodapé base b (m)' },
  ],
  resultados: [
    { key: 'concreto', label: 'Concreto', unidade: 'm³', cor: cores.concreto },
    { key: 'escavacao', label: 'Escavação', unidade: 'm³', cor: cores.escavacao },
    { key: 'botaFora', label: 'Bota-fora', unidade: 'm³' },
  ],
  resultadoDestaque: 'concreto',
  calcular: ({ qnt, Df, Hf, Db, Hb, b }) => {
    const fuste = (Math.PI * Df * Df / 4) * Hf;
    // Base alargada: cilindro (rodapé) + tronco de cone entre Db e Df
    const rodape = (Math.PI * Db * Db / 4) * b;
    const troncoCone = (Math.PI * Hb / 12) * (Db * Db + Db * Df + Df * Df);
    const concreto = qnt * (fuste + rodape + troncoCone);
    return {
      concreto,
      escavacao: concreto,
      botaFora: concreto / 0.9,
    };
  },
};

export const TubuloesCalculator: React.FC = () => (
  <EstruturaCalculator config={tubuloesConfig} icon={<Ruler color="var(--accent-primary)" />} />
);

// ---------- Pilares ----------

const pilaresConfig: EstruturaConfig = {
  titulo: 'Pilares',
  subtitulo: 'Pilares retangulares: concreto, forma e aço.',
  nomeLinha: 'Pilar',
  placeholderNome: 'P1',
  campos: [
    { key: 'L', label: 'Largura L (m)' },
    { key: 'H', label: 'Altura seção H (m)' },
    { key: 'C', label: 'Altura pilar C (m)' },
  ],
  resultados: [
    { key: 'concreto', label: 'Concreto', unidade: 'm³', cor: cores.concreto },
    { key: 'forma', label: 'Forma', unidade: 'm²' },
  ],
  resultadoDestaque: 'concreto',
  comAco: true,
  calcular: ({ qnt, L, H, C }) => ({
    concreto: qnt * L * H * C,
    forma: qnt * 2 * (L + H) * C,
  }),
};

export const PilaresCalculator: React.FC = () => (
  <EstruturaCalculator config={pilaresConfig} icon={<Columns3 color="var(--accent-primary)" />} />
);

// ---------- Vigas superiores ----------

const vigasConfig: EstruturaConfig = {
  titulo: 'Vigas Superiores',
  subtitulo: 'Vigas de concreto armado: concreto, forma, escoramento e aço.',
  nomeLinha: 'Viga',
  placeholderNome: 'V1',
  campos: [
    { key: 'L', label: 'Largura L (m)' },
    { key: 'H', label: 'Altura H (m)' },
    { key: 'C', label: 'Comprim. C (m)' },
    { key: 'hFundo', label: 'Alt. fundo viga (m)' },
  ],
  resultados: [
    { key: 'concreto', label: 'Concreto', unidade: 'm³', cor: cores.concreto },
    { key: 'forma', label: 'Forma', unidade: 'm²' },
    { key: 'escoramento', label: 'Escoramento', unidade: 'm³', cor: cores.escavacao },
  ],
  resultadoDestaque: 'concreto',
  comAco: true,
  calcular: ({ qnt, L, H, C, hFundo }) => ({
    concreto: qnt * L * H * C,
    forma: qnt * (2 * H + L) * C, // duas laterais + fundo
    escoramento: qnt * L * C * hFundo,
  }),
};

export const VigasSuperioresCalculator: React.FC = () => (
  <EstruturaCalculator config={vigasConfig} icon={<PanelTop color="var(--accent-primary)" />} />
);

// ---------- Lajes ----------

const lajesConfig: EstruturaConfig = {
  titulo: 'Lajes Maciças',
  subtitulo: 'Lajes maciças: concreto, forma, escoramento e aço/tela.',
  nomeLinha: 'Laje',
  placeholderNome: 'L1',
  campos: [
    { key: 'L', label: 'Largura L (m)' },
    { key: 'H', label: 'Espessura H (m)' },
    { key: 'C', label: 'Comprim. C (m)' },
    { key: 'Pd', label: 'Pé-direito Pd (m)' },
  ],
  resultados: [
    { key: 'concreto', label: 'Concreto', unidade: 'm³', cor: cores.concreto },
    { key: 'forma', label: 'Forma', unidade: 'm²' },
    { key: 'escoramento', label: 'Escoramento', unidade: 'm³', cor: cores.escavacao },
  ],
  resultadoDestaque: 'concreto',
  comAco: true,
  calcular: ({ qnt, L, H, C, Pd }) => ({
    concreto: qnt * L * H * C,
    forma: qnt * L * C,
    escoramento: qnt * L * C * Pd,
  }),
};

export const LajesCalculator: React.FC = () => (
  <EstruturaCalculator config={lajesConfig} icon={<Square color="var(--accent-primary)" />} />
);
