import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ArrowLeft, Plus, Trash2, Loader2, CircleDollarSign, Check } from 'lucide-react';

// ---------- Tipos ----------

interface CustoIndireto { id: string; categoria: string; descricao: string; quantidade: number; valorUnitario: number; total: number }
interface CustosResumo { itens: CustoIndireto[]; totalPorCategoria: Record<string, number>; total: number }

interface FormacaoPreco {
  admCentral: number; custoFinanceiro: number; contingencia: number; comissao: number; lucro: number;
  cofins: number; pis: number; icms: number; iss: number; irpj: number; csll: number;
  custoDireto: number; custoIndireto: number; baseCdCi: number;
  admCentralValor: number; custoFinanceiroValor: number; contingenciaValor: number; custoTotal: number;
  aliquotaTributos: number; tributosValor: number; comissaoValor: number; lucroValor: number;
  precoVenda: number; bdiSobreCustoTotal: number; bdiSobreCustoDireto: number; bdiAtualDoOrcamento: number;
}

const API = 'http://localhost:8080/api/v1';

const CATEGORIAS: { valor: string; label: string }[] = [
  { valor: 'EQUIPE_TECNICA', label: 'Equipe Técnica' },
  { valor: 'EQUIPE_SUPORTE', label: 'Equipe de Suporte' },
  { valor: 'EQUIPE_ADMINISTRATIVA', label: 'Equipe Administrativa' },
  { valor: 'MOBILIZACAO', label: 'Mobilização e Desmobilização' },
  { valor: 'EQP_CANTEIRO', label: 'Equipamentos do Canteiro' },
  { valor: 'EQP_ADMINISTRATIVO', label: 'Equipamentos Administrativos' },
  { valor: 'PROTECAO_COLETIVA', label: 'Proteção Coletiva' },
  { valor: 'EPI', label: 'EPI' },
  { valor: 'FERRAMENTAS', label: 'Ferramentas' },
  { valor: 'DESPESAS_CORRENTES', label: 'Despesas Correntes' },
  { valor: 'DESPESAS_PESSOAL', label: 'Despesas com Pessoal' },
  { valor: 'SERVICOS_TERCEIROS', label: 'Serviços de Terceiros' },
  { valor: 'TAXAS', label: 'Taxas e Emolumentos' },
  { valor: 'DIVERSOS', label: 'Diversos' },
];

const labelDe = (categoria: string) => CATEGORIAS.find((c) => c.valor === categoria)?.label ?? categoria;

const fmtMoeda = (v: number) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(v);
const fmtPct = (v: number) => `${(v * 100).toLocaleString('pt-BR', { maximumFractionDigits: 2 })}%`;
const num = (v: string) => parseFloat(v.replace(',', '.')) || 0;

const inputStyle: React.CSSProperties = {
  width: '100%', padding: '0.5rem 0.7rem', borderRadius: '4px',
  border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', color: 'var(--text-primary)',
};

// Campos de % da formação de preço (chave da API + rótulo)
const PARAMS_CUSTO: [keyof FormacaoPreco, string][] = [
  ['admCentral', 'ADM Central (%)'], ['custoFinanceiro', 'Custo Financeiro (%)'], ['contingencia', 'Imprevistos/Contingência (%)'],
];
const PARAMS_PRECO: [keyof FormacaoPreco, string][] = [
  ['lucro', 'Lucro (%)'], ['comissao', 'Comissão (%)'],
  ['cofins', 'COFINS (%)'], ['pis', 'PIS (%)'], ['icms', 'ICMS (%)'], ['iss', 'ISS (%)'], ['irpj', 'IRPJ (%)'], ['csll', 'CSLL (%)'],
];

export const OrcamentoPrecificacao: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { token } = useAuth();

  const [custos, setCustos] = useState<CustosResumo | null>(null);
  const [formacao, setFormacao] = useState<FormacaoPreco | null>(null);
  const [paramsEdit, setParamsEdit] = useState<Record<string, string>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [okMsg, setOkMsg] = useState('');

  const [novoCusto, setNovoCusto] = useState({ categoria: 'EQUIPE_TECNICA', descricao: '', quantidade: '1', valorUnitario: '' });

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

  const carregar = useCallback(async () => {
    try {
      const [custosRes, formacaoRes] = await Promise.all([
        call(`/orcamentos/${id}/custos-indiretos`),
        call(`/orcamentos/${id}/formacao-preco`),
      ]);
      setCustos(custosRes.data);
      const f: FormacaoPreco = formacaoRes.data;
      setFormacao(f);
      const edit: Record<string, string> = {};
      [...PARAMS_CUSTO, ...PARAMS_PRECO].forEach(([key]) => { edit[key as string] = String((f[key] as number) * 100); });
      setParamsEdit(edit);
      setError('');
    } catch (err: any) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  }, [id, call]);

  useEffect(() => { carregar(); }, [carregar]);

  const executar = async (fn: () => Promise<any>, sucesso = '') => {
    try { await fn(); await carregar(); setError(''); setOkMsg(sucesso); }
    catch (err: any) { setError(err.message); setOkMsg(''); }
  };

  const salvarParametros = () => executar(async () => {
    const payload: Record<string, number> = {};
    [...PARAMS_CUSTO, ...PARAMS_PRECO].forEach(([key]) => { payload[key as string] = num(paramsEdit[key as string]) / 100; });
    await call(`/orcamentos/${id}/formacao-preco`, { method: 'PUT', body: JSON.stringify(payload) });
  }, 'Parâmetros salvos e preço recalculado.');

  const aplicarBdi = () => executar(
    () => call(`/orcamentos/${id}/formacao-preco/aplicar-bdi`, { method: 'POST' }),
    'BDI aplicado ao orçamento — a planilha orçamentária agora fecha no preço de venda.'
  );

  if (isLoading) {
    return <div style={{ display: 'flex', justifyContent: 'center', padding: '4rem' }}><Loader2 className="animate-spin" size={32} color="var(--accent-primary)" /></div>;
  }

  const linha = (label: string, valor: string, cor?: string, destaque = false) => (
    <div style={{ display: 'flex', justifyContent: 'space-between', color: cor ?? 'var(--text-secondary)', ...(destaque ? { paddingTop: '0.8rem', borderTop: '1px solid rgba(255,255,255,0.1)', marginTop: '0.4rem' } : {}) }}>
      <span style={destaque ? { fontWeight: 500, color: 'white' } : undefined}>{label}</span>
      <span style={destaque ? { fontWeight: 700, fontSize: '1.15rem', color: cor ?? 'var(--success)' } : { fontVariantNumeric: 'tabular-nums' }}>{valor}</span>
    </div>
  );

  return (
    <div className="animate-fade-in" style={{ padding: '2rem', maxWidth: '1300px', margin: '0 auto' }}>
      <button onClick={() => navigate(`/orcamentos/${id}`)} style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.4rem', cursor: 'pointer', marginBottom: '1rem', padding: 0, fontSize: '0.9rem' }}>
        <ArrowLeft size={16} /> Voltar à Planilha Orçamentária
      </button>

      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '1.7rem' }}>
          <CircleDollarSign color="var(--accent-primary)" /> Precificação do Orçamento
        </h1>
        <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem' }}>
          Custos indiretos + formação do preço de venda com markup divisor — o BDI resultante é calculado, nunca chutado.
        </p>
      </div>

      {error && <div style={{ padding: '1rem', backgroundColor: 'rgba(239,68,68,0.1)', border: '1px solid var(--danger)', borderRadius: 'var(--border-radius-md)', color: 'var(--danger)', marginBottom: '1.5rem' }}>{error}</div>}
      {okMsg && <div style={{ padding: '1rem', backgroundColor: 'rgba(34,197,94,0.1)', border: '1px solid var(--success)', borderRadius: 'var(--border-radius-md)', color: 'var(--success)', marginBottom: '1.5rem' }}>{okMsg}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 400px', gap: '2rem', alignItems: 'start' }}>
        {/* Coluna esquerda: custos indiretos + parâmetros */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
          {/* Custos indiretos */}
          <div className="glass-panel" style={{ padding: '1.5rem' }}>
            <h2 style={{ fontSize: '1.15rem', marginBottom: '0.4rem' }}>Custos Indiretos</h2>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '1rem', fontStyle: 'italic' }}>
              Equipe técnica, mobilização, EPI, ferramentas, despesas correntes... Normalmente 5% a 30% do custo da obra.
            </p>

            {custos && custos.itens.length > 0 && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem', marginBottom: '1rem' }}>
                {custos.itens.map((c) => (
                  <div key={c.id} style={{ display: 'grid', gridTemplateColumns: '170px 1fr 60px 110px 110px 30px', gap: '0.6rem', alignItems: 'center', padding: '0.4rem 0', borderBottom: '1px solid var(--border-color)', fontSize: '0.88rem' }}>
                    <span style={{ color: 'var(--text-muted)', fontSize: '0.78rem' }}>{labelDe(c.categoria)}</span>
                    <span>{c.descricao}</span>
                    <span style={{ textAlign: 'right' }}>{c.quantidade}</span>
                    <span style={{ textAlign: 'right' }}>{fmtMoeda(c.valorUnitario)}</span>
                    <strong style={{ textAlign: 'right' }}>{fmtMoeda(c.total)}</strong>
                    <button
                      onClick={() => { if (window.confirm('Excluir este custo indireto?')) executar(() => call(`/orcamentos/${id}/custos-indiretos/${c.id}`, { method: 'DELETE' })); }}
                      style={{ background: 'transparent', border: 'none', color: 'var(--danger)', cursor: 'pointer' }}
                    ><Trash2 size={14} /></button>
                  </div>
                ))}
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', paddingTop: '0.6rem', fontWeight: 600 }}>
                  Total: <span style={{ color: 'var(--warning)' }}>{fmtMoeda(custos.total)}</span>
                </div>
              </div>
            )}

            <form
              onSubmit={(e) => {
                e.preventDefault();
                if (!novoCusto.descricao.trim()) return;
                executar(async () => {
                  await call(`/orcamentos/${id}/custos-indiretos`, {
                    method: 'POST',
                    body: JSON.stringify({
                      categoria: novoCusto.categoria,
                      descricao: novoCusto.descricao.trim(),
                      quantidade: num(novoCusto.quantidade),
                      valorUnitario: num(novoCusto.valorUnitario),
                    }),
                  });
                  setNovoCusto({ categoria: novoCusto.categoria, descricao: '', quantidade: '1', valorUnitario: '' });
                });
              }}
              style={{ display: 'grid', gridTemplateColumns: '180px 1fr 70px 110px 44px', gap: '0.6rem', alignItems: 'end' }}
            >
              <div>
                <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-muted)', fontSize: '0.75rem' }}>Categoria</label>
                <select style={inputStyle} value={novoCusto.categoria} onChange={(e) => setNovoCusto({ ...novoCusto, categoria: e.target.value })}>
                  {CATEGORIAS.map((c) => <option key={c.valor} value={c.valor}>{c.label}</option>)}
                </select>
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-muted)', fontSize: '0.75rem' }}>Descrição</label>
                <input style={inputStyle} value={novoCusto.descricao} onChange={(e) => setNovoCusto({ ...novoCusto, descricao: e.target.value })} placeholder="Ex: Engenheiro civil (mês)" />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-muted)', fontSize: '0.75rem' }}>Qtd</label>
                <input style={inputStyle} value={novoCusto.quantidade} onChange={(e) => setNovoCusto({ ...novoCusto, quantidade: e.target.value })} />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-muted)', fontSize: '0.75rem' }}>R$ Unit</label>
                <input style={inputStyle} value={novoCusto.valorUnitario} onChange={(e) => setNovoCusto({ ...novoCusto, valorUnitario: e.target.value })} />
              </div>
              <button type="submit" className="btn-primary" style={{ padding: '0.45rem' }}><Plus size={16} /></button>
            </form>
          </div>

          {/* Parâmetros da formação de preço */}
          <div className="glass-panel" style={{ padding: '1.5rem' }}>
            <h2 style={{ fontSize: '1.15rem', marginBottom: '1rem' }}>Parâmetros da Formação de Preço</h2>

            <h3 style={{ fontSize: '0.85rem', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.6rem' }}>Sobre o custo (CD + CI)</h3>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '0.8rem', marginBottom: '1.2rem' }}>
              {PARAMS_CUSTO.map(([key, label]) => (
                <div key={key as string}>
                  <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.82rem' }}>{label}</label>
                  <input style={inputStyle} value={paramsEdit[key as string] ?? ''} onChange={(e) => setParamsEdit({ ...paramsEdit, [key as string]: e.target.value })} />
                </div>
              ))}
            </div>

            <h3 style={{ fontSize: '0.85rem', color: 'var(--text-muted)', textTransform: 'uppercase', marginBottom: '0.6rem' }}>Sobre o preço de venda (entram no divisor)</h3>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: '0.8rem' }}>
              {PARAMS_PRECO.map(([key, label]) => (
                <div key={key as string}>
                  <label style={{ display: 'block', marginBottom: '0.3rem', color: 'var(--text-secondary)', fontSize: '0.82rem' }}>{label}</label>
                  <input style={inputStyle} value={paramsEdit[key as string] ?? ''} onChange={(e) => setParamsEdit({ ...paramsEdit, [key as string]: e.target.value })} />
                </div>
              ))}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '1.2rem' }}>
              <button className="btn-primary" onClick={salvarParametros} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Check size={16} /> Salvar e Recalcular
              </button>
            </div>
          </div>
        </div>

        {/* Coluna direita: formação do preço */}
        {formacao && (
          <div style={{ position: 'sticky', top: '2rem' }}>
            <div className="glass-panel" style={{ padding: '1.5rem', background: 'linear-gradient(145deg, rgba(30,41,59,0.7) 0%, rgba(15,23,42,0.9) 100%)' }}>
              <h2 style={{ fontSize: '1.15rem', marginBottom: '1.2rem', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '0.8rem' }}>Formação do Preço de Venda</h2>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.7rem', fontSize: '0.92rem' }}>
                {linha('Custo direto (EAP)', fmtMoeda(formacao.custoDireto))}
                {linha('Custo indireto', fmtMoeda(formacao.custoIndireto), 'var(--warning)')}
                {linha(`ADM central (${fmtPct(formacao.admCentral)})`, fmtMoeda(formacao.admCentralValor))}
                {linha(`Custo financeiro (${fmtPct(formacao.custoFinanceiro)})`, fmtMoeda(formacao.custoFinanceiroValor))}
                {linha(`Imprevistos (${fmtPct(formacao.contingencia)})`, fmtMoeda(formacao.contingenciaValor))}
                {linha('Custo total', fmtMoeda(formacao.custoTotal), 'var(--text-primary)', true)}
                {linha(`Tributos (${fmtPct(formacao.aliquotaTributos)})`, fmtMoeda(formacao.tributosValor), 'var(--danger)')}
                {formacao.comissao > 0 && linha(`Comissão (${fmtPct(formacao.comissao)})`, fmtMoeda(formacao.comissaoValor), 'var(--danger)')}
                {linha(`Lucro (${fmtPct(formacao.lucro)})`, fmtMoeda(formacao.lucroValor), 'var(--success)')}
                {linha('Preço de venda', fmtMoeda(formacao.precoVenda), 'var(--success)', true)}

                <div style={{ marginTop: '0.8rem', padding: '0.8rem', borderRadius: '6px', background: 'rgba(59,130,246,0.08)', border: '1px solid rgba(59,130,246,0.25)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--accent-primary)' }}>
                    <span>BDI sobre custo total</span><strong>{fmtPct(formacao.bdiSobreCustoTotal)}</strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--accent-primary)', marginTop: '0.3rem' }}>
                    <span>BDI sobre custo direto</span><strong>{fmtPct(formacao.bdiSobreCustoDireto)}</strong>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-muted)', marginTop: '0.3rem', fontSize: '0.85rem' }}>
                    <span>BDI aplicado hoje no orçamento</span><span>{fmtPct(formacao.bdiAtualDoOrcamento)}</span>
                  </div>
                </div>

                <button className="btn-primary" onClick={aplicarBdi} style={{ width: '100%', marginTop: '0.8rem' }}>
                  Aplicar BDI de {fmtPct(formacao.bdiSobreCustoDireto)} ao Orçamento
                </button>
                <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                  Aplica o BDI sobre o custo direto para a planilha orçamentária fechar exatamente no preço de venda formado acima.
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
