import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ArrowLeft, Plus, Trash2, Loader2, NotebookPen, Save, Sun, Cloud, CloudRain, CloudOff } from 'lucide-react';

// Relatório Diário de Obra — RDO (planilha de referência 006):
// clima por turno, efetivo de MO, equipamentos, atividades e ocorrências, por data.

interface LinhaQtde { rotulo: string; qtde: number }

interface Diario {
  id: string; data: string;
  climaManha: string | null; climaTarde: string | null; climaNoite: string | null;
  maoDeObra: string | null; equipamentos: string | null;
  atividades: string | null; ocorrencias: string | null; observacoes: string | null;
  createdAt: string;
}

const API = 'http://localhost:8080/api/v1';

const CLIMAS: { valor: string; rotulo: string; icone: React.ReactNode }[] = [
  { valor: 'BOM', rotulo: 'Bom', icone: <Sun size={13} /> },
  { valor: 'NUBLADO', rotulo: 'Nublado', icone: <Cloud size={13} /> },
  { valor: 'CHUVOSO', rotulo: 'Chuvoso', icone: <CloudRain size={13} /> },
  { valor: 'IMPRATICAVEL', rotulo: 'Impraticável', icone: <CloudOff size={13} /> },
];

const inputStyle: React.CSSProperties = {
  width: '100%', padding: '0.5rem 0.7rem', borderRadius: '4px',
  border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', color: 'var(--text-primary)',
};

const parseLinhas = (json: string | null): LinhaQtde[] => {
  if (!json) return [];
  try {
    const arr = JSON.parse(json);
    return Array.isArray(arr)
      ? arr.map((x: any) => ({ rotulo: String(x.funcao ?? x.descricao ?? ''), qtde: Number(x.qtde) || 0 }))
      : [];
  } catch { return []; }
};

const formVazio = () => ({
  data: new Date().toISOString().slice(0, 10),
  climaManha: 'BOM', climaTarde: 'BOM', climaNoite: '',
  maoDeObra: [] as LinhaQtde[],
  equipamentos: [] as LinhaQtde[],
  atividades: '', ocorrencias: '', observacoes: '',
});

export const OrcamentoDiario: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { token } = useAuth();

  const [diarios, setDiarios] = useState<Diario[]>([]);
  const [form, setForm] = useState(formVazio());
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');
  const [okMsg, setOkMsg] = useState('');

  const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };

  const carregar = useCallback(async () => {
    try {
      const response = await fetch(`${API}/orcamentos/${id}/diarios`, { headers });
      if (!response.ok) throw new Error('Falha ao carregar diários');
      const result = await response.json();
      setDiarios(result.data);
      setError('');
    } catch (err: any) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  }, [id, token]);

  useEffect(() => { carregar(); }, [carregar]);

  const salvar = async () => {
    setIsSaving(true);
    setError('');
    setOkMsg('');
    try {
      const response = await fetch(`${API}/orcamentos/${id}/diarios`, {
        method: 'POST', headers,
        body: JSON.stringify({
          data: form.data,
          climaManha: form.climaManha || null,
          climaTarde: form.climaTarde || null,
          climaNoite: form.climaNoite || null,
          maoDeObra: JSON.stringify(form.maoDeObra.filter((l) => l.rotulo.trim()).map((l) => ({ funcao: l.rotulo, qtde: l.qtde }))),
          equipamentos: JSON.stringify(form.equipamentos.filter((l) => l.rotulo.trim()).map((l) => ({ descricao: l.rotulo, qtde: l.qtde }))),
          atividades: form.atividades || null,
          ocorrencias: form.ocorrencias || null,
          observacoes: form.observacoes || null,
        }),
      });
      if (!response.ok) {
        const errBody = await response.json().catch(() => null);
        throw new Error(errBody?.error?.message || 'Falha ao salvar RDO');
      }
      await carregar();
      setOkMsg(`RDO de ${new Date(form.data + 'T12:00:00').toLocaleDateString('pt-BR')} salvo.`);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setIsSaving(false);
    }
  };

  const editarDiario = (diario: Diario) => {
    setForm({
      data: diario.data,
      climaManha: diario.climaManha ?? '', climaTarde: diario.climaTarde ?? '', climaNoite: diario.climaNoite ?? '',
      maoDeObra: parseLinhas(diario.maoDeObra),
      equipamentos: parseLinhas(diario.equipamentos),
      atividades: diario.atividades ?? '', ocorrencias: diario.ocorrencias ?? '', observacoes: diario.observacoes ?? '',
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const LinhasEditor: React.FC<{ titulo: string; placeholder: string; linhas: LinhaQtde[]; onChange: (l: LinhaQtde[]) => void }> = ({ titulo, placeholder, linhas, onChange }) => (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.4rem' }}>
        <label style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>{titulo}</label>
        <button type="button" onClick={() => onChange([...linhas, { rotulo: '', qtde: 1 }])} style={{ background: 'transparent', border: 'none', color: 'var(--accent-primary)', cursor: 'pointer', fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '0.2rem' }}>
          <Plus size={13} /> linha
        </button>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
        {linhas.map((l, i) => (
          <div key={i} style={{ display: 'grid', gridTemplateColumns: '1fr 70px 26px', gap: '0.4rem' }}>
            <input style={inputStyle} placeholder={placeholder} value={l.rotulo} onChange={(e) => onChange(linhas.map((x, j) => (j === i ? { ...x, rotulo: e.target.value } : x)))} />
            <input style={{ ...inputStyle, textAlign: 'right' }} type="number" min={0} value={l.qtde || ''} onChange={(e) => onChange(linhas.map((x, j) => (j === i ? { ...x, qtde: parseInt(e.target.value, 10) || 0 } : x)))} />
            <button type="button" onClick={() => onChange(linhas.filter((_, j) => j !== i))} style={{ background: 'transparent', border: 'none', color: 'var(--danger)', cursor: 'pointer' }}><Trash2 size={14} /></button>
          </div>
        ))}
        {linhas.length === 0 && <p style={{ color: 'var(--text-muted)', fontSize: '0.78rem', fontStyle: 'italic' }}>Nenhuma linha.</p>}
      </div>
    </div>
  );

  const ClimaSelect: React.FC<{ turno: string; valor: string; onChange: (v: string) => void }> = ({ turno, valor, onChange }) => (
    <div>
      <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.82rem' }}>{turno}</label>
      <select style={inputStyle} value={valor} onChange={(e) => onChange(e.target.value)}>
        <option value="">— sem expediente —</option>
        {CLIMAS.map((c) => <option key={c.valor} value={c.valor}>{c.rotulo}</option>)}
      </select>
    </div>
  );

  const climaBadge = (clima: string | null) => {
    if (!clima) return <span style={{ color: 'var(--text-muted)' }}>–</span>;
    const info = CLIMAS.find((c) => c.valor === clima);
    return <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem', color: clima === 'IMPRATICAVEL' ? 'var(--danger)' : 'var(--text-secondary)' }}>{info?.icone}{info?.rotulo}</span>;
  };

  const totalDe = (json: string | null) => parseLinhas(json).reduce((acc, l) => acc + l.qtde, 0);

  return (
    <div className="animate-fade-in" style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto' }}>
      <button onClick={() => navigate(`/orcamentos/${id}`)} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.4rem', cursor: 'pointer', marginBottom: '1rem', padding: 0, fontSize: '0.9rem' }}>
        <ArrowLeft size={16} /> Voltar à Planilha Orçamentária
      </button>

      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '1.7rem' }}>
          <NotebookPen color="var(--accent-primary)" /> Diário de Obra (RDO)
        </h1>
        <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem' }}>
          Registro diário de clima, efetivo, equipamentos, atividades e ocorrências — um RDO por data (salvar de novo na mesma data atualiza).
        </p>
      </div>

      {error && <div style={{ padding: '1rem', backgroundColor: 'rgba(239,68,68,0.1)', border: '1px solid var(--danger)', borderRadius: 'var(--border-radius-md)', color: 'var(--danger)', marginBottom: '1.5rem' }}>{error}</div>}
      {okMsg && <div style={{ padding: '1rem', backgroundColor: 'rgba(16,185,129,0.1)', border: '1px solid var(--success)', borderRadius: 'var(--border-radius-md)', color: 'var(--success)', marginBottom: '1.5rem' }}>{okMsg}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem', alignItems: 'start' }}>
        {/* Formulário do dia */}
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <h2 style={{ fontSize: '1.1rem', marginBottom: '1rem' }}>RDO do Dia</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr', gap: '0.7rem' }}>
              <div>
                <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.82rem' }}>Data *</label>
                <input style={inputStyle} type="date" value={form.data} onChange={(e) => setForm({ ...form, data: e.target.value })} />
              </div>
              <ClimaSelect turno="Manhã" valor={form.climaManha} onChange={(v) => setForm({ ...form, climaManha: v })} />
              <ClimaSelect turno="Tarde" valor={form.climaTarde} onChange={(v) => setForm({ ...form, climaTarde: v })} />
              <ClimaSelect turno="Noite" valor={form.climaNoite} onChange={(v) => setForm({ ...form, climaNoite: v })} />
            </div>

            <LinhasEditor titulo="Mão de obra (função × qtde)" placeholder="Ex: Pedreiro" linhas={form.maoDeObra} onChange={(l) => setForm({ ...form, maoDeObra: l })} />
            <LinhasEditor titulo="Equipamentos (descrição × qtde)" placeholder="Ex: Betoneira 400L" linhas={form.equipamentos} onChange={(l) => setForm({ ...form, equipamentos: l })} />

            <div>
              <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Atividades executadas</label>
              <textarea style={{ ...inputStyle, minHeight: '70px', resize: 'vertical' }} value={form.atividades} onChange={(e) => setForm({ ...form, atividades: e.target.value })} placeholder="Ex: Concretagem da laje do 2º pavimento; assentamento de alvenaria eixo B..." />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Ocorrências</label>
              <textarea style={{ ...inputStyle, minHeight: '50px', resize: 'vertical' }} value={form.ocorrencias} onChange={(e) => setForm({ ...form, ocorrencias: e.target.value })} placeholder="Ex: atraso na entrega de concreto; visita da fiscalização..." />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>Observações</label>
              <textarea style={{ ...inputStyle, minHeight: '50px', resize: 'vertical' }} value={form.observacoes} onChange={(e) => setForm({ ...form, observacoes: e.target.value })} />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.8rem' }}>
              <button className="btn-secondary" onClick={() => setForm(formVazio())}>Limpar</button>
              <button className="btn-primary" onClick={salvar} disabled={isSaving} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                {isSaving ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />} Salvar RDO
              </button>
            </div>
          </div>
        </div>

        {/* Histórico */}
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <h2 style={{ fontSize: '1.1rem', marginBottom: '1rem' }}>Histórico</h2>
          {isLoading ? (
            <div style={{ display: 'flex', justifyContent: 'center', padding: '2rem' }}><Loader2 className="animate-spin" size={24} color="var(--accent-primary)" /></div>
          ) : diarios.length === 0 ? (
            <p style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>Nenhum RDO registrado ainda.</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem', maxHeight: '70vh', overflowY: 'auto' }}>
              {diarios.map((diario) => (
                <div key={diario.id} onClick={() => editarDiario(diario)} style={{ padding: '0.8rem 1rem', borderRadius: '6px', border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', cursor: 'pointer' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.4rem' }}>
                    <strong>{new Date(diario.data + 'T12:00:00').toLocaleDateString('pt-BR', { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' })}</strong>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        if (window.confirm('Excluir este RDO?')) {
                          fetch(`${API}/orcamentos/${id}/diarios/${diario.id}`, { method: 'DELETE', headers }).then(carregar);
                        }
                      }}
                      style={{ background: 'transparent', border: 'none', color: 'var(--danger)', cursor: 'pointer', padding: '0.2rem' }}
                    ><Trash2 size={14} /></button>
                  </div>
                  <div style={{ display: 'flex', gap: '1rem', fontSize: '0.8rem', flexWrap: 'wrap' }}>
                    <span>Manhã: {climaBadge(diario.climaManha)}</span>
                    <span>Tarde: {climaBadge(diario.climaTarde)}</span>
                    <span>Noite: {climaBadge(diario.climaNoite)}</span>
                  </div>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '0.3rem' }}>
                    Efetivo: {totalDe(diario.maoDeObra)} pessoa(s) · Equipamentos: {totalDe(diario.equipamentos)}
                  </div>
                  {diario.atividades && <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.3rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{diario.atividades}</div>}
                  {diario.ocorrencias && <div style={{ fontSize: '0.8rem', color: 'var(--warning)', marginTop: '0.2rem', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>⚠ {diario.ocorrencias}</div>}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
