import React, { useMemo, useState } from 'react';
import { DoorOpen, Plus, Trash2 } from 'lucide-react';
import { NumField, PageHeader, ResultRow, SelectField, TextField, fmt, panelStyle, resumoPanelStyle } from './shared';

interface Esquadria {
  id: string;
  nome: string;
  tipo: string;
  material: string;
  quant: number;
  largura: number;
  altura: number;
  pintura: boolean;
  vidro: boolean;
  reducaoVidro: number; // % da área descontada de caixilhos
  soleira: boolean;
  peitoril: boolean;
}

const TIPOS = ['Porta', 'Janela', 'Portão', 'Basculante', 'Veneziana', 'Vitrô', 'Outro'];
const MATERIAIS = ['Madeira', 'Alumínio', 'Ferro', 'PVC', 'Vidro temperado', 'Outro'];

const nova = (): Esquadria => ({
  id: crypto.randomUUID(),
  nome: '',
  tipo: 'Porta',
  material: 'Madeira',
  quant: 1,
  largura: 0,
  altura: 0,
  pintura: false,
  vidro: false,
  reducaoVidro: 0,
  soleira: false,
  peitoril: false,
});

const checkLabel: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: '0.4rem',
  fontSize: '0.85rem',
  color: 'var(--text-secondary)',
  cursor: 'pointer',
  whiteSpace: 'nowrap',
};

export const EsquadriasCalculator: React.FC = () => {
  const [itens, setItens] = useState<Esquadria[]>([nova()]);

  const update = (id: string, patch: Partial<Esquadria>) =>
    setItens(itens.map((e) => (e.id === id ? { ...e, ...patch } : e)));

  const areaItem = (e: Esquadria) => e.quant * e.largura * e.altura;

  const totais = useMemo(() => {
    let area = 0, pintura = 0, vidro = 0, soleira = 0, peitoril = 0;
    for (const e of itens) {
      const a = areaItem(e);
      area += a;
      // Pintura nas duas faces da esquadria
      if (e.pintura) pintura += a * 2;
      if (e.vidro) vidro += a * (1 - e.reducaoVidro / 100);
      if (e.soleira) soleira += e.quant * e.largura;
      if (e.peitoril) peitoril += e.quant * e.largura;
    }
    return { area, pintura, vidro, soleira, peitoril };
  }, [itens]);

  return (
    <div className="animate-fade-in" style={{ padding: '2rem', maxWidth: '1300px', margin: '0 auto' }}>
      <PageHeader
        icon={<DoorOpen color="var(--accent-primary)" />}
        titulo="Esquadrias"
        subtitulo="Portas, janelas e portões: áreas, pintura, vidros, soleiras e peitoris."
      />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 350px', gap: '2rem', alignItems: 'start' }}>
        <div className="glass-panel" style={panelStyle}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
            <h2 style={{ fontSize: '1.2rem' }}>Esquadrias</h2>
            <button
              onClick={() => setItens([...itens, nova()])}
              className="btn-secondary"
              style={{ padding: '0.4rem 0.8rem', fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '0.3rem' }}
            >
              <Plus size={14} /> Adicionar
            </button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            {itens.map((e) => (
              <div key={e.id} style={{ borderBottom: '1px solid var(--border-color)', paddingBottom: '1.2rem' }}>
                <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr 1fr 0.7fr 0.9fr 0.9fr 40px', gap: '0.8rem', alignItems: 'end' }}>
                  <TextField label="Nome" value={e.nome} placeholder="P1, J1..." onChange={(v) => update(e.id, { nome: v })} />
                  <SelectField label="Tipo" value={e.tipo} onChange={(v) => update(e.id, { tipo: v })} options={TIPOS} />
                  <SelectField label="Material" value={e.material} onChange={(v) => update(e.id, { material: v })} options={MATERIAIS} />
                  <NumField label="Qnt." value={e.quant} step={1} onChange={(v) => update(e.id, { quant: v })} />
                  <NumField label="L (m)" value={e.largura} onChange={(v) => update(e.id, { largura: v })} />
                  <NumField label="H (m)" value={e.altura} onChange={(v) => update(e.id, { altura: v })} />
                  <button
                    onClick={() => setItens(itens.filter((x) => x.id !== e.id))}
                    disabled={itens.length === 1}
                    style={{
                      background: 'transparent', border: 'none', padding: '0.6rem',
                      color: itens.length === 1 ? 'var(--border-color)' : 'var(--danger)',
                      cursor: itens.length === 1 ? 'not-allowed' : 'pointer',
                    }}
                  >
                    <Trash2 size={18} />
                  </button>
                </div>

                <div style={{ display: 'flex', gap: '1.5rem', marginTop: '0.8rem', alignItems: 'center', flexWrap: 'wrap' }}>
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                    Área: <strong style={{ color: 'var(--success)' }}>{fmt(areaItem(e))} m²</strong>
                  </span>
                  <label style={checkLabel}>
                    <input type="checkbox" checked={e.pintura} onChange={(ev) => update(e.id, { pintura: ev.target.checked })} />
                    Pintura
                  </label>
                  <label style={checkLabel}>
                    <input type="checkbox" checked={e.vidro} onChange={(ev) => update(e.id, { vidro: ev.target.checked })} />
                    Vidro
                  </label>
                  {e.vidro && (
                    <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                      Redução caixilho (%):
                      <input
                        type="number"
                        min={0}
                        max={100}
                        value={e.reducaoVidro || ''}
                        onChange={(ev) => update(e.id, { reducaoVidro: parseFloat(ev.target.value) || 0 })}
                        style={{
                          width: '70px', padding: '0.3rem', borderRadius: '4px',
                          border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', color: 'var(--text-primary)',
                        }}
                      />
                    </span>
                  )}
                  <label style={checkLabel}>
                    <input type="checkbox" checked={e.soleira} onChange={(ev) => update(e.id, { soleira: ev.target.checked })} />
                    Soleira
                  </label>
                  <label style={checkLabel}>
                    <input type="checkbox" checked={e.peitoril} onChange={(ev) => update(e.id, { peitoril: ev.target.checked })} />
                    Peitoril
                  </label>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div style={{ position: 'sticky', top: '2rem' }}>
          <div className="glass-panel" style={resumoPanelStyle}>
            <h2 style={{ fontSize: '1.2rem', marginBottom: '1.5rem', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '0.8rem' }}>
              Totais de Esquadrias
            </h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <ResultRow label="Pintura" valor={totais.pintura} unidade="m²" />
              <ResultRow label="Vidros" valor={totais.vidro} unidade="m²" />
              <ResultRow label="Soleiras" valor={totais.soleira} unidade="m" />
              <ResultRow label="Peitoris" valor={totais.peitoril} unidade="m" />
              <ResultRow label="Área total" valor={totais.area} unidade="m²" destaque />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
