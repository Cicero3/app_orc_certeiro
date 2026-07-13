import React, { useMemo, useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { NumField, PageHeader, SelectField, TextField, fmt, panelStyle, resumoPanelStyle } from './shared';

export interface GrupoMaterial {
  nome: string;
  unidade: 'm' | 'un';
  itens: string[];
}

export interface InstalacaoConfig {
  titulo: string;
  subtitulo: string;
  localLabel: string; // ex.: "Local / Circuito", "Prumada", "Descrição"
  grupos: GrupoMaterial[];
}

interface Lancamento {
  id: string;
  local: string;
  grupo: string;
  item: string;
  quantidade: number;
}

interface Props {
  config: InstalacaoConfig;
  icon: React.ReactNode;
}

// Motor genérico de levantamento de instalações: lançamentos de material por
// local/circuito, com totais agregados por item no painel de resumo.
export const InstalacoesCalculator: React.FC<Props> = ({ config, icon }) => {
  const novoLancamento = (): Lancamento => ({
    id: crypto.randomUUID(),
    local: '',
    grupo: config.grupos[0].nome,
    item: config.grupos[0].itens[0],
    quantidade: 0,
  });

  const [lancamentos, setLancamentos] = useState<Lancamento[]>([novoLancamento()]);

  const grupoDe = (nome: string) => config.grupos.find((g) => g.nome === nome) ?? config.grupos[0];

  const update = (id: string, patch: Partial<Lancamento>) =>
    setLancamentos(lancamentos.map((l) => {
      if (l.id !== id) return l;
      const novo = { ...l, ...patch };
      // Ao trocar de grupo, seleciona o primeiro item do novo grupo
      if (patch.grupo && !grupoDe(novo.grupo).itens.includes(novo.item)) {
        novo.item = grupoDe(novo.grupo).itens[0];
      }
      return novo;
    }));

  // Totais agregados por grupo -> item
  const totais = useMemo(() => {
    const porGrupo = new Map<string, Map<string, number>>();
    for (const l of lancamentos) {
      if (l.quantidade <= 0) continue;
      if (!porGrupo.has(l.grupo)) porGrupo.set(l.grupo, new Map());
      const itens = porGrupo.get(l.grupo)!;
      itens.set(l.item, (itens.get(l.item) ?? 0) + l.quantidade);
    }
    return porGrupo;
  }, [lancamentos]);

  return (
    <div className="animate-fade-in" style={{ padding: '2rem', maxWidth: '1300px', margin: '0 auto' }}>
      <PageHeader icon={icon} titulo={config.titulo} subtitulo={config.subtitulo} />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 380px', gap: '2rem', alignItems: 'start' }}>
        <div className="glass-panel" style={panelStyle}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
            <h2 style={{ fontSize: '1.2rem' }}>Lançamentos</h2>
            <button
              onClick={() => setLancamentos([...lancamentos, novoLancamento()])}
              className="btn-secondary"
              style={{ padding: '0.4rem 0.8rem', fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '0.3rem' }}
            >
              <Plus size={14} /> Adicionar
            </button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {lancamentos.map((l) => {
              const grupo = grupoDe(l.grupo);
              return (
                <div key={l.id} style={{ display: 'grid', gridTemplateColumns: '1.2fr 1.2fr 2fr 0.8fr 40px', gap: '0.8rem', alignItems: 'end' }}>
                  <TextField label={config.localLabel} value={l.local} onChange={(v) => update(l.id, { local: v })} />
                  <SelectField
                    label="Categoria"
                    value={l.grupo}
                    onChange={(v) => update(l.id, { grupo: v })}
                    options={config.grupos.map((g) => g.nome)}
                  />
                  <SelectField label="Material" value={l.item} onChange={(v) => update(l.id, { item: v })} options={grupo.itens} />
                  <NumField
                    label={grupo.unidade === 'm' ? 'Metros' : 'Unid.'}
                    value={l.quantidade}
                    step={grupo.unidade === 'm' ? 0.1 : 1}
                    onChange={(v) => update(l.id, { quantidade: v })}
                  />
                  <button
                    onClick={() => setLancamentos(lancamentos.filter((x) => x.id !== l.id))}
                    disabled={lancamentos.length === 1}
                    style={{
                      background: 'transparent', border: 'none', padding: '0.6rem',
                      color: lancamentos.length === 1 ? 'var(--border-color)' : 'var(--danger)',
                      cursor: lancamentos.length === 1 ? 'not-allowed' : 'pointer',
                    }}
                  >
                    <Trash2 size={18} />
                  </button>
                </div>
              );
            })}
          </div>
        </div>

        <div style={{ position: 'sticky', top: '2rem' }}>
          <div className="glass-panel" style={resumoPanelStyle}>
            <h2 style={{ fontSize: '1.2rem', marginBottom: '1.5rem', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '0.8rem' }}>
              Lista de Materiais
            </h2>
            {totais.size === 0 && (
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', fontStyle: 'italic' }}>
                Informe quantidades para gerar a lista de materiais.
              </p>
            )}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.2rem', maxHeight: '70vh', overflowY: 'auto' }}>
              {config.grupos.filter((g) => totais.has(g.nome)).map((g) => (
                <div key={g.nome}>
                  <span style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-muted)', letterSpacing: '0.05em' }}>
                    {g.nome}
                  </span>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginTop: '0.5rem' }}>
                    {[...totais.get(g.nome)!.entries()].map(([item, qtd]) => (
                      <div key={item} style={{ display: 'flex', justifyContent: 'space-between', gap: '1rem', fontSize: '0.9rem' }}>
                        <span style={{ color: 'var(--text-secondary)' }}>{item}</span>
                        <span style={{ color: 'var(--success)', fontWeight: 600, whiteSpace: 'nowrap' }}>
                          {fmt(qtd, g.unidade === 'm' ? 2 : 0)} {g.unidade}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
