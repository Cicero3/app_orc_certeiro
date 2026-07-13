import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { Plus, Trash2, Loader2, X, Pencil, HardHat, Layers, Search } from 'lucide-react';

// ---------- Tipos ----------

interface FuncaoSalarial { id: string; nome: string; valorHora: number }

interface CpuSummary { id: string; codigo: string; descricao: string; unidade: string; isAuxiliar: boolean; valorUnitario: number }

interface CpuInsumoDetail {
  id: string; tipoInsumo: string; descricao: string; unidade: string; coeficiente: number;
  custoUnitarioEfetivo: number; custoTotal: number;
  funcaoSalarialId: string | null; funcaoSalarialNome: string | null;
  cpuReferenciaId: string | null; cpuReferenciaCodigo: string | null;
}

interface CpuDetail {
  id: string; codigo: string; descricao: string; unidade: string; isAuxiliar: boolean;
  valorMo: number; valorMat: number; valorSrv: number; valorUnitario: number;
  insumos: CpuInsumoDetail[];
}

interface InsumoForm {
  key: string; tipoInsumo: string; descricao: string; unidade: string;
  coeficiente: string; custoUnitario: string; funcaoSalarialId: string; cpuReferenciaId: string;
}

const API = 'http://localhost:8080/api/v1';
const TIPOS = ['MAO_DE_OBRA', 'MATERIAL', 'EQUIPAMENTO', 'SERVICO'];

const fmtMoeda = (v: number) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v);
const num = (v: string) => parseFloat(v.replace(',', '.')) || 0;

const inputStyle: React.CSSProperties = {
  width: '100%', padding: '0.5rem 0.7rem', borderRadius: '4px',
  border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', color: 'var(--text-primary)',
};

const novoInsumo = (): InsumoForm => ({
  key: crypto.randomUUID(), tipoInsumo: 'MATERIAL', descricao: '', unidade: 'UN',
  coeficiente: '1', custoUnitario: '0', funcaoSalarialId: '', cpuReferenciaId: '',
});

// ---------- Página ----------

export const CpusPage: React.FC = () => {
  const { token } = useAuth();
  const [funcoes, setFuncoes] = useState<FuncaoSalarial[]>([]);
  const [cpus, setCpus] = useState<CpuSummary[]>([]);
  const [search, setSearch] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  // Funções salariais — edição inline
  const [novaFuncao, setNovaFuncao] = useState({ nome: '', valorHora: '' });
  const [editFuncao, setEditFuncao] = useState<{ id: string; nome: string; valorHora: string } | null>(null);

  // CPU — modal de criação/edição
  const [cpuModal, setCpuModal] = useState<CpuDetail | 'nova' | null>(null);

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

  const carregar = useCallback(async (termo = '') => {
    try {
      const [funcoesRes, cpusRes] = await Promise.all([
        call('/funcoes-salariais'),
        call(`/cpus?size=100${termo ? `&search=${encodeURIComponent(termo)}` : ''}`),
      ]);
      setFuncoes(funcoesRes.data);
      setCpus(cpusRes.data.content ?? []);
      setError('');
    } catch (err: any) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  }, [call]);

  useEffect(() => {
    const timer = setTimeout(() => carregar(search), 400);
    return () => clearTimeout(timer);
  }, [search, carregar]);

  const executar = async (fn: () => Promise<any>) => {
    try { await fn(); await carregar(search); setError(''); }
    catch (err: any) { setError(err.message); }
  };

  return (
    <div className="animate-fade-in" style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '1.8rem' }}>
          <Layers color="var(--accent-primary)" /> CPUs & Mão de Obra
        </h1>
        <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem' }}>
          Suas composições de preço unitário reutilizáveis e a tabela de salários-hora por função.
        </p>
      </div>

      {error && (
        <div style={{ padding: '1rem', backgroundColor: 'rgba(239, 68, 68, 0.1)', border: '1px solid var(--danger)', borderRadius: 'var(--border-radius-md)', color: 'var(--danger)', marginBottom: '1.5rem' }}>
          {error}
        </div>
      )}

      {isLoading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '3rem' }}><Loader2 className="animate-spin" size={32} color="var(--accent-primary)" /></div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: '340px 1fr', gap: '2rem', alignItems: 'start' }}>
          {/* Tabela salarial */}
          <div className="glass-panel" style={{ padding: '1.5rem' }}>
            <h2 style={{ fontSize: '1.15rem', display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
              <HardHat size={18} color="var(--accent-primary)" /> Salários por Função (R$/h)
            </h2>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              {funcoes.map((f) => (
                <div key={f.id} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.4rem 0', borderBottom: '1px solid var(--border-color)' }}>
                  {editFuncao?.id === f.id ? (
                    <>
                      <input style={{ ...inputStyle, flex: 1 }} value={editFuncao.nome} onChange={(e) => setEditFuncao({ ...editFuncao, nome: e.target.value })} />
                      <input style={{ ...inputStyle, width: '90px', textAlign: 'right' }} value={editFuncao.valorHora} onChange={(e) => setEditFuncao({ ...editFuncao, valorHora: e.target.value })} />
                      <button
                        onClick={() => executar(async () => {
                          await call(`/funcoes-salariais/${f.id}`, { method: 'PUT', body: JSON.stringify({ nome: editFuncao.nome, valorHora: num(editFuncao.valorHora) }) });
                          setEditFuncao(null);
                        })}
                        className="btn-secondary" style={{ padding: '0.3rem 0.6rem', fontSize: '0.8rem' }}
                      >OK</button>
                    </>
                  ) : (
                    <>
                      <span style={{ flex: 1 }}>{f.nome}</span>
                      <strong style={{ fontVariantNumeric: 'tabular-nums' }}>{fmtMoeda(f.valorHora)}</strong>
                      <button onClick={() => setEditFuncao({ id: f.id, nome: f.nome, valorHora: String(f.valorHora) })} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: '0.2rem' }}><Pencil size={13} /></button>
                      <button
                        onClick={() => { if (window.confirm(`Excluir a função "${f.nome}"?`)) executar(() => call(`/funcoes-salariais/${f.id}`, { method: 'DELETE' })); }}
                        style={{ background: 'transparent', border: 'none', color: 'var(--danger)', cursor: 'pointer', padding: '0.2rem' }}
                      ><Trash2 size={13} /></button>
                    </>
                  )}
                </div>
              ))}
              {funcoes.length === 0 && <p style={{ color: 'var(--text-muted)', fontStyle: 'italic', fontSize: '0.9rem' }}>Cadastre funções como Pedreiro, Servente, Encanador...</p>}
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                if (!novaFuncao.nome.trim()) return;
                executar(async () => {
                  await call('/funcoes-salariais', { method: 'POST', body: JSON.stringify({ nome: novaFuncao.nome.trim(), valorHora: num(novaFuncao.valorHora) }) });
                  setNovaFuncao({ nome: '', valorHora: '' });
                });
              }}
              style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}
            >
              <input style={{ ...inputStyle, flex: 1 }} placeholder="Função (ex: Pedreiro)" value={novaFuncao.nome} onChange={(e) => setNovaFuncao({ ...novaFuncao, nome: e.target.value })} />
              <input style={{ ...inputStyle, width: '90px' }} placeholder="R$/h" value={novaFuncao.valorHora} onChange={(e) => setNovaFuncao({ ...novaFuncao, valorHora: e.target.value })} />
              <button type="submit" className="btn-primary" style={{ padding: '0.4rem 0.7rem' }}><Plus size={16} /></button>
            </form>
          </div>

          {/* CPUs próprias */}
          <div className="glass-panel" style={{ padding: '1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', gap: '1rem', flexWrap: 'wrap' }}>
              <h2 style={{ fontSize: '1.15rem' }}>Minhas Composições (CPU)</h2>
              <button className="btn-primary" onClick={() => setCpuModal('nova')} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Plus size={16} /> Nova CPU
              </button>
            </div>

            <div style={{ position: 'relative', marginBottom: '1rem' }}>
              <Search size={15} style={{ position: 'absolute', left: '0.7rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
              <input style={{ ...inputStyle, paddingLeft: '2.1rem' }} placeholder="Buscar por código ou descrição..." value={search} onChange={(e) => setSearch(e.target.value)} />
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              {cpus.map((cpu) => (
                <div
                  key={cpu.id}
                  onClick={() => executar(async () => {
                    const detail = await call(`/cpus/${cpu.id}`);
                    setCpuModal(detail.data);
                  })}
                  style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '0.8rem', padding: '0.7rem 0.9rem', borderRadius: '6px', border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', cursor: 'pointer' }}
                >
                  <div>
                    <span style={{ color: 'var(--text-muted)', marginRight: '0.5rem' }}>{cpu.codigo}</span>
                    {cpu.descricao}
                    {cpu.isAuxiliar && <span style={{ marginLeft: '0.5rem', fontSize: '0.7rem', padding: '0.15rem 0.5rem', borderRadius: '1rem', background: 'rgba(59,130,246,0.15)', color: 'var(--accent-primary)' }}>AUXILIAR</span>}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.8rem', whiteSpace: 'nowrap' }}>
                    <strong>{fmtMoeda(cpu.valorUnitario)}/{cpu.unidade}</strong>
                    <button
                      onClick={(e) => { e.stopPropagation(); if (window.confirm(`Excluir a CPU "${cpu.codigo}"?`)) executar(() => call(`/cpus/${cpu.id}`, { method: 'DELETE' })); }}
                      style={{ background: 'transparent', border: 'none', color: 'var(--danger)', cursor: 'pointer', padding: '0.2rem' }}
                    ><Trash2 size={14} /></button>
                  </div>
                </div>
              ))}
              {cpus.length === 0 && (
                <p style={{ color: 'var(--text-muted)', fontStyle: 'italic', padding: '1rem 0' }}>
                  Nenhuma CPU cadastrada. Crie composições próprias (ex.: "Assentamento de alvenaria") ou auxiliares (ex.: "Argamassa 1:2:8").
                </p>
              )}
            </div>
          </div>
        </div>
      )}

      {cpuModal && (
        <CpuEditorModal
          cpu={cpuModal === 'nova' ? null : cpuModal}
          funcoes={funcoes}
          auxiliares={cpus.filter((c) => c.isAuxiliar && (cpuModal === 'nova' || c.id !== cpuModal.id))}
          onClose={() => setCpuModal(null)}
          onSave={(payload, id) => executar(async () => {
            if (id) await call(`/cpus/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
            else await call('/cpus', { method: 'POST', body: JSON.stringify(payload) });
            setCpuModal(null);
          })}
        />
      )}
    </div>
  );
};

// ---------- Modal de edição de CPU ----------

const modalOverlay: React.CSSProperties = {
  position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex',
  justifyContent: 'center', alignItems: 'center', zIndex: 1000, backdropFilter: 'blur(4px)',
};

interface CpuEditorProps {
  cpu: CpuDetail | null;
  funcoes: FuncaoSalarial[];
  auxiliares: CpuSummary[];
  onClose: () => void;
  onSave: (payload: any, id?: string) => void;
}

const CpuEditorModal: React.FC<CpuEditorProps> = ({ cpu, funcoes, auxiliares, onClose, onSave }) => {
  const [codigo, setCodigo] = useState(cpu?.codigo ?? '');
  const [descricao, setDescricao] = useState(cpu?.descricao ?? '');
  const [unidade, setUnidade] = useState(cpu?.unidade ?? 'M2');
  const [isAuxiliar, setIsAuxiliar] = useState(cpu?.isAuxiliar ?? false);
  const [insumos, setInsumos] = useState<InsumoForm[]>(
    cpu?.insumos.map((i) => ({
      key: i.id, tipoInsumo: i.tipoInsumo, descricao: i.descricao, unidade: i.unidade,
      coeficiente: String(i.coeficiente), custoUnitario: String(i.custoUnitarioEfetivo),
      funcaoSalarialId: i.funcaoSalarialId ?? '', cpuReferenciaId: i.cpuReferenciaId ?? '',
    })) ?? []
  );

  const update = (key: string, patch: Partial<InsumoForm>) =>
    setInsumos(insumos.map((i) => (i.key === key ? { ...i, ...patch } : i)));

  const resolverCusto = (i: InsumoForm): number => {
    if (i.cpuReferenciaId) return auxiliares.find((a) => a.id === i.cpuReferenciaId)?.valorUnitario ?? 0;
    if (i.funcaoSalarialId) return funcoes.find((f) => f.id === i.funcaoSalarialId)?.valorHora ?? 0;
    return num(i.custoUnitario);
  };

  const total = insumos.reduce((acc, i) => acc + num(i.coeficiente) * resolverCusto(i), 0);

  const salvar = () => {
    if (!codigo.trim() || !descricao.trim() || !unidade.trim()) return;
    onSave({
      codigo: codigo.trim(),
      descricao: descricao.trim(),
      unidade: unidade.trim(),
      isAuxiliar,
      insumos: insumos.map((i) => ({
        tipoInsumo: i.tipoInsumo,
        descricao: i.descricao,
        unidade: i.unidade,
        coeficiente: num(i.coeficiente),
        custoUnitario: num(i.custoUnitario),
        funcaoSalarialId: i.funcaoSalarialId || null,
        cpuReferenciaId: i.cpuReferenciaId || null,
      })),
    }, cpu?.id);
  };

  return (
    <div style={modalOverlay}>
      <div className="glass-panel animate-fade-in" style={{ width: '100%', maxWidth: '860px', padding: '2rem', maxHeight: '90vh', overflowY: 'auto' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1.2rem' }}>
          <h2 style={{ fontSize: '1.3rem' }}>{cpu ? `Editar CPU ${cpu.codigo}` : 'Nova Composição de Preço Unitário'}</h2>
          <button onClick={onClose} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}><X size={22} /></button>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '120px 1fr 90px 130px', gap: '0.8rem', marginBottom: '1.2rem' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Código *</label>
            <input style={inputStyle} value={codigo} onChange={(e) => setCodigo(e.target.value)} placeholder="3.1" />
          </div>
          <div>
            <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Descrição *</label>
            <input style={inputStyle} value={descricao} onChange={(e) => setDescricao(e.target.value)} placeholder="Assentamento de alvenaria de vedação" />
          </div>
          <div>
            <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Unidade *</label>
            <input style={inputStyle} value={unidade} onChange={(e) => setUnidade(e.target.value)} placeholder="M2" />
          </div>
          <div style={{ display: 'flex', alignItems: 'flex-end', paddingBottom: '0.3rem' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-secondary)', fontSize: '0.85rem', cursor: cpu ? 'not-allowed' : 'pointer' }}>
              <input type="checkbox" checked={isAuxiliar} disabled={!!cpu} onChange={(e) => setIsAuxiliar(e.target.checked)} />
              Auxiliar
            </label>
          </div>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.6rem' }}>
          <h3 style={{ fontSize: '1rem' }}>Insumos</h3>
          <button className="btn-secondary" onClick={() => setInsumos([...insumos, novoInsumo()])} style={{ padding: '0.35rem 0.7rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
            <Plus size={14} /> Insumo
          </button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.7rem' }}>
          {insumos.map((i) => {
            const custoEfetivo = resolverCusto(i);
            const custoBloqueado = !!i.funcaoSalarialId || !!i.cpuReferenciaId;
            return (
              <div key={i.key} style={{ display: 'grid', gridTemplateColumns: '130px 1.6fr 70px 90px 110px 150px 110px 30px', gap: '0.5rem', alignItems: 'end' }}>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.2rem', color: 'var(--text-muted)', fontSize: '0.72rem' }}>Tipo</label>
                  <select style={inputStyle} value={i.tipoInsumo} onChange={(e) => update(i.key, { tipoInsumo: e.target.value, funcaoSalarialId: '', cpuReferenciaId: '' })}>
                    {TIPOS.map((t) => <option key={t} value={t}>{t.replaceAll('_', ' ')}</option>)}
                  </select>
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.2rem', color: 'var(--text-muted)', fontSize: '0.72rem' }}>Descrição</label>
                  <input style={inputStyle} value={i.descricao} onChange={(e) => update(i.key, { descricao: e.target.value })} />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.2rem', color: 'var(--text-muted)', fontSize: '0.72rem' }}>Und</label>
                  <input style={inputStyle} value={i.unidade} onChange={(e) => update(i.key, { unidade: e.target.value })} />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.2rem', color: 'var(--text-muted)', fontSize: '0.72rem' }}>Coefic.</label>
                  <input style={{ ...inputStyle, textAlign: 'right' }} value={i.coeficiente} onChange={(e) => update(i.key, { coeficiente: e.target.value })} />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.2rem', color: 'var(--text-muted)', fontSize: '0.72rem' }}>R$ Unit</label>
                  <input style={{ ...inputStyle, textAlign: 'right', opacity: custoBloqueado ? 0.5 : 1 }} disabled={custoBloqueado} value={custoBloqueado ? custoEfetivo.toFixed(2) : i.custoUnitario} onChange={(e) => update(i.key, { custoUnitario: e.target.value })} />
                </div>
                <div>
                  <label style={{ display: 'block', marginBottom: '0.2rem', color: 'var(--text-muted)', fontSize: '0.72rem' }}>
                    {i.tipoInsumo === 'MAO_DE_OBRA' ? 'Função (tabela)' : 'CPU auxiliar'}
                  </label>
                  {i.tipoInsumo === 'MAO_DE_OBRA' ? (
                    <select style={inputStyle} value={i.funcaoSalarialId} onChange={(e) => update(i.key, { funcaoSalarialId: e.target.value, cpuReferenciaId: '' })}>
                      <option value="">— manual —</option>
                      {funcoes.map((f) => <option key={f.id} value={f.id}>{f.nome} ({fmtMoeda(f.valorHora)}/h)</option>)}
                    </select>
                  ) : (
                    <select style={inputStyle} value={i.cpuReferenciaId} disabled={isAuxiliar} onChange={(e) => update(i.key, { cpuReferenciaId: e.target.value, funcaoSalarialId: '' })}>
                      <option value="">— manual —</option>
                      {auxiliares.map((a) => <option key={a.id} value={a.id}>{a.codigo} · {a.descricao}</option>)}
                    </select>
                  )}
                </div>
                <div style={{ textAlign: 'right', fontSize: '0.88rem', fontWeight: 600, color: 'var(--success)', paddingBottom: '0.5rem', fontVariantNumeric: 'tabular-nums' }}>
                  {fmtMoeda(num(i.coeficiente) * custoEfetivo)}
                </div>
                <button onClick={() => setInsumos(insumos.filter((x) => x.key !== i.key))} style={{ background: 'transparent', border: 'none', color: 'var(--danger)', cursor: 'pointer', paddingBottom: '0.5rem' }}>
                  <Trash2 size={15} />
                </button>
              </div>
            );
          })}
          {insumos.length === 0 && <p style={{ color: 'var(--text-muted)', fontStyle: 'italic', fontSize: '0.9rem' }}>Adicione insumos de MO, material, equipamento ou serviço.</p>}
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1.5rem', paddingTop: '1rem', borderTop: '1px solid var(--border-color)' }}>
          <div style={{ fontSize: '1rem' }}>
            Valor unitário: <strong style={{ color: 'var(--success)', fontSize: '1.2rem' }}>{fmtMoeda(total)}</strong> / {unidade || 'un'}
          </div>
          <div style={{ display: 'flex', gap: '0.8rem' }}>
            <button className="btn-secondary" onClick={onClose}>Cancelar</button>
            <button className="btn-primary" onClick={salvar}>{cpu ? 'Salvar Alterações' : 'Criar CPU'}</button>
          </div>
        </div>
      </div>
    </div>
  );
};
