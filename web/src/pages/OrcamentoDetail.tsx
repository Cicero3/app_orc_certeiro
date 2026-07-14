import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  ArrowLeft, Plus, Trash2, Loader2, FileSpreadsheet, Search, X,
  Pencil, Check, PackagePlus, Send,
} from 'lucide-react';

// ---------- Tipos (espelham os DTOs do backend) ----------

interface ComposicaoDetail {
  id: string;
  tipoInsumo: string;
  descricao: string;
  unidade: string;
  coeficiente: number;
  custoUnitarioInsumo: number;
  custoTotalInsumo: number;
}

interface EapItemDetail {
  id: string;
  codigoItem: string;
  descricao: string;
  marca: string | null;
  unidade: string | null;
  quantidade: number;
  valorMo: number;
  valorMat: number;
  valorSrv: number;
  valorUnitario: number;
  custoTotal: number;
  precoTotal: number;
  observacoes: string | null;
  composicoes: ComposicaoDetail[];
  subItens: EapItemDetail[];
}

interface ModuloPlanilha {
  id: string;
  tipoModulo: string;
  nome: string;
  itens: EapItemDetail[];
  totalMo: number;
  totalMat: number;
  totalSrv: number;
  totalCusto: number;
  totalPreco: number;
  percentual: number;
}

interface PlanilhaTotais {
  totalMo: number;
  totalMat: number;
  totalSrv: number;
  custoDireto: number;
  precoComBdi: number;
}

interface OrcamentoDetail {
  id: string;
  titulo: string;
  status: string;
  bdi: number;
  createdAt: string;
  updatedAt: string;
  modulos: ModuloPlanilha[];
  totais: PlanilhaTotais;
}

interface CatalogoItemSummary {
  id: string;
  codigo: string;
  descricao: string;
  unidade: string;
  valorTotal: number;
}

const API = 'http://localhost:8080/api/v1';

const TIPOS_MODULO = [
  'DEMOLICAO_CONSTRUCAO', 'FUNDACAO', 'ELETRICA_DISTRIBUICAO', 'ELETRICA_PRUMADAS',
  'ESGOTO_PLUVIAL', 'HIDRAULICA', 'ILUMINACAO', 'EQUIPAMENTOS', 'FORRO', 'REVESTIMENTO',
  'MARMORARIA', 'PINTURA', 'MARCENARIA', 'AR_CONDICIONADO', 'EXAUSTAO', 'RENOVACAO_AR',
  'MEMORIAL_DESCRITIVO', 'OUTROS',
];

const ACOES_POR_STATUS: Record<string, { acao: string; label: string }[]> = {
  RASCUNHO: [{ acao: 'SOLICITAR_APROVACAO', label: 'Enviar para revisão' }, { acao: 'CANCELAR', label: 'Cancelar' }],
  EM_REVISAO: [
    { acao: 'APROVAR', label: 'Aprovar (admin)' },
    { acao: 'REJEITAR', label: 'Rejeitar (admin)' },
    { acao: 'CANCELAR', label: 'Cancelar' },
  ],
  APROVADO: [{ acao: 'ENVIAR_CLIENTE', label: 'Enviar ao cliente' }, { acao: 'CANCELAR', label: 'Cancelar' }],
  ENVIADO_CLIENTE: [
    { acao: 'ACEITAR_CLIENTE', label: 'Cliente aceitou' },
    { acao: 'RECUSAR_CLIENTE', label: 'Cliente recusou' },
  ],
};

const fmtMoeda = (v: number) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v);
const fmtNum = (v: number, casas = 2) => v.toLocaleString('pt-BR', { minimumFractionDigits: casas, maximumFractionDigits: casas });

const inputStyle: React.CSSProperties = {
  width: '100%', padding: '0.5rem 0.7rem', borderRadius: '4px',
  border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', color: 'var(--text-primary)',
};

const thStyle: React.CSSProperties = {
  textAlign: 'right', padding: '0.5rem 0.6rem', fontSize: '0.75rem', textTransform: 'uppercase',
  color: 'var(--text-muted)', letterSpacing: '0.04em', whiteSpace: 'nowrap',
};

const tdNum: React.CSSProperties = { textAlign: 'right', padding: '0.45rem 0.6rem', whiteSpace: 'nowrap', fontVariantNumeric: 'tabular-nums' };

// ---------- Componente principal ----------

export const OrcamentoDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { token } = useAuth();

  const [orc, setOrc] = useState<OrcamentoDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  // BDI
  const [bdiEdit, setBdiEdit] = useState(false);
  const [bdiValor, setBdiValor] = useState('');

  // Modais
  const [moduloModal, setModuloModal] = useState(false);
  const [itemModal, setItemModal] = useState<{ moduloId?: string; parentId?: string } | null>(null);
  const [catalogoModal, setCatalogoModal] = useState<{ moduloId?: string; parentId?: string } | null>(null);
  const [cpuModal, setCpuModal] = useState<{ moduloId?: string; parentId?: string } | null>(null);

  // Edição inline de item
  const [editItem, setEditItem] = useState<{ id: string; quantidade: string; valorMo: string; valorMat: string; valorSrv: string } | null>(null);

  const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };

  const call = useCallback(async (path: string, options: RequestInit = {}) => {
    const response = await fetch(`${API}${path}`, { headers, ...options });
    if (!response.ok) {
      let msg = 'Falha na operação';
      try { msg = (await response.json()).error?.message || msg; } catch { /* corpo vazio */ }
      throw new Error(msg);
    }
    return response.status === 204 ? null : response.json();
  }, [token]);

  const fetchOrcamento = useCallback(async () => {
    try {
      const result = await call(`/orcamentos/${id}`);
      setOrc(result.data);
      setError('');
    } catch (err: any) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  }, [id, call]);

  useEffect(() => { fetchOrcamento(); }, [fetchOrcamento]);

  const executar = async (fn: () => Promise<any>) => {
    try {
      await fn();
      await fetchOrcamento();
      setError('');
    } catch (err: any) {
      setError(err.message);
    }
  };

  const editavel = orc?.status === 'RASCUNHO' || orc?.status === 'EM_REVISAO';

  // ---------- Ações ----------

  const salvarBdi = () => executar(async () => {
    const bdiDecimal = parseFloat(bdiValor.replace(',', '.')) / 100;
    await call(`/orcamentos/${id}/bdi`, { method: 'PATCH', body: JSON.stringify({ bdi: bdiDecimal }) });
    setBdiEdit(false);
  });

  const mudarStatus = (acao: string) => {
    if (acao === 'CANCELAR' && !window.confirm('Cancelar este orçamento? A ação não pode ser desfeita.')) return;
    executar(() => call(`/orcamentos/${id}/status`, { method: 'POST', body: JSON.stringify({ acao }) }));
  };

  const removerModulo = (moduloId: string) => {
    if (!window.confirm('Excluir este módulo e todos os seus itens?')) return;
    executar(() => call(`/orcamentos/${id}/modulos/${moduloId}`, { method: 'DELETE' }));
  };

  const removerItem = (itemId: string) => {
    if (!window.confirm('Excluir este item da EAP?')) return;
    executar(() => call(`/orcamentos/${id}/itens/${itemId}`, { method: 'DELETE' }));
  };

  const salvarEdicaoItem = () => {
    if (!editItem) return;
    executar(async () => {
      await call(`/orcamentos/${id}/itens/${editItem.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          quantidade: parseFloat(editItem.quantidade.replace(',', '.')) || 0,
          valorMo: parseFloat(editItem.valorMo.replace(',', '.')) || 0,
          valorMat: parseFloat(editItem.valorMat.replace(',', '.')) || 0,
          valorSrv: parseFloat(editItem.valorSrv.replace(',', '.')) || 0,
        }),
      });
      setEditItem(null);
    });
  };

  // ---------- Renderização de itens (recursivo para subitens) ----------

  const renderItem = (item: EapItemDetail, nivel: number): React.ReactNode => {
    const emEdicao = editItem?.id === item.id;
    return (
      <React.Fragment key={item.id}>
        <tr style={{ borderTop: '1px solid var(--border-color)' }}>
          <td style={{ padding: '0.45rem 0.6rem', whiteSpace: 'nowrap', color: 'var(--text-muted)', paddingLeft: `${0.6 + nivel * 1.2}rem` }}>
            {item.codigoItem}
          </td>
          <td style={{ padding: '0.45rem 0.6rem', minWidth: '240px' }}>
            {item.descricao}
            {item.composicoes.length > 0 && (
              <span title={`${item.composicoes.length} insumos na CPU`} style={{ marginLeft: '0.4rem', fontSize: '0.7rem', color: 'var(--accent-primary)' }}>
                CPU·{item.composicoes.length}
              </span>
            )}
          </td>
          <td style={{ ...tdNum, color: 'var(--text-muted)' }}>{item.unidade ?? '-'}</td>
          {emEdicao ? (
            <>
              <td style={tdNum}><input style={{ ...inputStyle, width: '70px', textAlign: 'right' }} value={editItem.quantidade} onChange={(e) => setEditItem({ ...editItem, quantidade: e.target.value })} /></td>
              <td style={tdNum}><input style={{ ...inputStyle, width: '80px', textAlign: 'right' }} value={editItem.valorMo} onChange={(e) => setEditItem({ ...editItem, valorMo: e.target.value })} /></td>
              <td style={tdNum}><input style={{ ...inputStyle, width: '80px', textAlign: 'right' }} value={editItem.valorMat} onChange={(e) => setEditItem({ ...editItem, valorMat: e.target.value })} /></td>
              <td style={tdNum}><input style={{ ...inputStyle, width: '80px', textAlign: 'right' }} value={editItem.valorSrv} onChange={(e) => setEditItem({ ...editItem, valorSrv: e.target.value })} /></td>
            </>
          ) : (
            <>
              <td style={tdNum}>{fmtNum(item.quantidade)}</td>
              <td style={tdNum}>{fmtNum(item.valorMo)}</td>
              <td style={tdNum}>{fmtNum(item.valorMat)}</td>
              <td style={tdNum}>{fmtNum(item.valorSrv)}</td>
            </>
          )}
          <td style={tdNum}>{fmtNum(item.valorUnitario)}</td>
          <td style={{ ...tdNum, fontWeight: 500 }}>{fmtMoeda(item.custoTotal)}</td>
          <td style={{ ...tdNum, color: 'var(--success)', fontWeight: 600 }}>{fmtMoeda(item.precoTotal)}</td>
          <td style={{ padding: '0.45rem 0.4rem', whiteSpace: 'nowrap', textAlign: 'right' }}>
            {editavel && (
              emEdicao ? (
                <>
                  <button onClick={salvarEdicaoItem} title="Salvar" style={{ background: 'transparent', border: 'none', color: 'var(--success)', cursor: 'pointer', padding: '0.2rem' }}><Check size={15} /></button>
                  <button onClick={() => setEditItem(null)} title="Cancelar" style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: '0.2rem' }}><X size={15} /></button>
                </>
              ) : (
                <>
                  <button
                    onClick={() => setEditItem({ id: item.id, quantidade: String(item.quantidade), valorMo: String(item.valorMo), valorMat: String(item.valorMat), valorSrv: String(item.valorSrv) })}
                    title="Editar valores" style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: '0.2rem' }}
                  ><Pencil size={14} /></button>
                  <button onClick={() => setItemModal({ parentId: item.id })} title="Adicionar subitem" style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: '0.2rem' }}><Plus size={15} /></button>
                  <button onClick={() => removerItem(item.id)} title="Excluir" style={{ background: 'transparent', border: 'none', color: 'var(--danger)', cursor: 'pointer', padding: '0.2rem' }}><Trash2 size={14} /></button>
                </>
              )
            )}
          </td>
        </tr>
        {item.subItens.map((sub) => renderItem(sub, nivel + 1))}
      </React.Fragment>
    );
  };

  // ---------- Render ----------

  if (isLoading) {
    return <div style={{ display: 'flex', justifyContent: 'center', padding: '4rem' }}><Loader2 className="animate-spin" size={32} color="var(--accent-primary)" /></div>;
  }

  if (!orc) {
    return (
      <div className="animate-fade-in" style={{ padding: '2rem' }}>
        <p style={{ color: 'var(--danger)' }}>{error || 'Orçamento não encontrado.'}</p>
        <button className="btn-secondary" onClick={() => navigate('/orcamentos')} style={{ marginTop: '1rem' }}>Voltar</button>
      </div>
    );
  }

  const acoes = ACOES_POR_STATUS[orc.status] ?? [];

  return (
    <div className="animate-fade-in" style={{ padding: '2rem' }}>
      {/* Cabeçalho */}
      <button
        onClick={() => navigate('/orcamentos')}
        style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.4rem', cursor: 'pointer', marginBottom: '1rem', padding: 0, fontSize: '0.9rem' }}
      >
        <ArrowLeft size={16} /> Meus Orçamentos
      </button>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.5rem' }}>
        <div>
          <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '1.6rem' }}>
            <FileSpreadsheet color="var(--accent-primary)" /> {orc.titulo}
          </h1>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginTop: '0.5rem', flexWrap: 'wrap' }}>
            <span style={{ padding: '0.25rem 0.7rem', borderRadius: '1rem', fontSize: '0.8rem', background: 'var(--bg-secondary)', color: 'var(--text-secondary)' }}>{orc.status}</span>
            <span style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
              BDI:{' '}
              {bdiEdit ? (
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
                  <input style={{ ...inputStyle, width: '80px', display: 'inline-block', padding: '0.2rem 0.4rem' }} value={bdiValor} onChange={(e) => setBdiValor(e.target.value)} autoFocus />%
                  <button onClick={salvarBdi} style={{ background: 'transparent', border: 'none', color: 'var(--success)', cursor: 'pointer' }}><Check size={15} /></button>
                  <button onClick={() => setBdiEdit(false)} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}><X size={15} /></button>
                </span>
              ) : (
                <strong style={{ color: 'var(--text-primary)', cursor: editavel ? 'pointer' : 'default' }} onClick={() => { if (editavel) { setBdiValor(fmtNum(orc.bdi * 100)); setBdiEdit(true); } }}>
                  {fmtNum(orc.bdi * 100)}% {editavel && <Pencil size={12} style={{ display: 'inline' }} />}
                </strong>
              )}
            </span>
          </div>
        </div>

        <div style={{ textAlign: 'right' }}>
          <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Preço total (c/ BDI)</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 700, color: 'var(--success)' }}>{fmtMoeda(orc.totais.precoComBdi)}</div>
          <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Custo direto: {fmtMoeda(orc.totais.custoDireto)}</div>
        </div>
      </div>

      {error && (
        <div style={{ padding: '1rem', backgroundColor: 'rgba(239, 68, 68, 0.1)', border: '1px solid var(--danger)', borderRadius: 'var(--border-radius-md)', color: 'var(--danger)', marginBottom: '1.5rem' }}>
          {error}
        </div>
      )}

      {/* Ações de workflow + adicionar módulo */}
      <div style={{ display: 'flex', gap: '0.8rem', marginBottom: '2rem', flexWrap: 'wrap' }}>
        {editavel && (
          <button className="btn-primary" onClick={() => setModuloModal(true)} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <Plus size={16} /> Novo Módulo
          </button>
        )}
        <button className="btn-secondary" onClick={() => navigate(`/orcamentos/${id}/precificacao`)} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          💰 Precificação e BDI
        </button>
        <button className="btn-secondary" onClick={() => navigate(`/orcamentos/${id}/dimensionamento`)} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          👷 Equipes e Prazos
        </button>
        <button className="btn-secondary" onClick={() => navigate(`/orcamentos/${id}/riscos`)} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          🎲 Riscos e Contingência
        </button>
        <button className="btn-secondary" onClick={() => navigate(`/orcamentos/${id}/cronograma`)} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          📅 Cronograma
        </button>
        <button className="btn-secondary" onClick={() => navigate(`/orcamentos/${id}/diario`)} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          📓 Diário de Obra
        </button>
        {acoes.map((a) => (
          <button key={a.acao} className="btn-secondary" onClick={() => mudarStatus(a.acao)} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <Send size={14} /> {a.label}
          </button>
        ))}
      </div>

      {/* Módulos */}
      {orc.modulos.length === 0 ? (
        <div className="glass-panel" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          <h3>Planilha vazia</h3>
          <p style={{ marginTop: '0.5rem' }}>Crie o primeiro módulo (ex.: Demolição, Hidráulica, Pintura...) para começar a EAP.</p>
        </div>
      ) : (
        orc.modulos.map((mod) => (
          <div key={mod.id} className="glass-panel" style={{ padding: '1.2rem 1.5rem', marginBottom: '1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.8rem', flexWrap: 'wrap', gap: '0.6rem' }}>
              <div>
                <h2 style={{ fontSize: '1.15rem' }}>{mod.nome}</h2>
                <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                  {mod.tipoModulo} · participação {fmtNum(mod.percentual * 100, 1)}%
                </span>
              </div>
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                {editavel && (
                  <>
                    <button className="btn-secondary" onClick={() => setItemModal({ moduloId: mod.id })} style={{ padding: '0.35rem 0.7rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                      <Plus size={14} /> Item
                    </button>
                    <button className="btn-secondary" onClick={() => setCatalogoModal({ moduloId: mod.id })} style={{ padding: '0.35rem 0.7rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                      <PackagePlus size={14} /> Do Catálogo
                    </button>
                    <button className="btn-secondary" onClick={() => setCpuModal({ moduloId: mod.id })} style={{ padding: '0.35rem 0.7rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                      <PackagePlus size={14} /> Minha CPU
                    </button>
                    <button onClick={() => removerModulo(mod.id)} title="Excluir módulo" style={{ background: 'transparent', border: 'none', color: 'var(--danger)', cursor: 'pointer', padding: '0.3rem' }}>
                      <Trash2 size={16} />
                    </button>
                  </>
                )}
              </div>
            </div>

            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.88rem' }}>
                <thead>
                  <tr>
                    <th style={{ ...thStyle, textAlign: 'left' }}>Item</th>
                    <th style={{ ...thStyle, textAlign: 'left' }}>Descrição</th>
                    <th style={thStyle}>Und</th>
                    <th style={thStyle}>Qtd</th>
                    <th style={thStyle}>M.O</th>
                    <th style={thStyle}>Mat</th>
                    <th style={thStyle}>Serv</th>
                    <th style={thStyle}>R$ Unit</th>
                    <th style={thStyle}>Custo Total</th>
                    <th style={thStyle}>Preço c/ BDI</th>
                    <th style={thStyle}></th>
                  </tr>
                </thead>
                <tbody>
                  {mod.itens.length === 0 ? (
                    <tr><td colSpan={11} style={{ padding: '1rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>Nenhum item neste módulo.</td></tr>
                  ) : (
                    mod.itens.map((item) => renderItem(item, 0))
                  )}
                </tbody>
                {mod.itens.length > 0 && (
                  <tfoot>
                    <tr style={{ borderTop: '2px solid var(--border-color)', fontWeight: 600 }}>
                      <td colSpan={4} style={{ padding: '0.5rem 0.6rem', color: 'var(--text-secondary)' }}>Subtotal do módulo</td>
                      <td style={tdNum}>{fmtNum(mod.totalMo)}</td>
                      <td style={tdNum}>{fmtNum(mod.totalMat)}</td>
                      <td style={tdNum}>{fmtNum(mod.totalSrv)}</td>
                      <td style={tdNum}></td>
                      <td style={tdNum}>{fmtMoeda(mod.totalCusto)}</td>
                      <td style={{ ...tdNum, color: 'var(--success)' }}>{fmtMoeda(mod.totalPreco)}</td>
                      <td></td>
                    </tr>
                  </tfoot>
                )}
              </table>
            </div>
          </div>
        ))
      )}

      {/* Totais gerais */}
      {orc.modulos.length > 0 && (
        <div className="glass-panel" style={{ padding: '1.5rem', background: 'linear-gradient(145deg, rgba(30,41,59,0.7) 0%, rgba(15,23,42,0.9) 100%)' }}>
          <h2 style={{ fontSize: '1.1rem', marginBottom: '1rem' }}>Totais do Orçamento</h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: '1rem' }}>
            <div><div style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>Mão de Obra</div><div style={{ fontSize: '1.1rem', fontWeight: 600 }}>{fmtMoeda(orc.totais.totalMo)}</div></div>
            <div><div style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>Material</div><div style={{ fontSize: '1.1rem', fontWeight: 600 }}>{fmtMoeda(orc.totais.totalMat)}</div></div>
            <div><div style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>Serviços</div><div style={{ fontSize: '1.1rem', fontWeight: 600 }}>{fmtMoeda(orc.totais.totalSrv)}</div></div>
            <div><div style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>Custo Direto</div><div style={{ fontSize: '1.1rem', fontWeight: 600 }}>{fmtMoeda(orc.totais.custoDireto)}</div></div>
            <div><div style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>Preço c/ BDI {fmtNum(orc.bdi * 100, 1)}%</div><div style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--success)' }}>{fmtMoeda(orc.totais.precoComBdi)}</div></div>
          </div>
        </div>
      )}

      {/* Modais */}
      {moduloModal && (
        <ModuloModal
          onClose={() => setModuloModal(false)}
          onSave={(nome, tipo) => executar(async () => {
            await call(`/orcamentos/${id}/modulos`, { method: 'POST', body: JSON.stringify({ nome, tipoModulo: tipo }) });
            setModuloModal(false);
          })}
        />
      )}

      {itemModal && (
        <ItemModal
          onClose={() => setItemModal(null)}
          onSave={(dados) => executar(async () => {
            await call(`/orcamentos/${id}/itens`, { method: 'POST', body: JSON.stringify({ ...dados, ...itemModal }) });
            setItemModal(null);
          })}
        />
      )}

      {catalogoModal && (
        <CatalogoPickerModal
          token={token!}
          onClose={() => setCatalogoModal(null)}
          onPick={(catalogoItemId, quantidade) => executar(async () => {
            await call(`/orcamentos/${id}/itens/catalogo`, {
              method: 'POST',
              body: JSON.stringify({ catalogoItemId, quantidade, ...catalogoModal }),
            });
            setCatalogoModal(null);
          })}
        />
      )}

      {cpuModal && (
        <CpuPickerModal
          token={token!}
          onClose={() => setCpuModal(null)}
          onPick={(cpuId, quantidade) => executar(async () => {
            await call(`/orcamentos/${id}/itens/cpu-propria`, {
              method: 'POST',
              body: JSON.stringify({ cpuId, quantidade, ...cpuModal }),
            });
            setCpuModal(null);
          })}
        />
      )}
    </div>
  );
};

// ---------- Modal: novo módulo ----------

const modalOverlay: React.CSSProperties = {
  position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex',
  justifyContent: 'center', alignItems: 'center', zIndex: 1000, backdropFilter: 'blur(4px)',
};

const ModuloModal: React.FC<{ onClose: () => void; onSave: (nome: string, tipo: string) => void }> = ({ onClose, onSave }) => {
  const [nome, setNome] = useState('');
  const [tipo, setTipo] = useState('OUTROS');
  return (
    <div style={modalOverlay}>
      <div className="glass-panel animate-fade-in" style={{ width: '100%', maxWidth: '460px', padding: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1.2rem' }}>
          <h2 style={{ fontSize: '1.3rem' }}>Novo Módulo</h2>
          <button onClick={onClose} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}><X size={22} /></button>
        </div>
        <form onSubmit={(e) => { e.preventDefault(); if (nome.trim()) onSave(nome.trim(), tipo); }} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '0.4rem', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>Nome *</label>
            <input style={inputStyle} required value={nome} onChange={(e) => setNome(e.target.value)} placeholder="Ex: Demolição e Retirada" autoFocus />
          </div>
          <div>
            <label style={{ display: 'block', marginBottom: '0.4rem', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>Tipo</label>
            <select style={inputStyle} value={tipo} onChange={(e) => setTipo(e.target.value)}>
              {TIPOS_MODULO.map((t) => <option key={t} value={t}>{t.replaceAll('_', ' ')}</option>)}
            </select>
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.8rem', marginTop: '0.5rem' }}>
            <button type="button" className="btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn-primary">Criar Módulo</button>
          </div>
        </form>
      </div>
    </div>
  );
};

// ---------- Modal: novo item manual ----------

interface ItemForm { codigoItem: string; descricao: string; unidade: string; quantidade: number; valorMo: number; valorMat: number; valorSrv: number }

const ItemModal: React.FC<{ onClose: () => void; onSave: (dados: ItemForm) => void }> = ({ onClose, onSave }) => {
  const [form, setForm] = useState<ItemForm>({ codigoItem: '', descricao: '', unidade: 'UN', quantidade: 1, valorMo: 0, valorMat: 0, valorSrv: 0 });
  const num = (v: string) => parseFloat(v.replace(',', '.')) || 0;
  return (
    <div style={modalOverlay}>
      <div className="glass-panel animate-fade-in" style={{ width: '100%', maxWidth: '560px', padding: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1.2rem' }}>
          <h2 style={{ fontSize: '1.3rem' }}>Novo Item da EAP</h2>
          <button onClick={onClose} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}><X size={22} /></button>
        </div>
        <form onSubmit={(e) => { e.preventDefault(); if (form.codigoItem.trim() && form.descricao.trim()) onSave(form); }} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '110px 1fr 90px', gap: '0.8rem' }}>
            <div>
              <label style={{ display: 'block', marginBottom: '0.4rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Código *</label>
              <input style={inputStyle} required value={form.codigoItem} onChange={(e) => setForm({ ...form, codigoItem: e.target.value })} placeholder="2.1" autoFocus />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: '0.4rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Descrição *</label>
              <input style={inputStyle} required value={form.descricao} onChange={(e) => setForm({ ...form, descricao: e.target.value })} placeholder="Demolição de alvenaria de vedação" />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: '0.4rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Unidade</label>
              <input style={inputStyle} value={form.unidade} onChange={(e) => setForm({ ...form, unidade: e.target.value })} placeholder="M²" />
            </div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0.8rem' }}>
            {([
              ['Quantidade', 'quantidade'],
              ['R$ M.O (unit)', 'valorMo'],
              ['R$ Mat (unit)', 'valorMat'],
              ['R$ Serv (unit)', 'valorSrv'],
            ] as const).map(([label, key]) => (
              <div key={key}>
                <label style={{ display: 'block', marginBottom: '0.4rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>{label}</label>
                <input style={inputStyle} type="number" min={0} step="0.01" value={form[key] || ''} onChange={(e) => setForm({ ...form, [key]: num(e.target.value) })} />
              </div>
            ))}
          </div>
          <div style={{ textAlign: 'right', color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Custo total: <strong style={{ color: 'var(--success)' }}>{fmtMoeda((form.valorMo + form.valorMat + form.valorSrv) * form.quantidade)}</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.8rem' }}>
            <button type="button" className="btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn-primary">Adicionar Item</button>
          </div>
        </form>
      </div>
    </div>
  );
};

// ---------- Modal: buscar nas CPUs próprias ----------

interface CpuPropriaSummary { id: string; codigo: string; descricao: string; unidade: string; isAuxiliar: boolean; valorUnitario: number }

const CpuPickerModal: React.FC<{ token: string; onClose: () => void; onPick: (cpuId: string, quantidade: number) => void }> = ({ token, onClose, onPick }) => {
  const [search, setSearch] = useState('');
  const [itens, setItens] = useState<CpuPropriaSummary[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [selecionado, setSelecionado] = useState<CpuPropriaSummary | null>(null);
  const [quantidade, setQuantidade] = useState('1');

  useEffect(() => {
    const timer = setTimeout(async () => {
      setIsLoading(true);
      try {
        const url = new URL(`${API}/cpus`);
        url.searchParams.append('size', '8');
        if (search) url.searchParams.append('search', search);
        const response = await fetch(url.toString(), { headers: { Authorization: `Bearer ${token}` } });
        if (response.ok) {
          const result = await response.json();
          setItens((result.data.content ?? []).filter((c: CpuPropriaSummary) => !c.isAuxiliar));
        }
      } finally {
        setIsLoading(false);
      }
    }, 400);
    return () => clearTimeout(timer);
  }, [search, token]);

  return (
    <div style={modalOverlay}>
      <div className="glass-panel animate-fade-in" style={{ width: '100%', maxWidth: '640px', padding: '2rem', maxHeight: '85vh', display: 'flex', flexDirection: 'column' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
          <h2 style={{ fontSize: '1.3rem' }}>Adicionar da Minha CPU</h2>
          <button onClick={onClose} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}><X size={22} /></button>
        </div>

        <div style={{ position: 'relative', marginBottom: '1rem' }}>
          <Search size={16} style={{ position: 'absolute', left: '0.7rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input style={{ ...inputStyle, paddingLeft: '2.2rem' }} placeholder="Buscar por código ou descrição..." value={search} onChange={(e) => setSearch(e.target.value)} autoFocus />
        </div>

        <div style={{ overflowY: 'auto', flex: 1, display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
          {isLoading ? (
            <div style={{ display: 'flex', justifyContent: 'center', padding: '2rem' }}><Loader2 className="animate-spin" size={22} color="var(--accent-primary)" /></div>
          ) : itens.length === 0 ? (
            <p style={{ color: 'var(--text-muted)', fontStyle: 'italic', padding: '1rem 0' }}>Nenhuma CPU encontrada. Cadastre em "CPUs & Mão de Obra".</p>
          ) : (
            itens.map((item) => (
              <div
                key={item.id}
                onClick={() => setSelecionado(item)}
                style={{
                  padding: '0.7rem 0.9rem', borderRadius: '6px', cursor: 'pointer',
                  border: `1px solid ${selecionado?.id === item.id ? 'var(--accent-primary)' : 'var(--border-color)'}`,
                  background: selecionado?.id === item.id ? 'rgba(59,130,246,0.08)' : 'var(--bg-tertiary)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: '0.8rem' }}>
                  <span style={{ fontSize: '0.88rem' }}><span style={{ color: 'var(--text-muted)' }}>{item.codigo}</span> · {item.descricao}</span>
                  <strong style={{ whiteSpace: 'nowrap' }}>{fmtMoeda(item.valorUnitario)}/{item.unidade}</strong>
                </div>
              </div>
            ))
          )}
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'flex-end', gap: '0.8rem', marginTop: '1.2rem' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '0.4rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Quantidade</label>
            <input style={{ ...inputStyle, width: '110px' }} type="number" min="0.01" step="0.01" value={quantidade} onChange={(e) => setQuantidade(e.target.value)} />
          </div>
          <button
            className="btn-primary"
            disabled={!selecionado}
            style={{ opacity: selecionado ? 1 : 0.5 }}
            onClick={() => selecionado && onPick(selecionado.id, parseFloat(quantidade.replace(',', '.')) || 1)}
          >
            Adicionar à EAP
          </button>
        </div>
      </div>
    </div>
  );
};

// ---------- Modal: buscar no catálogo SINAPI ----------

const CatalogoPickerModal: React.FC<{ token: string; onClose: () => void; onPick: (catalogoItemId: string, quantidade: number) => void }> = ({ token, onClose, onPick }) => {
  const [search, setSearch] = useState('');
  const [itens, setItens] = useState<CatalogoItemSummary[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [selecionado, setSelecionado] = useState<CatalogoItemSummary | null>(null);
  const [quantidade, setQuantidade] = useState('1');

  useEffect(() => {
    const timer = setTimeout(async () => {
      setIsLoading(true);
      try {
        const url = new URL(`${API}/catalogos/itens`);
        url.searchParams.append('size', '8');
        if (search) url.searchParams.append('search', search);
        const response = await fetch(url.toString(), { headers: { Authorization: `Bearer ${token}` } });
        if (response.ok) {
          const result = await response.json();
          setItens(result.data.content ?? []);
        }
      } finally {
        setIsLoading(false);
      }
    }, 400);
    return () => clearTimeout(timer);
  }, [search, token]);

  return (
    <div style={modalOverlay}>
      <div className="glass-panel animate-fade-in" style={{ width: '100%', maxWidth: '640px', padding: '2rem', maxHeight: '85vh', display: 'flex', flexDirection: 'column' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
          <h2 style={{ fontSize: '1.3rem' }}>Adicionar do Catálogo SINAPI</h2>
          <button onClick={onClose} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}><X size={22} /></button>
        </div>

        <div style={{ position: 'relative', marginBottom: '1rem' }}>
          <Search size={16} style={{ position: 'absolute', left: '0.7rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input
            style={{ ...inputStyle, paddingLeft: '2.2rem' }}
            placeholder="Buscar por código ou descrição..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            autoFocus
          />
        </div>

        <div style={{ overflowY: 'auto', flex: 1, display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
          {isLoading ? (
            <div style={{ display: 'flex', justifyContent: 'center', padding: '2rem' }}><Loader2 className="animate-spin" size={22} color="var(--accent-primary)" /></div>
          ) : itens.length === 0 ? (
            <p style={{ color: 'var(--text-muted)', fontStyle: 'italic', padding: '1rem 0' }}>Nenhum item encontrado.</p>
          ) : (
            itens.map((item) => (
              <div
                key={item.id}
                onClick={() => setSelecionado(item)}
                style={{
                  padding: '0.7rem 0.9rem', borderRadius: '6px', cursor: 'pointer',
                  border: `1px solid ${selecionado?.id === item.id ? 'var(--accent-primary)' : 'var(--border-color)'}`,
                  background: selecionado?.id === item.id ? 'rgba(59,130,246,0.08)' : 'var(--bg-tertiary)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: '0.8rem' }}>
                  <span style={{ fontSize: '0.88rem' }}><span style={{ color: 'var(--text-muted)' }}>{item.codigo}</span> · {item.descricao}</span>
                  <strong style={{ whiteSpace: 'nowrap' }}>{fmtMoeda(item.valorTotal)}/{item.unidade}</strong>
                </div>
              </div>
            ))
          )}
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'flex-end', gap: '0.8rem', marginTop: '1.2rem' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '0.4rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Quantidade</label>
            <input style={{ ...inputStyle, width: '110px' }} type="number" min="0.01" step="0.01" value={quantidade} onChange={(e) => setQuantidade(e.target.value)} />
          </div>
          <button
            className="btn-primary"
            disabled={!selecionado}
            style={{ opacity: selecionado ? 1 : 0.5 }}
            onClick={() => selecionado && onPick(selecionado.id, parseFloat(quantidade.replace(',', '.')) || 1)}
          >
            Adicionar à EAP
          </button>
        </div>
      </div>
    </div>
  );
};
