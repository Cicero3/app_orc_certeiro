import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ArrowLeft, Loader2, Users } from 'lucide-react';

interface ItemDim {
  itemId: string; moduloNome: string; codigoItem: string; descricao: string; unidade: string | null;
  quantidade: number; indiceHorasPorUnidade: number; horasTotais: number; diasEquipeBasica: number; temCpuDeMo: boolean;
}

interface Dimensionamento {
  jornadaHorasDia: number;
  itens: ItemDim[];
  totalHoras: number;
  totalDiasSequenciais: number;
  horasPorFuncao: Record<string, number>;
}

const API = 'http://localhost:8080/api/v1';
const fmt = (v: number, casas = 2) => v.toLocaleString('pt-BR', { minimumFractionDigits: casas, maximumFractionDigits: casas });

const thStyle: React.CSSProperties = {
  textAlign: 'right', padding: '0.5rem 0.6rem', fontSize: '0.75rem', textTransform: 'uppercase',
  color: 'var(--text-muted)', letterSpacing: '0.04em', whiteSpace: 'nowrap',
};
const tdNum: React.CSSProperties = { textAlign: 'right', padding: '0.45rem 0.6rem', whiteSpace: 'nowrap', fontVariantNumeric: 'tabular-nums' };

export const OrcamentoDimensionamento: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { token } = useAuth();

  const [dim, setDim] = useState<Dimensionamento | null>(null);
  const [jornada, setJornada] = useState('8');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const carregar = useCallback(async (horas: string) => {
    setIsLoading(true);
    try {
      const response = await fetch(`${API}/orcamentos/${id}/dimensionamento?jornada=${encodeURIComponent(horas)}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!response.ok) {
        const errBody = await response.json().catch(() => null);
        throw new Error(errBody?.error?.message || 'Falha ao calcular dimensionamento');
      }
      const result = await response.json();
      setDim(result.data);
      setError('');
    } catch (err: any) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  }, [id, token]);

  useEffect(() => {
    const timer = setTimeout(() => carregar(jornada || '8'), 400);
    return () => clearTimeout(timer);
  }, [jornada, carregar]);

  return (
    <div className="animate-fade-in" style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto' }}>
      <button onClick={() => navigate(`/orcamentos/${id}`)} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.4rem', cursor: 'pointer', marginBottom: '1rem', padding: 0, fontSize: '0.9rem' }}>
        <ArrowLeft size={16} /> Voltar à Planilha Orçamentária
      </button>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: '1rem', marginBottom: '2rem' }}>
        <div>
          <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '1.7rem' }}>
            <Users color="var(--accent-primary)" /> Dimensionamento de Equipes
          </h1>
          <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem' }}>
            Duração por serviço derivada dos coeficientes de mão de obra da CPU — a mesma fonte do custo.
          </p>
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Jornada (h/dia)</label>
          <input
            style={{ width: '110px', padding: '0.5rem 0.7rem', borderRadius: '4px', border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', color: 'var(--text-primary)' }}
            type="number" min="1" step="0.5" value={jornada} onChange={(e) => setJornada(e.target.value)}
          />
        </div>
      </div>

      {error && <div style={{ padding: '1rem', backgroundColor: 'rgba(239,68,68,0.1)', border: '1px solid var(--danger)', borderRadius: 'var(--border-radius-md)', color: 'var(--danger)', marginBottom: '1.5rem' }}>{error}</div>}

      {isLoading || !dim ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '3rem' }}><Loader2 className="animate-spin" size={32} color="var(--accent-primary)" /></div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 320px', gap: '2rem', alignItems: 'start' }}>
          <div className="glass-panel" style={{ padding: '1.5rem', overflowX: 'auto' }}>
            <h2 style={{ fontSize: '1.1rem', marginBottom: '1rem' }}>Duração por Serviço</h2>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.88rem' }}>
              <thead>
                <tr>
                  <th style={{ ...thStyle, textAlign: 'left' }}>Item</th>
                  <th style={{ ...thStyle, textAlign: 'left' }}>Serviço</th>
                  <th style={thStyle}>Qtd</th>
                  <th style={thStyle}>Índice (h/und)</th>
                  <th style={thStyle}>Horas</th>
                  <th style={thStyle}>Dias (equipe básica)</th>
                </tr>
              </thead>
              <tbody>
                {dim.itens.length === 0 ? (
                  <tr><td colSpan={6} style={{ padding: '1rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>Nenhum item na EAP.</td></tr>
                ) : dim.itens.map((item) => (
                  <tr key={item.itemId} style={{ borderTop: '1px solid var(--border-color)', opacity: item.temCpuDeMo ? 1 : 0.55 }}>
                    <td style={{ padding: '0.45rem 0.6rem', whiteSpace: 'nowrap', color: 'var(--text-muted)' }}>{item.codigoItem}</td>
                    <td style={{ padding: '0.45rem 0.6rem' }}>
                      {item.descricao}
                      {!item.temCpuDeMo && <span style={{ marginLeft: '0.4rem', fontSize: '0.72rem', color: 'var(--warning)' }} title="Item sem insumos de MO na CPU — sem estimativa">sem CPU de MO</span>}
                    </td>
                    <td style={tdNum}>{fmt(item.quantidade)} {item.unidade ?? ''}</td>
                    <td style={tdNum}>{fmt(item.indiceHorasPorUnidade, 4)}</td>
                    <td style={tdNum}>{fmt(item.horasTotais)}</td>
                    <td style={{ ...tdNum, fontWeight: 600, color: item.temCpuDeMo ? 'var(--success)' : 'var(--text-muted)' }}>{item.diasEquipeBasica}</td>
                  </tr>
                ))}
              </tbody>
              {dim.itens.length > 0 && (
                <tfoot>
                  <tr style={{ borderTop: '2px solid var(--border-color)', fontWeight: 600 }}>
                    <td colSpan={4} style={{ padding: '0.5rem 0.6rem', color: 'var(--text-secondary)' }}>Totais (execução sequencial)</td>
                    <td style={tdNum}>{fmt(dim.totalHoras)} h</td>
                    <td style={{ ...tdNum, color: 'var(--success)' }}>{dim.totalDiasSequenciais} dias</td>
                  </tr>
                </tfoot>
              )}
            </table>
          </div>

          <div className="glass-panel" style={{ padding: '1.5rem', position: 'sticky', top: '2rem' }}>
            <h2 style={{ fontSize: '1.1rem', marginBottom: '1rem' }}>Horas por Função</h2>
            {Object.keys(dim.horasPorFuncao).length === 0 ? (
              <p style={{ color: 'var(--text-muted)', fontStyle: 'italic', fontSize: '0.9rem' }}>
                Adicione itens com CPU (insumos de mão de obra) para ver o histograma de recursos.
              </p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
                {Object.entries(dim.horasPorFuncao).map(([funcao, horas]) => (
                  <div key={funcao} style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.4rem' }}>
                    <span style={{ fontSize: '0.9rem' }}>{funcao}</span>
                    <strong style={{ fontVariantNumeric: 'tabular-nums' }}>{fmt(horas)} h</strong>
                  </div>
                ))}
              </div>
            )}
            <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontStyle: 'italic', marginTop: '1rem' }}>
              Dias = ⌈quantidade × índice ÷ jornada⌉ por serviço, com 1 equipe básica. Serviços podem rodar em paralelo — o total sequencial é o teto.
            </p>
          </div>
        </div>
      )}
    </div>
  );
};
