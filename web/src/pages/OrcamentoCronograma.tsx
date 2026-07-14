import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ArrowLeft, Loader2, CalendarRange, Save } from 'lucide-react';

// ---------- Tipos (espelham os DTOs do backend) ----------

interface Celula { periodo: number; previstoPct: number; realPct: number; previstoValor: number; realValor: number }

interface Linha {
  moduloId: string; moduloNome: string; valorTotal: number;
  celulas: Celula[]; totalPrevistoPct: number; totalRealPct: number; avancoRealValor: number;
}

interface Periodo {
  periodo: number;
  fisicoPrevistoPct: number; fisicoRealPct: number;
  fisicoPrevistoAcumuladoPct: number; fisicoRealAcumuladoPct: number;
  financeiroPrevisto: number; financeiroReal: number;
  financeiroPrevistoAcumulado: number; financeiroRealAcumulado: number;
}

interface Cronograma {
  periodos: number; valorTotal: number; linhas: Linha[]; porPeriodo: Periodo[]; desvioFisicoPct: number;
}

const API = 'http://localhost:8080/api/v1';

// Paleta validada (dataviz): Previsto #3b82f6 tracejado · Real #059669 sólido
const COR_PREVISTO = '#3b82f6';
const COR_REAL = '#059669';

const fmtMoeda = (v: number) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 }).format(v);
const fmtPct = (v: number) => `${(v * 100).toLocaleString('pt-BR', { maximumFractionDigits: 1 })}%`;

const cellInput: React.CSSProperties = {
  width: '52px', padding: '0.25rem 0.3rem', borderRadius: '3px', textAlign: 'right',
  border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', color: 'var(--text-primary)', fontSize: '0.8rem',
};

// ---------- Curva S (2 séries: Previsto tracejado × Real sólido) ----------

const CurvaS: React.FC<{ periodos: Periodo[] }> = ({ periodos }) => {
  const W = 680; const H = 240; const PAD = { top: 14, right: 60, bottom: 26, left: 44 };
  const innerW = W - PAD.left - PAD.right;
  const innerH = H - PAD.top - PAD.bottom;
  const n = periodos.length;
  const x = (i: number) => PAD.left + (n <= 1 ? 0 : (i / (n - 1)) * innerW);
  const y = (frac: number) => PAD.top + (1 - Math.min(frac, 1)) * innerH;

  const caminho = (valores: number[]) => valores.map((v, i) => `${i === 0 ? 'M' : 'L'}${x(i).toFixed(1)},${y(v).toFixed(1)}`).join(' ');

  const previsto = periodos.map((p) => p.fisicoPrevistoAcumuladoPct);
  const real = periodos.map((p) => p.fisicoRealAcumuladoPct);
  const ultimoRealIdx = periodos.reduce((acc, p, i) => (p.financeiroReal > 0 ? i : acc), -1);
  const realVisivel = ultimoRealIdx >= 0 ? real.slice(0, ultimoRealIdx + 1) : [];

  return (
    <div>
      {/* Legenda (2 séries — identidade nunca só por cor: tracejado × sólido) */}
      <div style={{ display: 'flex', gap: '1.2rem', marginBottom: '0.5rem', fontSize: '0.82rem', color: 'var(--text-secondary)' }}>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
          <svg width="26" height="8"><line x1="0" y1="4" x2="26" y2="4" stroke={COR_PREVISTO} strokeWidth="2" strokeDasharray="5 3" /></svg>
          Previsto (acumulado)
        </span>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}>
          <svg width="26" height="8"><line x1="0" y1="4" x2="26" y2="4" stroke={COR_REAL} strokeWidth="2" /></svg>
          Real (acumulado)
        </span>
      </div>
      <svg viewBox={`0 0 ${W} ${H}`} role="img" aria-label="Curva S — avanço físico previsto e real acumulado" style={{ width: '100%', height: 'auto' }}>
        {/* Grid recessivo: 0/25/50/75/100% */}
        {[0, 0.25, 0.5, 0.75, 1].map((g) => (
          <g key={g}>
            <line x1={PAD.left} y1={y(g)} x2={W - PAD.right} y2={y(g)} stroke="var(--border-color)" strokeWidth={0.5} />
            <text x={PAD.left - 6} y={y(g) + 3} textAnchor="end" fontSize={9} fill="var(--text-muted)">{(g * 100).toFixed(0)}%</text>
          </g>
        ))}
        {periodos.map((p, i) => (
          <text key={p.periodo} x={x(i)} y={H - 8} textAnchor="middle" fontSize={9} fill="var(--text-muted)">{p.periodo}</text>
        ))}

        <path d={caminho(previsto)} fill="none" stroke={COR_PREVISTO} strokeWidth={2} strokeDasharray="6 4" />
        {realVisivel.length > 0 && <path d={caminho(realVisivel)} fill="none" stroke={COR_REAL} strokeWidth={2} />}

        {/* Marcadores com tooltip nativo */}
        {periodos.map((p, i) => (
          <g key={`m${p.periodo}`}>
            <circle cx={x(i)} cy={y(previsto[i])} r={3.5} fill={COR_PREVISTO} stroke="var(--bg-secondary)" strokeWidth={2}>
              <title>{`Período ${p.periodo} — Previsto acumulado: ${fmtPct(previsto[i])} (${fmtMoeda(p.financeiroPrevistoAcumulado)})`}</title>
            </circle>
            {i <= ultimoRealIdx && (
              <circle cx={x(i)} cy={y(real[i])} r={3.5} fill={COR_REAL} stroke="var(--bg-secondary)" strokeWidth={2}>
                <title>{`Período ${p.periodo} — Real acumulado: ${fmtPct(real[i])} (${fmtMoeda(p.financeiroRealAcumulado)})`}</title>
              </circle>
            )}
          </g>
        ))}

        {/* Rótulos diretos no último ponto de cada série */}
        {n > 0 && (
          <text x={x(n - 1) + 6} y={y(previsto[n - 1]) + 3} fontSize={10} fill="var(--text-secondary)">{fmtPct(previsto[n - 1])}</text>
        )}
        {ultimoRealIdx >= 0 && (
          <text x={x(ultimoRealIdx) + 6} y={y(real[ultimoRealIdx]) - 6} fontSize={10} fill="var(--text-secondary)">{fmtPct(real[ultimoRealIdx])}</text>
        )}
      </svg>
    </div>
  );
};

// ---------- Página ----------

export const OrcamentoCronograma: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { token } = useAuth();

  const [cron, setCron] = useState<Cronograma | null>(null);
  const [edicao, setEdicao] = useState<Record<string, { previsto: string; real: string }>>({});
  const [numPeriodos, setNumPeriodos] = useState('12');
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');
  const [okMsg, setOkMsg] = useState('');

  const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };

  const chave = (moduloId: string, periodo: number) => `${moduloId}:${periodo}`;

  const hidratar = (dados: Cronograma) => {
    setCron(dados);
    const mapa: Record<string, { previsto: string; real: string }> = {};
    dados.linhas.forEach((l) => l.celulas.forEach((c) => {
      mapa[chave(l.moduloId, c.periodo)] = {
        previsto: c.previstoPct > 0 ? String(Math.round(c.previstoPct * 10000) / 100) : '',
        real: c.realPct > 0 ? String(Math.round(c.realPct * 10000) / 100) : '',
      };
    }));
    setEdicao(mapa);
  };

  const carregar = useCallback(async (periodos = numPeriodos) => {
    try {
      const response = await fetch(`${API}/orcamentos/${id}/cronograma?periodos=${periodos}`, { headers });
      if (!response.ok) throw new Error('Falha ao carregar cronograma');
      const result = await response.json();
      hidratar(result.data);
      setError('');
    } catch (err: any) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  }, [id, token, numPeriodos]);

  useEffect(() => { carregar(); }, []);

  const salvar = async () => {
    if (!cron) return;
    setIsSaving(true);
    setError('');
    setOkMsg('');
    try {
      const alocacoes = Object.entries(edicao).map(([k, v]) => {
        const [moduloId, periodo] = k.split(':');
        return {
          moduloId,
          periodo: parseInt(periodo, 10),
          previstoPct: (parseFloat(v.previsto.replace(',', '.')) || 0) / 100,
          realPct: (parseFloat(v.real.replace(',', '.')) || 0) / 100,
        };
      });
      const response = await fetch(`${API}/orcamentos/${id}/cronograma?periodos=${numPeriodos}`, {
        method: 'PUT', headers, body: JSON.stringify({ alocacoes }),
      });
      if (!response.ok) {
        const errBody = await response.json().catch(() => null);
        throw new Error(errBody?.error?.message || 'Falha ao salvar cronograma');
      }
      const result = await response.json();
      hidratar(result.data);
      setOkMsg('Cronograma salvo e curvas recalculadas.');
    } catch (err: any) {
      setError(err.message);
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return <div style={{ display: 'flex', justifyContent: 'center', padding: '4rem' }}><Loader2 className="animate-spin" size={32} color="var(--accent-primary)" /></div>;
  }

  const desvio = cron?.desvioFisicoPct ?? 0;

  return (
    <div className="animate-fade-in" style={{ padding: '2rem', maxWidth: '1400px', margin: '0 auto' }}>
      <button onClick={() => navigate(`/orcamentos/${id}`)} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.4rem', cursor: 'pointer', marginBottom: '1rem', padding: 0, fontSize: '0.9rem' }}>
        <ArrowLeft size={16} /> Voltar à Planilha Orçamentária
      </button>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.5rem' }}>
        <div>
          <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '1.7rem' }}>
            <CalendarRange color="var(--accent-primary)" /> Cronograma Físico-Financeiro
          </h1>
          <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem' }}>
            Distribua o % de cada módulo pelos períodos (previsto) e registre as medições (real). Avanço e curva S são calculados ponderados pelo valor.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.8rem', alignItems: 'flex-end' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.82rem' }}>Períodos</label>
            <input
              style={{ width: '90px', padding: '0.5rem 0.7rem', borderRadius: '4px', border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', color: 'var(--text-primary)' }}
              type="number" min="1" max="120" value={numPeriodos}
              onChange={(e) => setNumPeriodos(e.target.value)}
              onBlur={() => carregar(numPeriodos || '12')}
            />
          </div>
          <button className="btn-primary" onClick={salvar} disabled={isSaving} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            {isSaving ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />} Salvar Cronograma
          </button>
        </div>
      </div>

      {error && <div style={{ padding: '1rem', backgroundColor: 'rgba(239,68,68,0.1)', border: '1px solid var(--danger)', borderRadius: 'var(--border-radius-md)', color: 'var(--danger)', marginBottom: '1.5rem' }}>{error}</div>}
      {okMsg && <div style={{ padding: '1rem', backgroundColor: 'rgba(16,185,129,0.1)', border: '1px solid var(--success)', borderRadius: 'var(--border-radius-md)', color: 'var(--success)', marginBottom: '1.5rem' }}>{okMsg}</div>}

      {cron && cron.linhas.length === 0 ? (
        <div className="glass-panel" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          <h3>Sem módulos na EAP</h3>
          <p style={{ marginTop: '0.5rem' }}>Monte a planilha orçamentária primeiro — o cronograma distribui os módulos no tempo.</p>
        </div>
      ) : cron && (
        <>
          {/* Grid módulos × períodos */}
          <div className="glass-panel" style={{ padding: '1.5rem', marginBottom: '2rem', overflowX: 'auto' }}>
            <table style={{ borderCollapse: 'collapse', fontSize: '0.82rem', minWidth: '100%' }}>
              <thead>
                <tr style={{ color: 'var(--text-muted)', fontSize: '0.72rem', textTransform: 'uppercase' }}>
                  <th style={{ textAlign: 'left', padding: '0.4rem 0.6rem', position: 'sticky', left: 0, background: 'var(--bg-secondary)' }}>Módulo</th>
                  <th style={{ textAlign: 'right', padding: '0.4rem 0.6rem' }}>Valor (c/ BDI)</th>
                  {Array.from({ length: cron.periodos }, (_, i) => (
                    <th key={i} colSpan={2} style={{ textAlign: 'center', padding: '0.4rem 0.4rem', borderLeft: '1px solid var(--border-color)' }}>{i + 1}</th>
                  ))}
                  <th style={{ textAlign: 'right', padding: '0.4rem 0.6rem', borderLeft: '1px solid var(--border-color)' }}>Σ Prev / Real</th>
                </tr>
                <tr style={{ color: 'var(--text-muted)', fontSize: '0.68rem' }}>
                  <th style={{ position: 'sticky', left: 0, background: 'var(--bg-secondary)' }}></th>
                  <th></th>
                  {Array.from({ length: cron.periodos }, (_, i) => (
                    <React.Fragment key={i}>
                      <th style={{ padding: '0.15rem 0.2rem', borderLeft: '1px solid var(--border-color)' }}>Prev %</th>
                      <th style={{ padding: '0.15rem 0.2rem' }}>Real %</th>
                    </React.Fragment>
                  ))}
                  <th style={{ borderLeft: '1px solid var(--border-color)' }}></th>
                </tr>
              </thead>
              <tbody>
                {cron.linhas.map((linha) => (
                  <tr key={linha.moduloId} style={{ borderTop: '1px solid var(--border-color)' }}>
                    <td style={{ padding: '0.4rem 0.6rem', whiteSpace: 'nowrap', position: 'sticky', left: 0, background: 'var(--bg-secondary)' }}>{linha.moduloNome}</td>
                    <td style={{ padding: '0.4rem 0.6rem', textAlign: 'right', whiteSpace: 'nowrap', fontVariantNumeric: 'tabular-nums' }}>{fmtMoeda(linha.valorTotal)}</td>
                    {linha.celulas.map((c) => {
                      const k = chave(linha.moduloId, c.periodo);
                      const v = edicao[k] ?? { previsto: '', real: '' };
                      return (
                        <React.Fragment key={c.periodo}>
                          <td style={{ padding: '0.2rem', borderLeft: '1px solid var(--border-color)' }}>
                            <input style={cellInput} value={v.previsto} placeholder="–" onChange={(e) => setEdicao({ ...edicao, [k]: { ...v, previsto: e.target.value } })} />
                          </td>
                          <td style={{ padding: '0.2rem' }}>
                            <input style={{ ...cellInput, borderColor: v.real ? COR_REAL : 'var(--border-color)' }} value={v.real} placeholder="–" onChange={(e) => setEdicao({ ...edicao, [k]: { ...v, real: e.target.value } })} />
                          </td>
                        </React.Fragment>
                      );
                    })}
                    <td style={{ padding: '0.4rem 0.6rem', textAlign: 'right', whiteSpace: 'nowrap', borderLeft: '1px solid var(--border-color)', fontVariantNumeric: 'tabular-nums' }}>
                      <span style={{ color: linha.totalPrevistoPct > 1 ? 'var(--danger)' : 'var(--text-secondary)' }}>{fmtPct(linha.totalPrevistoPct)}</span>
                      {' / '}
                      <span style={{ color: linha.totalRealPct > 1 ? 'var(--danger)' : 'var(--text-primary)', fontWeight: 600 }}>{fmtPct(linha.totalRealPct)}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr style={{ borderTop: '2px solid var(--border-color)', fontWeight: 600, fontSize: '0.78rem' }}>
                  <td style={{ padding: '0.4rem 0.6rem', position: 'sticky', left: 0, background: 'var(--bg-secondary)' }}>Avanço físico (período)</td>
                  <td style={{ padding: '0.4rem 0.6rem', textAlign: 'right' }}>{fmtMoeda(cron.valorTotal)}</td>
                  {cron.porPeriodo.map((p) => (
                    <React.Fragment key={p.periodo}>
                      <td style={{ padding: '0.3rem 0.2rem', textAlign: 'right', borderLeft: '1px solid var(--border-color)', color: 'var(--text-secondary)' }}>{fmtPct(p.fisicoPrevistoPct)}</td>
                      <td style={{ padding: '0.3rem 0.2rem', textAlign: 'right' }}>{fmtPct(p.fisicoRealPct)}</td>
                    </React.Fragment>
                  ))}
                  <td style={{ borderLeft: '1px solid var(--border-color)' }}></td>
                </tr>
                <tr style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                  <td style={{ padding: '0.3rem 0.6rem', position: 'sticky', left: 0, background: 'var(--bg-secondary)' }}>Desembolso acumulado (R$)</td>
                  <td></td>
                  {cron.porPeriodo.map((p) => (
                    <td key={p.periodo} colSpan={2} style={{ padding: '0.3rem 0.2rem', textAlign: 'center', borderLeft: '1px solid var(--border-color)', whiteSpace: 'nowrap' }}>
                      {fmtMoeda(p.financeiroPrevistoAcumulado)}
                    </td>
                  ))}
                  <td style={{ borderLeft: '1px solid var(--border-color)' }}></td>
                </tr>
              </tfoot>
            </table>
          </div>

          {/* Curva S + resumo */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 300px', gap: '2rem', alignItems: 'start' }}>
            <div className="glass-panel" style={{ padding: '1.5rem' }}>
              <h2 style={{ fontSize: '1.1rem', marginBottom: '1rem' }}>Curva S — Avanço Físico Acumulado</h2>
              <CurvaS periodos={cron.porPeriodo} />
            </div>
            <div className="glass-panel" style={{ padding: '1.5rem', position: 'sticky', top: '2rem' }}>
              <h2 style={{ fontSize: '1.1rem', marginBottom: '1rem' }}>Situação da Obra</h2>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem', fontSize: '0.9rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--text-secondary)' }}>Valor total (c/ BDI)</span>
                  <strong>{fmtMoeda(cron.valorTotal)}</strong>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--text-secondary)' }}>Medido até agora</span>
                  <strong>{fmtMoeda(cron.linhas.reduce((acc, l) => acc + l.avancoRealValor, 0))}</strong>
                </div>
                <div style={{ paddingTop: '0.8rem', borderTop: '1px solid var(--border-color)' }}>
                  <div style={{ color: 'var(--text-secondary)', fontSize: '0.82rem', marginBottom: '0.3rem' }}>Desvio físico (real − previsto)</div>
                  <div style={{ fontSize: '1.4rem', fontWeight: 700, color: desvio >= 0 ? 'var(--success)' : 'var(--danger)' }}>
                    {desvio >= 0 ? '▲ adiantada ' : '▼ atrasada '}{fmtPct(Math.abs(desvio))}
                  </div>
                  <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontStyle: 'italic', marginTop: '0.4rem' }}>
                    Medido no último período com medição real registrada.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};
