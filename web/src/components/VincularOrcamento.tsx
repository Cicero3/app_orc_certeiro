import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { X, Loader2, Link2 } from 'lucide-react';

/**
 * Botão + modal reutilizável para vincular o resultado de qualquer calculadora
 * a um item da EAP de um orçamento (o resultado vira a quantidade do item e os
 * inputs ficam salvos como memória de cálculo).
 */

interface OrcamentoResumo { id: string; titulo: string; status: string }

interface ItemPlano { id: string; rotulo: string }

interface Props {
  tipo: string;          // ex: 'ALVENARIA'
  descricao: string;     // ex: 'Levantamento de alvenaria — área líquida'
  unidade: string;       // ex: 'M2'
  resultado: number;     // valor calculado
  payload: unknown;      // inputs da calculadora (serializados como memória de cálculo)
}

const API = 'http://localhost:8080/api/v1';

const inputStyle: React.CSSProperties = {
  width: '100%', padding: '0.55rem 0.7rem', borderRadius: '4px',
  border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', color: 'var(--text-primary)',
};

export const VincularOrcamentoButton: React.FC<Props> = ({ tipo, descricao, unidade, resultado, payload }) => {
  const { token } = useAuth();
  const [aberto, setAberto] = useState(false);
  const [orcamentos, setOrcamentos] = useState<OrcamentoResumo[]>([]);
  const [orcamentoId, setOrcamentoId] = useState('');
  const [itens, setItens] = useState<ItemPlano[]>([]);
  const [eapItemId, setEapItemId] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [msg, setMsg] = useState<{ tipo: 'ok' | 'erro'; texto: string } | null>(null);

  const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };

  useEffect(() => {
    if (!aberto) return;
    (async () => {
      setIsLoading(true);
      try {
        const response = await fetch(`${API}/orcamentos`, { headers });
        const result = await response.json();
        setOrcamentos((result.data ?? []).filter((o: OrcamentoResumo) => o.status === 'RASCUNHO' || o.status === 'EM_REVISAO'));
      } finally {
        setIsLoading(false);
      }
    })();
  }, [aberto]);

  useEffect(() => {
    if (!orcamentoId) { setItens([]); return; }
    (async () => {
      setIsLoading(true);
      try {
        const response = await fetch(`${API}/orcamentos/${orcamentoId}`, { headers });
        const result = await response.json();
        const planos: ItemPlano[] = [];
        const coletar = (item: any, prefixo: string) => {
          planos.push({ id: item.id, rotulo: `${prefixo}${item.codigoItem} · ${item.descricao}` });
          (item.subItens ?? []).forEach((s: any) => coletar(s, `${prefixo}— `));
        };
        (result.data.modulos ?? []).forEach((m: any) => (m.itens ?? []).forEach((i: any) => coletar(i, `[${m.nome}] `)));
        setItens(planos);
      } finally {
        setIsLoading(false);
      }
    })();
  }, [orcamentoId]);

  const salvar = async () => {
    setIsLoading(true);
    setMsg(null);
    try {
      const response = await fetch(`${API}/orcamentos/${orcamentoId}/levantamentos`, {
        method: 'POST',
        headers,
        body: JSON.stringify({
          tipo,
          descricao,
          unidade,
          resultado,
          payload: JSON.stringify(payload),
          eapItemId: eapItemId || null,
        }),
      });
      if (!response.ok) {
        const errBody = await response.json().catch(() => null);
        throw new Error(errBody?.error?.message || 'Falha ao salvar levantamento');
      }
      setMsg({
        tipo: 'ok',
        texto: eapItemId
          ? 'Levantamento salvo e quantidade do item atualizada!'
          : 'Levantamento salvo no orçamento (sem vincular a item).',
      });
    } catch (err: any) {
      setMsg({ tipo: 'erro', texto: err.message });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      <button className="btn-primary" style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }} onClick={() => { setAberto(true); setMsg(null); }}>
        <Link2 size={16} /> Vincular ao Orçamento
      </button>

      {aberto && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000, backdropFilter: 'blur(4px)' }}>
          <div className="glass-panel animate-fade-in" style={{ width: '100%', maxWidth: '520px', padding: '2rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
              <h2 style={{ fontSize: '1.25rem' }}>Vincular ao Orçamento</h2>
              <button onClick={() => setAberto(false)} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}><X size={22} /></button>
            </div>

            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1.2rem' }}>
              Resultado: <strong style={{ color: 'var(--success)' }}>{resultado.toLocaleString('pt-BR', { maximumFractionDigits: 2 })} {unidade}</strong> — {descricao}
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', marginBottom: '0.4rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Orçamento (rascunho/em revisão)</label>
                <select style={inputStyle} value={orcamentoId} onChange={(e) => { setOrcamentoId(e.target.value); setEapItemId(''); }}>
                  <option value="">— selecione —</option>
                  {orcamentos.map((o) => <option key={o.id} value={o.id}>{o.titulo}</option>)}
                </select>
              </div>

              {orcamentoId && (
                <div>
                  <label style={{ display: 'block', marginBottom: '0.4rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                    Item da EAP que receberá a quantidade (opcional)
                  </label>
                  <select style={inputStyle} value={eapItemId} onChange={(e) => setEapItemId(e.target.value)}>
                    <option value="">— apenas salvar a memória de cálculo —</option>
                    {itens.map((i) => <option key={i.id} value={i.id}>{i.rotulo}</option>)}
                  </select>
                </div>
              )}

              {msg && (
                <div style={{ padding: '0.8rem', borderRadius: '6px', fontSize: '0.9rem', background: msg.tipo === 'ok' ? 'rgba(34,197,94,0.1)' : 'rgba(239,68,68,0.1)', border: `1px solid ${msg.tipo === 'ok' ? 'var(--success)' : 'var(--danger)'}`, color: msg.tipo === 'ok' ? 'var(--success)' : 'var(--danger)' }}>
                  {msg.texto}
                </div>
              )}

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.8rem' }}>
                <button className="btn-secondary" onClick={() => setAberto(false)}>Fechar</button>
                <button className="btn-primary" disabled={!orcamentoId || isLoading} style={{ opacity: !orcamentoId || isLoading ? 0.5 : 1, display: 'flex', alignItems: 'center', gap: '0.4rem' }} onClick={salvar}>
                  {isLoading ? <Loader2 size={16} className="animate-spin" /> : <Link2 size={16} />} Salvar Levantamento
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
