import React from 'react';

// ---------- Estilos base compartilhados entre as calculadoras ----------

export const inputStyle: React.CSSProperties = {
  width: '100%',
  padding: '0.6rem',
  borderRadius: '4px',
  border: '1px solid var(--border-color)',
  background: 'var(--bg-tertiary)',
  color: 'var(--text-primary)',
};

export const labelStyle: React.CSSProperties = {
  display: 'block',
  fontSize: '0.8rem',
  color: 'var(--text-secondary)',
  marginBottom: '0.3rem',
  whiteSpace: 'nowrap',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
};

export const panelStyle: React.CSSProperties = { padding: '1.5rem' };

export const resumoPanelStyle: React.CSSProperties = {
  padding: '1.5rem',
  background: 'linear-gradient(145deg, rgba(30,41,59,0.7) 0%, rgba(15,23,42,0.9) 100%)',
  border: '1px solid rgba(255,255,255,0.1)',
};

export const fmt = (n: number, casas = 2) =>
  n.toLocaleString('pt-BR', { minimumFractionDigits: casas, maximumFractionDigits: casas });

// ---------- Inputs pequenos ----------

interface NumFieldProps {
  label: string;
  value: number;
  onChange: (v: number) => void;
  step?: number;
  min?: number;
}

export const NumField: React.FC<NumFieldProps> = ({ label, value, onChange, step = 0.01, min = 0 }) => (
  <div>
    <label style={labelStyle} title={label}>{label}</label>
    <input
      type="number"
      min={min}
      step={step}
      value={value || ''}
      onChange={(e) => onChange(parseFloat(e.target.value) || 0)}
      style={inputStyle}
    />
  </div>
);

interface TextFieldProps {
  label: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
}

export const TextField: React.FC<TextFieldProps> = ({ label, value, onChange, placeholder }) => (
  <div>
    <label style={labelStyle} title={label}>{label}</label>
    <input type="text" value={value} placeholder={placeholder} onChange={(e) => onChange(e.target.value)} style={inputStyle} />
  </div>
);

interface SelectFieldProps {
  label: string;
  value: string;
  onChange: (v: string) => void;
  options: string[];
}

export const SelectField: React.FC<SelectFieldProps> = ({ label, value, onChange, options }) => (
  <div>
    <label style={labelStyle} title={label}>{label}</label>
    <select value={value} onChange={(e) => onChange(e.target.value)} style={inputStyle}>
      {options.map((o) => (
        <option key={o} value={o}>{o}</option>
      ))}
    </select>
  </div>
);

// ---------- Cabeçalho padrão de página ----------

interface PageHeaderProps {
  icon: React.ReactNode;
  titulo: string;
  subtitulo: string;
}

export const PageHeader: React.FC<PageHeaderProps> = ({ icon, titulo, subtitulo }) => (
  <div style={{ marginBottom: '2rem' }}>
    <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '1.8rem' }}>
      {icon} {titulo}
    </h1>
    <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem' }}>{subtitulo}</p>
  </div>
);

// ---------- Linha de resultado no painel de resumo ----------

interface ResultRowProps {
  label: string;
  valor: number;
  unidade: string;
  cor?: string;
  destaque?: boolean;
}

export const ResultRow: React.FC<ResultRowProps> = ({ label, valor, unidade, cor, destaque }) => (
  <div
    style={{
      display: 'flex',
      justifyContent: 'space-between',
      alignItems: 'center',
      color: cor ?? 'var(--text-secondary)',
      ...(destaque
        ? { marginTop: '0.5rem', paddingTop: '0.8rem', borderTop: '1px solid rgba(255,255,255,0.1)' }
        : {}),
    }}
  >
    <span style={destaque ? { fontSize: '1.05rem', fontWeight: 500, color: 'white' } : undefined}>{label}</span>
    <span style={destaque ? { fontSize: '1.3rem', fontWeight: 700, color: cor ?? 'var(--success)' } : undefined}>
      {fmt(valor)} {unidade}
    </span>
  </div>
);

// ---------- Aço: tabelas de peso extraídas da planilha Sienge ----------

// Aço CA-50 / CA-25 — bitola -> kg/m
export const ACO_CA50: Record<string, number> = {
  '6,3mm (1/4")': 0.25,
  '8mm (5/16")': 0.4,
  '10mm (3/8")': 0.63,
  '12,5mm (1/2")': 1,
  '16mm (5/8")': 1.6,
  '20mm (3/4")': 2.5,
  '22,3mm (7/8")': 3,
  '25mm (1")': 4,
  '32mm (1 1/4")': 6.3,
};

// Aço CA-60 — bitola -> kg/m
export const ACO_CA60: Record<string, number> = {
  '3,4mm': 0.07,
  '4,2mm': 0.11,
  '4,6mm': 0.13,
  '5mm': 0.16,
  '6mm': 0.23,
  '6,4mm': 0.26,
  '7mm': 0.3,
  '8mm': 0.4,
};

// Telas soldadas — tipo -> kg/m²
export const TELAS: Record<string, number> = {
  Q61: 0.97, Q75: 1.27, Q92: 1.48, L92: 1.12, Q113: 1.8, L113: 1.21, T113: 1.22,
  Q138: 2.2, R138: 1.83, M138: 1.65, L138: 1.47, T138: 1.49,
  Q159: 2.52, R159: 2.11, M159: 1.9, L159: 1.69,
  Q196: 3.11, R196: 2.6, M196: 2.34, L196: 2.09, T196: 2.11,
  Q246: 3.91, R246: 3.26, M246: 2.94, L246: 2.62, T246: 2.64,
  Q283: 4.48, R283: 3.74, M283: 3.37, L283: 3.0, T283: 3.03,
  Q335: 5.37, L335: 3.48, T335: 3.45,
  Q396: 6.28, R396: 5.24, M396: 4.73, L396: 3.91, T396: 3.92,
  Q503: 7.97, R503: 6.66, M503: 6.0, L503: 4.77, T503: 4.76,
  Q636: 10.09, L636: 5.84, Q785: 12.46, L785: 7.03, LA1227: 10.87,
};

export type CategoriaAco = 'CA50' | 'CA60' | 'TELA';

export interface AcoItem {
  id: string;
  categoria: CategoriaAco;
  bitola: string;
  // metros para barras (CA-50/CA-60), m² para telas
  quantidade: number;
}

const TABELAS: Record<CategoriaAco, Record<string, number>> = {
  CA50: ACO_CA50,
  CA60: ACO_CA60,
  TELA: TELAS,
};

const CATEGORIA_LABEL: Record<CategoriaAco, string> = {
  CA50: 'CA-50 / CA-25 (barras)',
  CA60: 'CA-60 (barras)',
  TELA: 'Tela soldada',
};

export const pesoAcoItem = (item: AcoItem): number =>
  (TABELAS[item.categoria][item.bitola] ?? 0) * item.quantidade;

export interface AcoTotais {
  ca50: number;
  ca60: number;
  tela: number;
  total: number;
}

export const calcAcoTotais = (itens: AcoItem[]): AcoTotais => {
  const soma = (cat: CategoriaAco) =>
    itens.filter((i) => i.categoria === cat).reduce((acc, i) => acc + pesoAcoItem(i), 0);
  const ca50 = soma('CA50');
  const ca60 = soma('CA60');
  const tela = soma('TELA');
  return { ca50, ca60, tela, total: ca50 + ca60 + tela };
};

export const novoAcoItem = (categoria: CategoriaAco = 'CA50'): AcoItem => ({
  id: crypto.randomUUID(),
  categoria,
  bitola: Object.keys(TABELAS[categoria])[0],
  quantidade: 0,
});

interface AcoSectionProps {
  itens: AcoItem[];
  setItens: (itens: AcoItem[]) => void;
}

// Seção de aço reutilizável: o usuário escolhe categoria + bitola e informa
// o comprimento total (m) ou a área (m²); o peso sai da tabela da planilha.
export const AcoSection: React.FC<AcoSectionProps> = ({ itens, setItens }) => {
  const update = (id: string, patch: Partial<AcoItem>) => {
    setItens(itens.map((i) => {
      if (i.id !== id) return i;
      const novo = { ...i, ...patch };
      // Ao trocar de categoria, garante bitola válida
      if (patch.categoria && !(novo.bitola in TABELAS[novo.categoria])) {
        novo.bitola = Object.keys(TABELAS[novo.categoria])[0];
      }
      return novo;
    }));
  };

  return (
    <div className="glass-panel" style={panelStyle}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h2 style={{ fontSize: '1.2rem' }}>Aço (armaduras e telas)</h2>
        <button
          onClick={() => setItens([...itens, novoAcoItem()])}
          className="btn-secondary"
          style={{ padding: '0.4rem 0.8rem', fontSize: '0.9rem' }}
        >
          + Adicionar aço
        </button>
      </div>

      {itens.length === 0 && (
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', fontStyle: 'italic' }}>
          Nenhum aço lançado. Adicione barras CA-50/CA-60 ou telas soldadas.
        </p>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        {itens.map((item) => (
          <div key={item.id} style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr 1fr 1fr 40px', gap: '1rem', alignItems: 'end' }}>
            <div>
              <label style={labelStyle}>Categoria</label>
              <select
                value={item.categoria}
                onChange={(e) => update(item.id, { categoria: e.target.value as CategoriaAco })}
                style={inputStyle}
              >
                {(Object.keys(CATEGORIA_LABEL) as CategoriaAco[]).map((c) => (
                  <option key={c} value={c}>{CATEGORIA_LABEL[c]}</option>
                ))}
              </select>
            </div>
            <SelectField
              label="Bitola / Tipo"
              value={item.bitola}
              onChange={(v) => update(item.id, { bitola: v })}
              options={Object.keys(TABELAS[item.categoria])}
            />
            <NumField
              label={item.categoria === 'TELA' ? 'Área (m²)' : 'Comprim. (m)'}
              value={item.quantidade}
              onChange={(v) => update(item.id, { quantidade: v })}
            />
            <div>
              <label style={labelStyle}>Peso (kg)</label>
              <div style={{ ...inputStyle, background: 'transparent', color: 'var(--success)', fontWeight: 600 }}>
                {fmt(pesoAcoItem(item))}
              </div>
            </div>
            <button
              onClick={() => setItens(itens.filter((i) => i.id !== item.id))}
              style={{ background: 'transparent', border: 'none', color: 'var(--danger)', cursor: 'pointer', padding: '0.6rem', fontSize: '1rem' }}
              title="Remover"
            >
              ✕
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};
