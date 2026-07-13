import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { FolderGit2, Plus, Loader2, X, Clock, CheckCircle, XCircle, ArrowLeft, Trash2 } from 'lucide-react';
interface Orcamento {
  id: string;
  titulo: string;
  status: string;
  valorTotal: number;
  createdAt: string;
  updatedAt: string;
}

export const OrcamentosList: React.FC = () => {
  const [orcamentos, setOrcamentos] = useState<Orcamento[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [novoTitulo, setNovoTitulo] = useState('');
  const [novoBdi, setNovoBdi] = useState('25.0');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  const { token } = useAuth();
  const navigate = useNavigate();

  const fetchOrcamentos = async () => {
    setIsLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/v1/orcamentos', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (!response.ok) throw new Error('Falha ao buscar orçamentos');
      const data = await response.json();
      setOrcamentos(data.data);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchOrcamentos();
  }, [token]);

  const handleCriarOrcamento = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError('');

    try {
      const bdiDecimal = parseFloat(novoBdi) / 100;
      const response = await fetch('http://localhost:8080/api/v1/orcamentos', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          titulo: novoTitulo,
          bdi: bdiDecimal
        })
      });

      if (!response.ok) {
        const errData = await response.json();
        throw new Error(errData.error?.message || 'Falha ao criar orçamento');
      }

      await fetchOrcamentos();
      setIsModalOpen(false);
      setNovoTitulo('');
      setNovoBdi('25.0');
    } catch (err: any) {
      setError(err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteOrcamento = async (e: React.MouseEvent, id: string) => {
    e.stopPropagation();
    if (!window.confirm('Tem certeza que deseja excluir este orçamento?')) return;
    
    try {
      const response = await fetch(`http://localhost:8080/api/v1/orcamentos/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (!response.ok) throw new Error('Falha ao excluir orçamento');
      await fetchOrcamentos();
    } catch (err: any) {
      setError(err.message);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'RASCUNHO':
        return <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.3rem', padding: '0.3rem 0.6rem', backgroundColor: 'var(--bg-secondary)', color: 'var(--text-secondary)', borderRadius: '1rem', fontSize: '0.8rem', fontWeight: '500' }}><Clock size={14} /> Rascunho</span>;
      case 'EM_REVISAO':
        return <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.3rem', padding: '0.3rem 0.6rem', backgroundColor: 'rgba(234, 179, 8, 0.1)', color: 'var(--warning)', borderRadius: '1rem', fontSize: '0.8rem', fontWeight: '500' }}><Loader2 size={14} className="animate-spin" /> Em Revisão</span>;
      case 'APROVADO':
      case 'ACEITO':
        return <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.3rem', padding: '0.3rem 0.6rem', backgroundColor: 'rgba(34, 197, 94, 0.1)', color: 'var(--success)', borderRadius: '1rem', fontSize: '0.8rem', fontWeight: '500' }}><CheckCircle size={14} /> {status}</span>;
      default:
        return <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.3rem', padding: '0.3rem 0.6rem', backgroundColor: 'rgba(239, 68, 68, 0.1)', color: 'var(--danger)', borderRadius: '1rem', fontSize: '0.8rem', fontWeight: '500' }}><XCircle size={14} /> {status}</span>;
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
  };

  const formatDate = (isoString: string) => {
    return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(isoString));
  };

  return (
    <div className="animate-fade-in" style={{ padding: '2rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <div>
          <button 
            onClick={() => navigate('/')} 
            className="hover:text-white"
            style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.4rem', cursor: 'pointer', marginBottom: '1rem', padding: 0, fontSize: '0.9rem', transition: 'color 0.2s' }}
          >
            <ArrowLeft size={16} /> Voltar ao Dashboard
          </button>
          <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '1.8rem' }}>
            <FolderGit2 color="var(--accent-primary)" /> Meus Orçamentos
          </h1>
          <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem' }}>Gerencie todos os seus projetos e propostas</p>
        </div>
        <button className="btn-primary" onClick={() => setIsModalOpen(true)} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Plus size={18} /> Novo Orçamento
        </button>
      </div>

      {error && !isModalOpen && (
        <div style={{ padding: '1rem', backgroundColor: 'rgba(239, 68, 68, 0.1)', border: '1px solid var(--danger)', borderRadius: 'var(--border-radius-md)', color: 'var(--danger)', marginBottom: '1.5rem' }}>
          {error}
        </div>
      )}

      {isLoading ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '3rem' }}>
          <Loader2 className="animate-spin" size={32} color="var(--accent-primary)" />
        </div>
      ) : orcamentos.length === 0 ? (
        <div className="glass-panel" style={{ padding: '4rem', textAlign: 'center', color: 'var(--text-muted)' }}>
          <FolderGit2 size={48} style={{ margin: '0 auto', opacity: 0.5, marginBottom: '1rem' }} />
          <h3>Nenhum orçamento encontrado</h3>
          <p style={{ marginTop: '0.5rem', marginBottom: '1.5rem' }}>Você ainda não criou nenhum orçamento.</p>
          <button className="btn-primary" onClick={() => setIsModalOpen(true)}>Começar meu primeiro orçamento</button>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.5rem' }}>
          {orcamentos.map((orcamento) => (
            <div key={orcamento.id} className="glass-panel card-hover" onClick={() => navigate(`/orcamentos/${orcamento.id}`)} style={{ padding: '1.5rem', cursor: 'pointer', transition: 'all 0.3s' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
                <h3 style={{ fontSize: '1.2rem', color: 'var(--text-primary)', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                  {orcamento.titulo}
                </h3>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.8rem' }}>
                  {getStatusBadge(orcamento.status)}
                  <button 
                    onClick={(e) => handleDeleteOrcamento(e, orcamento.id)}
                    style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: '0.2rem' }}
                    title="Excluir orçamento"
                    onMouseOver={(e) => e.currentTarget.style.color = 'var(--danger)'}
                    onMouseOut={(e) => e.currentTarget.style.color = 'var(--text-muted)'}
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
              
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginBottom: '1.5rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                  <span>Criado em:</span>
                  <span>{formatDate(orcamento.createdAt)}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                  <span>Atualizado:</span>
                  <span>{formatDate(orcamento.updatedAt)}</span>
                </div>
              </div>

              <div style={{ paddingTop: '1rem', borderTop: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
                <span style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Valor Total</span>
                <strong style={{ fontSize: '1.2rem', color: 'var(--text-primary)' }}>{formatCurrency(orcamento.valorTotal)}</strong>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal Novo Orçamento */}
      {isModalOpen && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 1000, backdropFilter: 'blur(4px)' }}>
          <div className="glass-panel animate-fade-in" style={{ width: '100%', maxWidth: '500px', padding: '2rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <h2 style={{ fontSize: '1.4rem' }}>Criar Novo Orçamento</h2>
              <button onClick={() => setIsModalOpen(false)} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}>
                <X size={24} />
              </button>
            </div>

            {error && isModalOpen && (
              <div style={{ padding: '1rem', backgroundColor: 'rgba(239, 68, 68, 0.1)', border: '1px solid var(--danger)', borderRadius: 'var(--border-radius-md)', color: 'var(--danger)', marginBottom: '1.5rem', fontSize: '0.9rem' }}>
                {error}
              </div>
            )}

            <form onSubmit={handleCriarOrcamento} style={{ display: 'flex', flexDirection: 'column', gap: '1.2rem' }}>
              <div>
                <label style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>Título do Projeto *</label>
                <input
                  type="text"
                  required
                  value={novoTitulo}
                  onChange={(e) => setNovoTitulo(e.target.value)}
                  placeholder="Ex: Reforma da Escola Municipal..."
                  style={{ width: '100%', padding: '0.8rem 1rem', borderRadius: 'var(--border-radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'var(--bg-tertiary)', color: 'var(--text-primary)', outline: 'none' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>BDI (%)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  value={novoBdi}
                  onChange={(e) => setNovoBdi(e.target.value)}
                  style={{ width: '100%', padding: '0.8rem 1rem', borderRadius: 'var(--border-radius-md)', border: '1px solid var(--border-color)', backgroundColor: 'var(--bg-tertiary)', color: 'var(--text-primary)', outline: 'none' }}
                />
                <small style={{ color: 'var(--text-muted)', display: 'block', marginTop: '0.3rem' }}>BDI inicial sugerido de 25%</small>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1rem' }}>
                <button type="button" onClick={() => setIsModalOpen(false)} className="btn-secondary">Cancelar</button>
                <button type="submit" disabled={isSubmitting} className="btn-primary" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  {isSubmitting ? <Loader2 size={18} className="animate-spin" /> : 'Salvar Orçamento'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      <style>{`
        .card-hover:hover {
          transform: translateY(-4px);
          border-color: rgba(255, 255, 255, 0.15);
          box-shadow: 0 10px 20px rgba(0, 0, 0, 0.2);
        }
      `}</style>
    </div>
  );
};
