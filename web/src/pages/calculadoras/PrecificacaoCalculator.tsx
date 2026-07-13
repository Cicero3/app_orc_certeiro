import React, { useMemo, useState } from 'react';
import { CircleDollarSign, Plus, Trash2 } from 'lucide-react';
import { NumField, PageHeader, TextField, fmt, panelStyle, resumoPanelStyle } from './shared';

// Módulo de precificação baseado no Guia Definitivo do Orçamento de Obras
// (Engplay): custos diretos + indiretos, contingência para imprevistos e
// margem de lucro, com o BDI resultante.

interface Custo {
  id: string;
  descricao: string;
  valor: number;
}

const novoCusto = (descricao = ''): Custo => ({ id: crypto.randomUUID(), descricao, valor: 0 });

const moeda = (v: number) => `R$ ${fmt(v)}`;

interface ListaCustosProps {
  titulo: string;
  hint: string;
  custos: Custo[];
  setCustos: (c: Custo[]) => void;
}

const ListaCustos: React.FC<ListaCustosProps> = ({ titulo, hint, custos, setCustos }) => {
  const update = (id: string, patch: Partial<Custo>) =>
    setCustos(custos.map((c) => (c.id === id ? { ...c, ...patch } : c)));

  return (
    <div className="glass-panel" style={panelStyle}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
        <h2 style={{ fontSize: '1.2rem' }}>{titulo}</h2>
        <button
          onClick={() => setCustos([...custos, novoCusto()])}
          className="btn-secondary"
          style={{ padding: '0.4rem 0.8rem', fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '0.3rem' }}
        >
          <Plus size={14} /> Adicionar
        </button>
      </div>
      <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '1rem', fontStyle: 'italic' }}>{hint}</p>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        {custos.map((c) => (
          <div key={c.id} style={{ display: 'grid', gridTemplateColumns: '2.5fr 1fr 40px', gap: '0.8rem', alignItems: 'end' }}>
            <TextField label="Descrição" value={c.descricao} onChange={(v) => update(c.id, { descricao: v })} />
            <NumField label="Valor (R$)" value={c.valor} onChange={(v) => update(c.id, { valor: v })} />
            <button
              onClick={() => setCustos(custos.filter((x) => x.id !== c.id))}
              disabled={custos.length === 1}
              style={{
                background: 'transparent', border: 'none', padding: '0.6rem',
                color: custos.length === 1 ? 'var(--border-color)' : 'var(--danger)',
                cursor: custos.length === 1 ? 'not-allowed' : 'pointer',
              }}
            >
              <Trash2 size={18} />
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};

export const PrecificacaoCalculator: React.FC = () => {
  const [diretos, setDiretos] = useState<Custo[]>([
    novoCusto('Materiais'),
    novoCusto('Mão de obra'),
    novoCusto('Equipamentos'),
  ]);
  const [indiretos, setIndiretos] = useState<Custo[]>([
    novoCusto('Despesas administrativas'),
    novoCusto('Seguros e taxas'),
    novoCusto('Transporte e armazenagem'),
  ]);
  const [contingencia, setContingencia] = useState(5);
  const [margem, setMargem] = useState(20);
  const [impostos, setImpostos] = useState(0);

  const calc = useMemo(() => {
    const custoDireto = diretos.reduce((acc, c) => acc + c.valor, 0);
    const custoIndireto = indiretos.reduce((acc, c) => acc + c.valor, 0);
    const custoBase = custoDireto + custoIndireto;
    const reservaContingencia = custoBase * (contingencia / 100);
    const custoTotal = custoBase + reservaContingencia;
    const lucro = custoTotal * (margem / 100);
    const subTotal = custoTotal + lucro;
    // Impostos sobre o preço de venda: preço = subtotal / (1 - imposto%)
    const precoVenda = impostos > 0 && impostos < 100 ? subTotal / (1 - impostos / 100) : subTotal;
    const valorImpostos = precoVenda - subTotal;
    const bdi = custoDireto > 0 ? (precoVenda / custoDireto - 1) * 100 : 0;
    return { custoDireto, custoIndireto, custoBase, reservaContingencia, custoTotal, lucro, valorImpostos, precoVenda, bdi };
  }, [diretos, indiretos, contingencia, margem, impostos]);

  const linha = (label: string, valor: string, cor?: string): React.ReactNode => (
    <div style={{ display: 'flex', justifyContent: 'space-between', color: cor ?? 'var(--text-secondary)' }}>
      <span>{label}</span>
      <span>{valor}</span>
    </div>
  );

  return (
    <div className="animate-fade-in" style={{ padding: '2rem', maxWidth: '1300px', margin: '0 auto' }}>
      <PageHeader
        icon={<CircleDollarSign color="var(--accent-primary)" />}
        titulo="Precificação e Margem de Lucro"
        subtitulo="Custos diretos e indiretos, contingência, margem e impostos — com o BDI resultante."
      />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 380px', gap: '2rem', alignItems: 'start' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
          <ListaCustos
            titulo="Custos Diretos"
            hint="Ligados diretamente à execução: materiais, mão de obra e equipamentos."
            custos={diretos}
            setCustos={setDiretos}
          />
          <ListaCustos
            titulo="Custos Indiretos"
            hint="Despesas administrativas, seguros, variações cambiais, transporte, canteiro..."
            custos={indiretos}
            setCustos={setIndiretos}
          />

          <div className="glass-panel" style={panelStyle}>
            <h2 style={{ fontSize: '1.2rem', marginBottom: '1rem' }}>Estratégia de Precificação</h2>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem' }}>
              <NumField label="Contingência p/ imprevistos (%)" value={contingencia} step={0.5} onChange={setContingencia} />
              <NumField label="Margem de lucro (%)" value={margem} step={0.5} onChange={setMargem} />
              <NumField label="Impostos s/ preço de venda (%)" value={impostos} step={0.5} onChange={setImpostos} />
            </div>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '1rem', fontStyle: 'italic' }}>
              💡 Obras de maior complexidade e risco justificam margens maiores. Inclua sempre uma
              reserva de contingência e não comprometa a margem em negociações — prefira oferecer
              benefícios adicionais a descontos.
            </p>
          </div>
        </div>

        <div style={{ position: 'sticky', top: '2rem' }}>
          <div className="glass-panel" style={resumoPanelStyle}>
            <h2 style={{ fontSize: '1.2rem', marginBottom: '1.5rem', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '0.8rem' }}>
              Formação do Preço
            </h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem' }}>
              {linha('Custos diretos', moeda(calc.custoDireto))}
              {linha('Custos indiretos', moeda(calc.custoIndireto))}
              {linha(`Contingência (${fmt(contingencia, 1)}%)`, moeda(calc.reservaContingencia), 'var(--warning)')}
              {linha('Custo total', moeda(calc.custoTotal), 'var(--text-primary)')}
              {linha(`Lucro (${fmt(margem, 1)}%)`, moeda(calc.lucro), 'var(--success)')}
              {impostos > 0 && linha(`Impostos (${fmt(impostos, 1)}%)`, moeda(calc.valorImpostos), 'var(--danger)')}

              <div style={{ marginTop: '0.5rem', paddingTop: '1rem', borderTop: '1px solid rgba(255,255,255,0.1)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '1.05rem', fontWeight: 500, color: 'white' }}>Preço de venda</span>
                <span style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--success)' }}>{moeda(calc.precoVenda)}</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--accent-primary)', fontSize: '0.95rem' }}>
                <span>BDI resultante</span>
                <span style={{ fontWeight: 600 }}>{fmt(calc.bdi, 1)}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
