import React, { useMemo, useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import type { AcoItem } from './shared';
import {
  AcoSection, NumField, PageHeader, ResultRow, TextField,
  calcAcoTotais, panelStyle, resumoPanelStyle,
} from './shared';

export interface CampoDef {
  key: string;
  label: string;
}

export interface ParametroDef {
  key: string;
  label: string;
  valorInicial: number;
}

export interface ResultadoDef {
  key: string;
  label: string;
  unidade: string;
  cor?: string;
}

export interface EstruturaConfig {
  titulo: string;
  subtitulo: string;
  nomeLinha: string; // rótulo do elemento, ex.: "Viga"
  placeholderNome: string; // ex.: "V1"
  campos: CampoDef[];
  parametros?: ParametroDef[];
  resultados: ResultadoDef[];
  resultadoDestaque: string; // key do resultado principal
  comAco?: boolean;
  // Recebe os campos da linha (+ qnt) e os parâmetros globais; devolve os resultados da linha
  calcular: (c: Record<string, number>, p: Record<string, number>) => Record<string, number>;
}

interface Linha {
  id: string;
  nome: string;
  qnt: number;
  valores: Record<string, number>;
}

const novaLinha = (config: EstruturaConfig): Linha => ({
  id: crypto.randomUUID(),
  nome: '',
  qnt: 1,
  valores: Object.fromEntries(config.campos.map((c) => [c.key, 0])),
});

interface Props {
  config: EstruturaConfig;
  icon: React.ReactNode;
}

// Motor genérico das calculadoras estruturais: linhas de elementos com
// dimensões + parâmetros globais + seção opcional de aço, com totais no resumo.
export const EstruturaCalculator: React.FC<Props> = ({ config, icon }) => {
  const [linhas, setLinhas] = useState<Linha[]>([novaLinha(config)]);
  const [params, setParams] = useState<Record<string, number>>(
    Object.fromEntries((config.parametros ?? []).map((p) => [p.key, p.valorInicial]))
  );
  const [aco, setAco] = useState<AcoItem[]>([]);

  const updateLinha = (id: string, patch: Partial<Linha>) =>
    setLinhas(linhas.map((l) => (l.id === id ? { ...l, ...patch } : l)));

  const updateValor = (id: string, key: string, v: number) =>
    setLinhas(linhas.map((l) => (l.id === id ? { ...l, valores: { ...l.valores, [key]: v } } : l)));

  const totais = useMemo(() => {
    const acc: Record<string, number> = Object.fromEntries(config.resultados.map((r) => [r.key, 0]));
    for (const l of linhas) {
      const res = config.calcular({ ...l.valores, qnt: l.qnt }, params);
      for (const r of config.resultados) acc[r.key] += res[r.key] ?? 0;
    }
    return acc;
  }, [linhas, params, config]);

  const acoTotais = useMemo(() => calcAcoTotais(aco), [aco]);

  const colunas = `2fr 0.7fr ${config.campos.map(() => '1fr').join(' ')} 40px`;

  return (
    <div className="animate-fade-in" style={{ padding: '2rem', maxWidth: '1300px', margin: '0 auto' }}>
      <PageHeader icon={icon} titulo={config.titulo} subtitulo={config.subtitulo} />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 350px', gap: '2rem', alignItems: 'start' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
          {config.parametros && config.parametros.length > 0 && (
            <div className="glass-panel" style={panelStyle}>
              <h2 style={{ fontSize: '1.2rem', marginBottom: '1rem' }}>Parâmetros</h2>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem' }}>
                {config.parametros.map((p) => (
                  <NumField
                    key={p.key}
                    label={p.label}
                    value={params[p.key]}
                    onChange={(v) => setParams({ ...params, [p.key]: v })}
                  />
                ))}
              </div>
            </div>
          )}

          <div className="glass-panel" style={panelStyle}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <h2 style={{ fontSize: '1.2rem' }}>{config.nomeLinha}s</h2>
              <button
                onClick={() => setLinhas([...linhas, novaLinha(config)])}
                className="btn-secondary"
                style={{ padding: '0.4rem 0.8rem', fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '0.3rem' }}
              >
                <Plus size={14} /> Adicionar
              </button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', overflowX: 'auto' }}>
              {linhas.map((linha) => (
                <div key={linha.id} style={{ display: 'grid', gridTemplateColumns: colunas, gap: '0.8rem', alignItems: 'end', minWidth: '600px' }}>
                  <TextField
                    label={`${config.nomeLinha} (nome)`}
                    value={linha.nome}
                    placeholder={config.placeholderNome}
                    onChange={(v) => updateLinha(linha.id, { nome: v })}
                  />
                  <NumField label="Qnt." value={linha.qnt} step={1} onChange={(v) => updateLinha(linha.id, { qnt: v })} />
                  {config.campos.map((c) => (
                    <NumField
                      key={c.key}
                      label={c.label}
                      value={linha.valores[c.key]}
                      onChange={(v) => updateValor(linha.id, c.key, v)}
                    />
                  ))}
                  <button
                    onClick={() => setLinhas(linhas.filter((l) => l.id !== linha.id))}
                    disabled={linhas.length === 1}
                    style={{
                      background: 'transparent', border: 'none', padding: '0.6rem',
                      color: linhas.length === 1 ? 'var(--border-color)' : 'var(--danger)',
                      cursor: linhas.length === 1 ? 'not-allowed' : 'pointer',
                    }}
                  >
                    <Trash2 size={18} />
                  </button>
                </div>
              ))}
            </div>
          </div>

          {config.comAco && <AcoSection itens={aco} setItens={setAco} />}
        </div>

        <div style={{ position: 'sticky', top: '2rem' }}>
          <div className="glass-panel" style={resumoPanelStyle}>
            <h2 style={{ fontSize: '1.2rem', marginBottom: '1.5rem', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '0.8rem' }}>
              Resumo do Cálculo
            </h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {config.resultados.map((r) => (
                <ResultRow
                  key={r.key}
                  label={r.label}
                  valor={totais[r.key]}
                  unidade={r.unidade}
                  cor={r.cor}
                  destaque={r.key === config.resultadoDestaque}
                />
              ))}
              {config.comAco && (
                <>
                  <div style={{ borderTop: '1px solid rgba(255,255,255,0.1)', paddingTop: '1rem', marginTop: '0.5rem' }}>
                    <span style={{ fontSize: '0.8rem', textTransform: 'uppercase', color: 'var(--text-muted)', letterSpacing: '0.05em' }}>Aço</span>
                  </div>
                  <ResultRow label="CA-50 / CA-25" valor={acoTotais.ca50} unidade="kg" />
                  <ResultRow label="CA-60" valor={acoTotais.ca60} unidade="kg" />
                  <ResultRow label="Tela soldada" valor={acoTotais.tela} unidade="kg" />
                  <ResultRow label="Aço total" valor={acoTotais.total} unidade="kg" destaque cor="var(--accent-primary)" />
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
