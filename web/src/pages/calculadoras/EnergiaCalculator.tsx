import React, { useState, useMemo } from 'react';
import { Zap, Plus, Trash2, Link2, X, Loader2 } from 'lucide-react';
import { NumField, PageHeader, TextField, fmt, inputStyle, panelStyle, resumoPanelStyle, ResultRow } from './shared';
import { useAuth } from '../../context/AuthContext';

// Calculadora de consumo de energia do canteiro (planilhas de referência 003/011):
// energia (kWh) = qtde × potência (kW) × h/dia × dias/mês × meses; custo = kWh × tarifa.
// O total pode ser lançado como custo indireto (Despesas Correntes) de um orçamento.

interface Equipamento {
  id: string;
  descricao: string;
  quantidade: number;
  potenciaKw: number;
  horasDia: number;
  diasMes: number;
  meses: number;
}

// Potências de referência da planilha 003/011
const PRESETS: [string, number][] = [
  ['Betoneira elétrica 400 L', 1.5],
  ['Misturador de argamassa 3,5 m³/h', 2.2],
  ['Vibrador de imersão', 1.5],
  ['Serra circular de bancada', 3.7],
  ['Dobradora de ferro 5 HP', 3.7],
  ['Máquina de solda', 10],
  ['Grua fixa 50m/2t', 110],
  ['Cremalheira', 30],
  ['Central de concreto 60 m³/h', 200],
  ['Escritório (iluminação + tomadas)', 2],
  ['Outro equipamento', 0],
];

const novoEquipamento = (descricao = '', potenciaKw = 0): Equipamento => ({
  id: crypto.randomUUID(), descricao, quantidade: 1, potenciaKw, horasDia: 8, diasMes: 22, meses: 1,
});

const kwhDe = (e: Equipamento) => e.quantidade * e.potenciaKw * e.horasDia * e.diasMes * e.meses;

const API = 'http://localhost:8080/api/v1';

export const EnergiaCalculator: React.FC = () => {
  const { token } = useAuth();
  const [equipamentos, setEquipamentos] = useState<Equipamento[]>([
    novoEquipamento('Betoneira elétrica 400 L', 1.5),
    novoEquipamento('Serra circular de bancada', 3.7),
  ]);
  const [tarifa, setTarifa] = useState(0.95);
  const [modal, setModal] = useState(false);
  const [orcamentos, setOrcamentos] = useState<{ id: string; titulo: string; status: string }[]>([]);
  const [orcamentoId, setOrcamentoId] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [msg, setMsg] = useState<{ tipo: 'ok' | 'erro'; texto: string } | null>(null);

  const update = (id: string, patch: Partial<Equipamento>) =>
    setEquipamentos(equipamentos.map((e) => (e.id === id ? { ...e, ...patch } : e)));

  const totais = useMemo(() => {
    const kwh = equipamentos.reduce((acc, e) => acc + kwhDe(e), 0);
    return { kwh, custo: kwh * tarifa };
  }, [equipamentos, tarifa]);

  const abrirVinculo = async () => {
    setModal(true);
    setMsg(null);
    const response = await fetch(`${API}/orcamentos`, { headers: { Authorization: `Bearer ${token}` } });
    const result = await response.json();
    setOrcamentos((result.data ?? []).filter((o: any) => o.status === 'RASCUNHO' || o.status === 'EM_REVISAO'));
  };

  const lancarCustoIndireto = async () => {
    setIsSaving(true);
    setMsg(null);
    try {
      const response = await fetch(`${API}/orcamentos/${orcamentoId}/custos-indiretos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify({
          categoria: 'DESPESAS_CORRENTES',
          descricao: `Consumo de energia do canteiro (${fmt(totais.kwh, 0)} kWh × R$ ${fmt(tarifa)} /kWh)`,
          quantidade: Math.round(totais.kwh * 100) / 100,
          valorUnitario: tarifa,
        }),
      });
      if (!response.ok) {
        const errBody = await response.json().catch(() => null);
        throw new Error(errBody?.error?.message || 'Falha ao lançar custo indireto');
      }
      setMsg({ tipo: 'ok', texto: 'Lançado como custo indireto (Despesas Correntes) do orçamento!' });
    } catch (err: any) {
      setMsg({ tipo: 'erro', texto: err.message });
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="animate-fade-in" style={{ padding: '2rem', maxWidth: '1300px', margin: '0 auto' }}>
      <PageHeader
        icon={<Zap color="var(--accent-primary)" />}
        titulo="Consumo de Energia do Canteiro"
        subtitulo="kWh = qtde × potência × h/dia × dias/mês × meses. Lance o total como custo indireto do orçamento."
      />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: '2rem', alignItems: 'start' }}>
        <div className="glass-panel" style={panelStyle}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.2rem' }}>
            <h2 style={{ fontSize: '1.2rem' }}>Equipamentos</h2>
            <select
              className="btn-secondary"
              style={{ ...inputStyle, width: 'auto', fontSize: '0.85rem' }}
              value=""
              onChange={(e) => {
                const preset = PRESETS.find(([nome]) => nome === e.target.value);
                if (preset) setEquipamentos([...equipamentos, novoEquipamento(preset[0], preset[1])]);
              }}
            >
              <option value="">+ Adicionar equipamento...</option>
              {PRESETS.map(([nome, kw]) => <option key={nome} value={nome}>{nome}{kw > 0 ? ` (${kw} kW)` : ''}</option>)}
            </select>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {equipamentos.map((e) => (
              <div key={e.id} style={{ display: 'grid', gridTemplateColumns: '2fr 60px 80px 70px 70px 60px 100px 30px', gap: '0.6rem', alignItems: 'end' }}>
                <TextField label="Descrição" value={e.descricao} onChange={(v) => update(e.id, { descricao: v })} />
                <NumField label="Qtde" value={e.quantidade} step={1} onChange={(v) => update(e.id, { quantidade: v })} />
                <NumField label="Potência (kW)" value={e.potenciaKw} onChange={(v) => update(e.id, { potenciaKw: v })} />
                <NumField label="h/dia" value={e.horasDia} step={0.5} onChange={(v) => update(e.id, { horasDia: v })} />
                <NumField label="dias/mês" value={e.diasMes} step={1} onChange={(v) => update(e.id, { diasMes: v })} />
                <NumField label="meses" value={e.meses} step={1} onChange={(v) => update(e.id, { meses: v })} />
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>kWh</label>
                  <div style={{ ...inputStyle, background: 'transparent', color: 'var(--success)', fontWeight: 600, textAlign: 'right' }}>{fmt(kwhDe(e), 0)}</div>
                </div>
                <button onClick={() => setEquipamentos(equipamentos.filter((x) => x.id !== e.id))} style={{ background: 'transparent', border: 'none', color: 'var(--danger)', cursor: 'pointer', paddingBottom: '0.55rem' }}>
                  <Trash2 size={16} />
                </button>
              </div>
            ))}
            {equipamentos.length === 0 && <p style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>Adicione equipamentos do canteiro pela lista acima.</p>}
          </div>
        </div>

        <div style={{ position: 'sticky', top: '2rem' }}>
          <div className="glass-panel" style={resumoPanelStyle}>
            <h2 style={{ fontSize: '1.2rem', marginBottom: '1.5rem', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '0.8rem' }}>Resumo</h2>
            <div style={{ marginBottom: '1.2rem' }}>
              <NumField label="Tarifa de energia (R$/kWh)" value={tarifa} step={0.01} onChange={setTarifa} />
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <ResultRow label="Consumo total" valor={totais.kwh} unidade="kWh" />
              <ResultRow label="Custo de energia" valor={totais.custo} unidade="R$" destaque />
            </div>
            <div style={{ marginTop: '2rem' }}>
              <button className="btn-primary" style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }} onClick={abrirVinculo}>
                <Link2 size={16} /> Lançar como Custo Indireto
              </button>
            </div>
          </div>
        </div>
      </div>

      {modal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000, backdropFilter: 'blur(4px)' }}>
          <div className="glass-panel animate-fade-in" style={{ width: '100%', maxWidth: '480px', padding: '2rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
              <h2 style={{ fontSize: '1.25rem' }}>Lançar Custo Indireto</h2>
              <button onClick={() => setModal(false)} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}><X size={22} /></button>
            </div>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1rem' }}>
              {fmt(totais.kwh, 0)} kWh × R$ {fmt(tarifa)} = <strong style={{ color: 'var(--success)' }}>R$ {fmt(totais.custo)}</strong> em Despesas Correntes.
            </p>
            <select style={inputStyle} value={orcamentoId} onChange={(e) => setOrcamentoId(e.target.value)}>
              <option value="">— selecione o orçamento —</option>
              {orcamentos.map((o) => <option key={o.id} value={o.id}>{o.titulo}</option>)}
            </select>
            {msg && (
              <div style={{ marginTop: '1rem', padding: '0.8rem', borderRadius: '6px', fontSize: '0.9rem', background: msg.tipo === 'ok' ? 'rgba(16,185,129,0.1)' : 'rgba(239,68,68,0.1)', border: `1px solid ${msg.tipo === 'ok' ? 'var(--success)' : 'var(--danger)'}`, color: msg.tipo === 'ok' ? 'var(--success)' : 'var(--danger)' }}>
                {msg.texto}
              </div>
            )}
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.8rem', marginTop: '1.2rem' }}>
              <button className="btn-secondary" onClick={() => setModal(false)}>Fechar</button>
              <button className="btn-primary" disabled={!orcamentoId || isSaving} style={{ opacity: !orcamentoId || isSaving ? 0.5 : 1, display: 'flex', alignItems: 'center', gap: '0.4rem' }} onClick={lancarCustoIndireto}>
                {isSaving ? <Loader2 size={16} className="animate-spin" /> : <Plus size={16} />} Lançar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
