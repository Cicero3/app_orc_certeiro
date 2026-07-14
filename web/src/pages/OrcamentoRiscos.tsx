import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ArrowLeft, Plus, Trash2, Loader2, ShieldAlert, X, Pencil, Dices } from 'lucide-react';

// ---------- Tipos (espelham os DTOs do backend) ----------

interface Risco {
  id: string; descricao: string; categoria: string | null;
  probabilidade: number; impactoMin: number | null; impactoProvavel: number; impactoMax: number | null;
  resposta: string | null; valorEsperado: number;
  probabilidadeScore: number; probabilidadeLabel: string;
  impactoScore: number; impactoLabel: string;
  severidade: number; nivel: string;
}

interface FaixaHistograma { inicio: number; fim: number; contagem: number }

interface Simulacao {
  iteracoes: number; seed: number; media: number; desvioPadrao: number;
  minimo: number; maximo: number; p10: number; p50: number; p80: number; p90: number; p95: number;
  icInferior95: number; icSuperior95: number; histograma: FaixaHistograma[];
}

interface Analise {
  riscos: Risco[];
  custoDireto: number;
  valorEsperadoTotal: number;
  simulacao: Simulacao | null;
  contingenciaAtual: number;
}

const API = 'http://localhost:8080/api/v1';

const fmtMoeda = (v: number) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 }).format(v);
const fmtPct = (v: number) => `${(v * 100).toLocaleString('pt-BR', { maximumFractionDigits: 1 })}%`;
const num = (v: string) => parseFloat(v.replace(',', '.')) || 0;

const inputStyle: React.CSSProperties = {
  width: '100%', padding: '0.5rem 0.7rem', borderRadius: '4px',
  border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', color: 'var(--text-primary)',
};

// Status (reservados): nível de severidade sempre com rótulo, nunca só cor
const NIVEL_COR: Record<string, string> = {
  BAIXO: 'var(--success)',
  MODERADO: 'var(--warning)',
  ALTO: '#f97316',
  CRITICO: 'var(--danger)',
};

const NivelBadge: React.FC<{ nivel: string; severidade: number }> = ({ nivel, severidade }) => (
  <span style={{
    display: 'inline-flex', alignItems: 'center', gap: '0.3rem', padding: '0.2rem 0.55rem',
    borderRadius: '1rem', fontSize: '0.75rem', fontWeight: 600,
    color: NIVEL_COR[nivel] ?? 'var(--text-secondary)',
    border: `1px solid ${NIVEL_COR[nivel] ?? 'var(--border-color)'}`,
    background: 'transparent',
  }}>
    {nivel} · {severidade}
  </span>
);

// ---------- Matriz 5×5 P×I ----------

const IMPACTO_SCORES = [1, 2, 4, 8, 16];
const PROB_LABELS = ['Quase Certa (5)', 'Alta (4)', 'Média (3)', 'Baixa (2)', 'Muito Baixa (1)'];
const IMPACTO_LABELS = ['Muito Baixo', 'Baixo', 'Médio', 'Alto', 'Muito Alto'];

const nivelDaCelula = (severidade: number): string =>
  severidade <= 4 ? 'BAIXO' : severidade <= 12 ? 'MODERADO' : severidade <= 32 ? 'ALTO' : 'CRITICO';

const MatrizRisco: React.FC<{ riscos: Risco[] }> = ({ riscos }) => (
  <div style={{ overflowX: 'auto' }}>
    <table style={{ borderCollapse: 'separate', borderSpacing: '2px', fontSize: '0.8rem', width: '100%' }}>
      <thead>
        <tr>
          <th style={{ textAlign: 'right', padding: '0.3rem 0.5rem', color: 'var(--text-muted)', fontWeight: 500 }}>Prob. \ Impacto</th>
          {IMPACTO_LABELS.map((l, i) => (
            <th key={l} style={{ padding: '0.3rem 0.4rem', color: 'var(--text-muted)', fontWeight: 500, whiteSpace: 'nowrap' }}>{l} ({IMPACTO_SCORES[i]})</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {PROB_LABELS.map((probLabel, linha) => {
          const pScore = 5 - linha;
          return (
            <tr key={probLabel}>
              <td style={{ textAlign: 'right', padding: '0.3rem 0.5rem', color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>{probLabel}</td>
              {IMPACTO_SCORES.map((iScore) => {
                const severidade = pScore * iScore;
                const nivel = nivelDaCelula(severidade);
                const daCelula = riscos.filter((r) => r.probabilidadeScore === pScore && r.impactoScore === iScore);
                return (
                  <td
                    key={iScore}
                    title={daCelula.length > 0 ? daCelula.map((r) => r.descricao).join('\n') : `Severidade ${severidade} — ${nivel}`}
                    style={{
                      textAlign: 'center', padding: '0.55rem 0.4rem', borderRadius: '4px', minWidth: '74px',
                      background: `color-mix(in srgb, ${NIVEL_COR[nivel]} ${daCelula.length > 0 ? 30 : 10}%, var(--bg-tertiary))`,
                      border: daCelula.length > 0 ? `1px solid ${NIVEL_COR[nivel]}` : '1px solid transparent',
                      color: 'var(--text-primary)', fontWeight: daCelula.length > 0 ? 700 : 400,
                    }}
                  >
                    {daCelula.length > 0 ? `${daCelula.length} risco${daCelula.length > 1 ? 's' : ''}` : severidade}
                  </td>
                );
              })}
            </tr>
          );
        })}
      </tbody>
    </table>
  </div>
);

// ---------- Histograma da simulação (série única — cor de acento) ----------

const Histograma: React.FC<{ sim: Simulacao }> = ({ sim }) => {
  const W = 640; const H = 190; const PAD = { top: 10, right: 12, bottom: 34, left: 12 };
  const faixas = sim.histograma;
  const maxContagem = Math.max(...faixas.map((f) => f.contagem), 1);
  const innerW = W - PAD.left - PAD.right;
  const innerH = H - PAD.top - PAD.bottom;
  const larguraBarra = innerW / faixas.length;
  const escalaX = (valor: number) =>
    PAD.left + ((valor - sim.minimo) / Math.max(sim.maximo - sim.minimo, 1e-9)) * innerW;

  const marcadores: [number, string][] = [[sim.p50, 'P50'], [sim.p80, 'P80'], [sim.p95, 'P95']];

  return (
    <svg viewBox={`0 0 ${W} ${H}`} role="img" aria-label="Histograma da contingência simulada" style={{ width: '100%', height: 'auto' }}>
      {faixas.map((f, i) => {
        const h = (f.contagem / maxContagem) * innerH;
        return (
          <rect
            key={i}
            x={PAD.left + i * larguraBarra + 1}
            y={PAD.top + innerH - h}
            width={Math.max(larguraBarra - 2, 1)}
            height={h}
            rx={2}
            fill="var(--accent-primary)"
            opacity={0.85}
          >
            <title>{`${fmtMoeda(f.inicio)} – ${fmtMoeda(f.fim)}: ${f.contagem} cenários (${fmtPct(f.contagem / sim.iteracoes)})`}</title>
          </rect>
        );
      })}
      <line x1={PAD.left} y1={PAD.top + innerH} x2={W - PAD.right} y2={PAD.top + innerH} stroke="var(--border-color)" strokeWidth={1} />
      {marcadores.map(([valor, rotulo]) => (
        <g key={rotulo}>
          <line x1={escalaX(valor)} y1={PAD.top} x2={escalaX(valor)} y2={PAD.top + innerH} stroke="var(--text-muted)" strokeWidth={1} strokeDasharray="3 3" />
          <text x={escalaX(valor)} y={PAD.top + innerH + 14} textAnchor="middle" fontSize={10} fill="var(--text-secondary)">{rotulo}</text>
        </g>
      ))}
      <text x={PAD.left} y={H - 6} fontSize={10} fill="var(--text-muted)">{fmtMoeda(sim.minimo)}</text>
      <text x={W - PAD.right} y={H - 6} textAnchor="end" fontSize={10} fill="var(--text-muted)">{fmtMoeda(sim.maximo)}</text>
    </svg>
  );
};

// ---------- Página ----------

const riscoVazio = { descricao: '', categoria: '', probabilidade: '20', impactoMin: '', impactoProvavel: '', impactoMax: '', resposta: '' };

export const OrcamentoRiscos: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { token } = useAuth();

  const [analise, setAnalise] = useState<Analise | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [okMsg, setOkMsg] = useState('');
  const [iteracoes, setIteracoes] = useState('10000');
  const [percentil, setPercentil] = useState('P80');
  const [modal, setModal] = useState<null | { editando?: Risco }>(null);
  const [form, setForm] = useState(riscoVazio);

  const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };

  const call = useCallback(async (path: string, options: RequestInit = {}) => {
    const response = await fetch(`${API}${path}`, { headers, ...options });
    if (!response.ok) {
      let msg = 'Falha na operação';
      try { msg = (await response.json()).error?.message || msg; } catch { /* sem corpo */ }
      throw new Error(msg);
    }
    return response.status === 204 ? null : response.json();
  }, [token]);

  const carregar = useCallback(async (its = iteracoes) => {
    try {
      const result = await call(`/orcamentos/${id}/riscos/analise?iteracoes=${its}`);
      setAnalise(result.data);
      setError('');
    } catch (err: any) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  }, [id, call, iteracoes]);

  useEffect(() => { carregar(); }, []);

  const executar = async (fn: () => Promise<any>, sucesso = '') => {
    try { await fn(); await carregar(); setOkMsg(sucesso); setError(''); }
    catch (err: any) { setError(err.message); setOkMsg(''); }
  };

  const salvarRisco = () => {
    const payload = {
      descricao: form.descricao.trim(),
      categoria: form.categoria.trim() || null,
      probabilidade: num(form.probabilidade) / 100,
      impactoMin: form.impactoMin ? num(form.impactoMin) : null,
      impactoProvavel: num(form.impactoProvavel),
      impactoMax: form.impactoMax ? num(form.impactoMax) : null,
      resposta: form.resposta.trim() || null,
    };
    executar(async () => {
      if (modal?.editando) await call(`/orcamentos/${id}/riscos/${modal.editando.id}`, { method: 'PUT', body: JSON.stringify(payload) });
      else await call(`/orcamentos/${id}/riscos`, { method: 'POST', body: JSON.stringify(payload) });
      setModal(null);
      setForm(riscoVazio);
    });
  };

  const abrirEdicao = (r: Risco) => {
    setForm({
      descricao: r.descricao, categoria: r.categoria ?? '',
      probabilidade: String(r.probabilidade * 100),
      impactoMin: r.impactoMin != null ? String(r.impactoMin) : '',
      impactoProvavel: String(r.impactoProvavel),
      impactoMax: r.impactoMax != null ? String(r.impactoMax) : '',
      resposta: r.resposta ?? '',
    });
    setModal({ editando: r });
  };

  if (isLoading) {
    return <div style={{ display: 'flex', justifyContent: 'center', padding: '4rem' }}><Loader2 className="animate-spin" size={32} color="var(--accent-primary)" /></div>;
  }

  const sim = analise?.simulacao ?? null;

  return (
    <div className="animate-fade-in" style={{ padding: '2rem', maxWidth: '1300px', margin: '0 auto' }}>
      <button onClick={() => navigate(`/orcamentos/${id}`)} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.4rem', cursor: 'pointer', marginBottom: '1rem', padding: 0, fontSize: '0.9rem' }}>
        <ArrowLeft size={16} /> Voltar à Planilha Orçamentária
      </button>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.5rem' }}>
        <div>
          <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '1.7rem' }}>
            <ShieldAlert color="var(--accent-primary)" /> Riscos & Contingência
          </h1>
          <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem' }}>
            Matriz de risco P×I e contingência por Simulação de Monte Carlo — cada risco dispara com sua probabilidade e o impacto é sorteado da distribuição triangular (mín/provável/máx).
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.8rem', alignItems: 'flex-end' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.82rem' }}>Iterações</label>
            <select style={{ ...inputStyle, width: '120px' }} value={iteracoes} onChange={(e) => { setIteracoes(e.target.value); carregar(e.target.value); }}>
              <option value="1000">1.000</option>
              <option value="10000">10.000</option>
              <option value="50000">50.000</option>
            </select>
          </div>
          <button className="btn-primary" onClick={() => { setForm(riscoVazio); setModal({}); }} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <Plus size={16} /> Novo Risco
          </button>
        </div>
      </div>

      {error && <div style={{ padding: '1rem', backgroundColor: 'rgba(239,68,68,0.1)', border: '1px solid var(--danger)', borderRadius: 'var(--border-radius-md)', color: 'var(--danger)', marginBottom: '1.5rem' }}>{error}</div>}
      {okMsg && <div style={{ padding: '1rem', backgroundColor: 'rgba(16,185,129,0.1)', border: '1px solid var(--success)', borderRadius: 'var(--border-radius-md)', color: 'var(--success)', marginBottom: '1.5rem' }}>{okMsg}</div>}

      {/* Tabela de riscos */}
      <div className="glass-panel" style={{ padding: '1.5rem', marginBottom: '2rem', overflowX: 'auto' }}>
        <h2 style={{ fontSize: '1.1rem', marginBottom: '1rem' }}>Registro de Riscos</h2>
        {analise && analise.riscos.length === 0 ? (
          <p style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>
            Nenhum risco cadastrado. Ex.: "Atraso na entrega dos materiais", probabilidade 32%, impacto provável R$ 159.000.
          </p>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.88rem' }}>
            <thead>
              <tr style={{ color: 'var(--text-muted)', fontSize: '0.75rem', textTransform: 'uppercase' }}>
                <th style={{ textAlign: 'left', padding: '0.4rem 0.6rem' }}>Risco</th>
                <th style={{ textAlign: 'right', padding: '0.4rem 0.6rem' }}>Prob.</th>
                <th style={{ textAlign: 'right', padding: '0.4rem 0.6rem' }}>Impacto (mín/prov/máx)</th>
                <th style={{ textAlign: 'right', padding: '0.4rem 0.6rem' }}>Valor Esperado</th>
                <th style={{ textAlign: 'center', padding: '0.4rem 0.6rem' }}>Severidade</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {analise?.riscos.map((r) => (
                <tr key={r.id} style={{ borderTop: '1px solid var(--border-color)' }}>
                  <td style={{ padding: '0.5rem 0.6rem' }}>
                    {r.descricao}
                    {r.resposta && <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Resposta: {r.resposta}</div>}
                  </td>
                  <td style={{ textAlign: 'right', padding: '0.5rem 0.6rem', whiteSpace: 'nowrap' }}>{fmtPct(r.probabilidade)} <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>({r.probabilidadeLabel})</span></td>
                  <td style={{ textAlign: 'right', padding: '0.5rem 0.6rem', whiteSpace: 'nowrap', fontVariantNumeric: 'tabular-nums' }}>
                    {r.impactoMin != null ? `${fmtMoeda(r.impactoMin)} / ` : ''}<strong>{fmtMoeda(r.impactoProvavel)}</strong>{r.impactoMax != null ? ` / ${fmtMoeda(r.impactoMax)}` : ''}
                  </td>
                  <td style={{ textAlign: 'right', padding: '0.5rem 0.6rem', fontWeight: 600, whiteSpace: 'nowrap' }}>{fmtMoeda(r.valorEsperado)}</td>
                  <td style={{ textAlign: 'center', padding: '0.5rem 0.6rem' }}><NivelBadge nivel={r.nivel} severidade={r.severidade} /></td>
                  <td style={{ textAlign: 'right', padding: '0.5rem 0.4rem', whiteSpace: 'nowrap' }}>
                    <button onClick={() => abrirEdicao(r)} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: '0.2rem' }}><Pencil size={14} /></button>
                    <button
                      onClick={() => { if (window.confirm(`Excluir o risco "${r.descricao}"?`)) executar(() => call(`/orcamentos/${id}/riscos/${r.id}`, { method: 'DELETE' })); }}
                      style={{ background: 'transparent', border: 'none', color: 'var(--danger)', cursor: 'pointer', padding: '0.2rem' }}
                    ><Trash2 size={14} /></button>
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr style={{ borderTop: '2px solid var(--border-color)', fontWeight: 600 }}>
                <td colSpan={3} style={{ padding: '0.5rem 0.6rem', color: 'var(--text-secondary)' }}>Valor esperado total (Σ p × impacto)</td>
                <td style={{ textAlign: 'right', padding: '0.5rem 0.6rem' }}>{fmtMoeda(analise?.valorEsperadoTotal ?? 0)}</td>
                <td colSpan={2}></td>
              </tr>
            </tfoot>
          </table>
        )}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem', alignItems: 'start' }}>
        {/* Matriz P×I */}
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <h2 style={{ fontSize: '1.1rem', marginBottom: '1rem' }}>Matriz de Risco (Probabilidade × Impacto)</h2>
          <MatrizRisco riscos={analise?.riscos ?? []} />
          <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontStyle: 'italic', marginTop: '0.8rem' }}>
            Impacto classificado em relação ao custo direto ({fmtMoeda(analise?.custoDireto ?? 0)}): &lt;1% Muito Baixo · &lt;5% Baixo · &lt;10% Médio · &lt;20% Alto · ≥20% Muito Alto.
          </p>
        </div>

        {/* Simulação de Monte Carlo */}
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <h2 style={{ fontSize: '1.1rem', marginBottom: '0.4rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Dices size={18} color="var(--accent-primary)" /> Simulação de Monte Carlo
          </h2>
          {!sim ? (
            <p style={{ color: 'var(--text-muted)', fontStyle: 'italic', marginTop: '0.6rem' }}>Cadastre riscos para rodar a simulação.</p>
          ) : (
            <>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
                {sim.iteracoes.toLocaleString('pt-BR')} cenários · distribuição da contingência total (frequência por faixa de valor)
              </p>
              <Histograma sim={sim} />
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0.8rem', marginTop: '1rem' }}>
                {([
                  ['Média', sim.media], ['P50', sim.p50], ['P80', sim.p80], ['P95', sim.p95],
                ] as [string, number][]).map(([rotulo, valor]) => (
                  <div key={rotulo} style={{ padding: '0.6rem', borderRadius: '6px', background: 'var(--bg-tertiary)', textAlign: 'center' }}>
                    <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>{rotulo}</div>
                    <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>{fmtMoeda(valor)}</div>
                  </div>
                ))}
              </div>
              <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', marginTop: '0.8rem' }}>
                IC 95%: {fmtMoeda(sim.icInferior95)} – {fmtMoeda(sim.icSuperior95)} · σ = {fmtMoeda(sim.desvioPadrao)}
              </div>

              <div style={{ display: 'flex', gap: '0.8rem', alignItems: 'flex-end', marginTop: '1.2rem', paddingTop: '1rem', borderTop: '1px solid var(--border-color)' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.82rem' }}>Percentil de proteção</label>
                  <select style={{ ...inputStyle, width: '110px' }} value={percentil} onChange={(e) => setPercentil(e.target.value)}>
                    <option value="P50">P50</option>
                    <option value="P80">P80</option>
                    <option value="P90">P90</option>
                    <option value="P95">P95</option>
                  </select>
                </div>
                <button
                  className="btn-primary"
                  onClick={() => executar(
                    () => call(`/orcamentos/${id}/riscos/aplicar-contingencia`, {
                      method: 'POST',
                      body: JSON.stringify({ percentil, iteracoes: parseInt(iteracoes, 10) }),
                    }),
                    `Contingência ${percentil} aplicada à formação de preço.`
                  )}
                >
                  Aplicar como contingência
                </button>
              </div>
              <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontStyle: 'italic', marginTop: '0.6rem' }}>
                Contingência atual na formação de preço: {fmtPct(analise?.contingenciaAtual ?? 0)} do custo (CD+CI). P80 = valor que cobre os riscos em 80% dos cenários.
              </p>
            </>
          )}
        </div>
      </div>

      {/* Modal novo/editar risco */}
      {modal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000, backdropFilter: 'blur(4px)' }}>
          <div className="glass-panel animate-fade-in" style={{ width: '100%', maxWidth: '620px', padding: '2rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1.2rem' }}>
              <h2 style={{ fontSize: '1.3rem' }}>{modal.editando ? 'Editar Risco' : 'Novo Risco'}</h2>
              <button onClick={() => setModal(null)} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}><X size={22} /></button>
            </div>
            <form onSubmit={(e) => { e.preventDefault(); if (form.descricao.trim() && form.impactoProvavel) salvarRisco(); }} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 180px', gap: '0.8rem' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Descrição *</label>
                  <input style={inputStyle} required value={form.descricao} onChange={(e) => setForm({ ...form, descricao: e.target.value })} placeholder="Ex: Atraso na entrega dos materiais" autoFocus />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Categoria</label>
                  <input style={inputStyle} value={form.categoria} onChange={(e) => setForm({ ...form, categoria: e.target.value })} placeholder="Suprimentos" />
                </div>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0.8rem' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Probabilidade (%) *</label>
                  <input style={inputStyle} required value={form.probabilidade} onChange={(e) => setForm({ ...form, probabilidade: e.target.value })} />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Impacto mín (R$)</label>
                  <input style={inputStyle} value={form.impactoMin} onChange={(e) => setForm({ ...form, impactoMin: e.target.value })} placeholder="opcional" />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Impacto provável (R$) *</label>
                  <input style={inputStyle} required value={form.impactoProvavel} onChange={(e) => setForm({ ...form, impactoProvavel: e.target.value })} />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Impacto máx (R$)</label>
                  <input style={inputStyle} value={form.impactoMax} onChange={(e) => setForm({ ...form, impactoMax: e.target.value })} placeholder="opcional" />
                </div>
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Plano de resposta / mitigação</label>
                <input style={inputStyle} value={form.resposta} onChange={(e) => setForm({ ...form, resposta: e.target.value })} placeholder="Ex: contrato com fornecedor alternativo" />
              </div>
              <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                💡 Informe mín e máx para o impacto ser sorteado da distribuição triangular na simulação; só o provável = impacto fixo.
              </p>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.8rem' }}>
                <button type="button" className="btn-secondary" onClick={() => setModal(null)}>Cancelar</button>
                <button type="submit" className="btn-primary">{modal.editando ? 'Salvar' : 'Adicionar Risco'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
