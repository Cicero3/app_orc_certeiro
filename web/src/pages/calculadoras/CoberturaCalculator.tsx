import React, { useMemo, useState } from 'react';
import { Home, Plus, Trash2 } from 'lucide-react';
import { NumField, PageHeader, ResultRow, TextField, panelStyle, resumoPanelStyle } from './shared';

interface Agua {
  id: string;
  nome: string;
  area: number;
  calha: number;
  rufo: number;
  cumeeira: number;
}

interface Especificacoes {
  estrutura: string;
  subCobertura: string;
  telhas: string;
  calhas: string;
  rufos: string;
  cumeeiras: string;
  sobreCobertura: string;
  impermeabilizacoes: string;
}

const ESPEC_LABELS: Record<keyof Especificacoes, string> = {
  estrutura: 'Estrutura da cobertura',
  subCobertura: 'Sub-cobertura',
  telhas: 'Telhas',
  calhas: 'Calhas',
  rufos: 'Rufos',
  cumeeiras: 'Cumeeiras',
  sobreCobertura: 'Sobre-cobertura',
  impermeabilizacoes: 'Impermeabilizações',
};

const novaAgua = (): Agua => ({
  id: crypto.randomUUID(),
  nome: '',
  area: 0,
  calha: 0,
  rufo: 0,
  cumeeira: 0,
});

export const CoberturaCalculator: React.FC = () => {
  const [aguas, setAguas] = useState<Agua[]>([novaAgua()]);
  const [especs, setEspecs] = useState<Especificacoes>({
    estrutura: '', subCobertura: '', telhas: '', calhas: '',
    rufos: '', cumeeiras: '', sobreCobertura: '', impermeabilizacoes: '',
  });

  const update = (id: string, patch: Partial<Agua>) =>
    setAguas(aguas.map((a) => (a.id === id ? { ...a, ...patch } : a)));

  const totais = useMemo(() => ({
    area: aguas.reduce((acc, a) => acc + a.area, 0),
    calha: aguas.reduce((acc, a) => acc + a.calha, 0),
    rufo: aguas.reduce((acc, a) => acc + a.rufo, 0),
    cumeeira: aguas.reduce((acc, a) => acc + a.cumeeira, 0),
  }), [aguas]);

  return (
    <div className="animate-fade-in" style={{ padding: '2rem', maxWidth: '1300px', margin: '0 auto' }}>
      <PageHeader
        icon={<Home color="var(--accent-primary)" />}
        titulo="Cobertura"
        subtitulo="Levantamento de coberturas: áreas, calhas, rufos, cumeeiras e especificações."
      />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 350px', gap: '2rem', alignItems: 'start' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
          <div className="glass-panel" style={panelStyle}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <h2 style={{ fontSize: '1.2rem' }}>Águas / Panos de Telhado</h2>
              <button
                onClick={() => setAguas([...aguas, novaAgua()])}
                className="btn-secondary"
                style={{ padding: '0.4rem 0.8rem', fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '0.3rem' }}
              >
                <Plus size={14} /> Adicionar
              </button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {aguas.map((a) => (
                <div key={a.id} style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr 1fr 1fr 40px', gap: '0.8rem', alignItems: 'end' }}>
                  <TextField label="Nome" value={a.nome} placeholder="Água 1" onChange={(v) => update(a.id, { nome: v })} />
                  <NumField label="Área (m²)" value={a.area} onChange={(v) => update(a.id, { area: v })} />
                  <NumField label="Calha (m)" value={a.calha} onChange={(v) => update(a.id, { calha: v })} />
                  <NumField label="Rufo (m)" value={a.rufo} onChange={(v) => update(a.id, { rufo: v })} />
                  <NumField label="Cumeeira (m)" value={a.cumeeira} onChange={(v) => update(a.id, { cumeeira: v })} />
                  <button
                    onClick={() => setAguas(aguas.filter((x) => x.id !== a.id))}
                    disabled={aguas.length === 1}
                    style={{
                      background: 'transparent', border: 'none', padding: '0.6rem',
                      color: aguas.length === 1 ? 'var(--border-color)' : 'var(--danger)',
                      cursor: aguas.length === 1 ? 'not-allowed' : 'pointer',
                    }}
                  >
                    <Trash2 size={18} />
                  </button>
                </div>
              ))}
            </div>
          </div>

          <div className="glass-panel" style={panelStyle}>
            <h2 style={{ fontSize: '1.2rem', marginBottom: '1.5rem' }}>Tipos e Especificações</h2>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1rem' }}>
              {(Object.keys(ESPEC_LABELS) as (keyof Especificacoes)[]).map((k) => (
                <TextField
                  key={k}
                  label={ESPEC_LABELS[k]}
                  value={especs[k]}
                  placeholder="Ex.: madeira, cerâmica, alumínio..."
                  onChange={(v) => setEspecs({ ...especs, [k]: v })}
                />
              ))}
            </div>
          </div>
        </div>

        <div style={{ position: 'sticky', top: '2rem' }}>
          <div className="glass-panel" style={resumoPanelStyle}>
            <h2 style={{ fontSize: '1.2rem', marginBottom: '1.5rem', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '0.8rem' }}>
              Totais da Cobertura
            </h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <ResultRow label="Calhas" valor={totais.calha} unidade="m" />
              <ResultRow label="Rufos" valor={totais.rufo} unidade="m" />
              <ResultRow label="Cumeeiras" valor={totais.cumeeira} unidade="m" />
              <ResultRow label="Área total" valor={totais.area} unidade="m²" destaque />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
