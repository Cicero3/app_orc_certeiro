import React, { useState, useMemo } from 'react';
import { Plus, Trash2, Calculator, BrickWall } from 'lucide-react';
import { VincularOrcamentoButton } from '../../components/VincularOrcamento';

interface Parede {
  id: string;
  nome: string;
  comprimento: number;
  altura: number;
}

interface Desconto {
  id: string;
  nome: string;
  quantidade: number;
  comprimento: number;
  altura: number;
}

export const AlvenariaCalculator: React.FC = () => {
  const [paredes, setParedes] = useState<Parede[]>([
    { id: crypto.randomUUID(), nome: 'Eixo A', comprimento: 0, altura: 0 }
  ]);
  
  const [descontos, setDescontos] = useState<Desconto[]>([
    { id: crypto.randomUUID(), nome: 'Porta P1', quantidade: 1, comprimento: 0, altura: 0 }
  ]);

  const [tipoBloco, setTipoBloco] = useState('Tijolo cerâmico 10x20x20');

  // --- Handlers de Parede ---
  const handleAddParede = () => {
    setParedes([...paredes, { id: crypto.randomUUID(), nome: `Parede ${paredes.length + 1}`, comprimento: 0, altura: 0 }]);
  };

  const handleUpdateParede = (id: string, field: keyof Parede, value: string | number) => {
    setParedes(paredes.map(p => p.id === id ? { ...p, [field]: value } : p));
  };

  const handleRemoveParede = (id: string) => {
    if (paredes.length > 1) {
      setParedes(paredes.filter(p => p.id !== id));
    }
  };

  // --- Handlers de Desconto ---
  const handleAddDesconto = () => {
    setDescontos([...descontos, { id: crypto.randomUUID(), nome: `Vão ${descontos.length + 1}`, quantidade: 1, comprimento: 0, altura: 0 }]);
  };

  const handleUpdateDesconto = (id: string, field: keyof Desconto, value: string | number) => {
    setDescontos(descontos.map(d => d.id === id ? { ...d, [field]: value } : d));
  };

  const handleRemoveDesconto = (id: string) => {
    setDescontos(descontos.filter(d => d.id !== id));
  };

  // --- Cálculos ---
  const areaBruta = useMemo(() => {
    return paredes.reduce((acc, p) => acc + (p.comprimento * p.altura), 0);
  }, [paredes]);

  const encunhamento = useMemo(() => {
    return paredes.reduce((acc, p) => acc + p.comprimento, 0);
  }, [paredes]);

  const areaDesconto = useMemo(() => {
    return descontos.reduce((acc, d) => acc + (d.quantidade * d.comprimento * d.altura), 0);
  }, [descontos]);

  const areaLiquida = useMemo(() => {
    const liquida = areaBruta - areaDesconto;
    return liquida > 0 ? liquida : 0;
  }, [areaBruta, areaDesconto]);

  return (
    <div className="animate-fade-in" style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '1.8rem' }}>
          <BrickWall color="var(--accent-primary)" /> Calculadora de Alvenaria
        </h1>
        <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem' }}>
          Levantamento quantitativo rápido para paredes e fechamentos.
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 350px', gap: '2rem', alignItems: 'start' }}>
        {/* Esquerda: Formulários */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
          
          {/* Seção Paredes */}
          <div className="glass-panel" style={{ padding: '1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <h2 style={{ fontSize: '1.2rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Calculator size={18} color="var(--accent-primary)" /> Paredes (Área Bruta)
              </h2>
              <button onClick={handleAddParede} className="btn-secondary" style={{ padding: '0.4rem 0.8rem', fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                <Plus size={14} /> Adicionar
              </button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {paredes.map((parede) => (
                <div key={parede.id} style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr 40px', gap: '1rem', alignItems: 'end' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>Eixo / Nome</label>
                    <input 
                      type="text" 
                      value={parede.nome} 
                      onChange={(e) => handleUpdateParede(parede.id, 'nome', e.target.value)}
                      style={{ width: '100%', padding: '0.6rem', borderRadius: '4px', border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', color: 'var(--text-primary)' }}
                    />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>Compr. (m)</label>
                    <input 
                      type="number" 
                      min="0" step="0.01"
                      value={parede.comprimento || ''} 
                      onChange={(e) => handleUpdateParede(parede.id, 'comprimento', parseFloat(e.target.value) || 0)}
                      style={{ width: '100%', padding: '0.6rem', borderRadius: '4px', border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', color: 'var(--text-primary)' }}
                    />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>Altura (m)</label>
                    <input 
                      type="number" 
                      min="0" step="0.01"
                      value={parede.altura || ''} 
                      onChange={(e) => handleUpdateParede(parede.id, 'altura', parseFloat(e.target.value) || 0)}
                      style={{ width: '100%', padding: '0.6rem', borderRadius: '4px', border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', color: 'var(--text-primary)' }}
                    />
                  </div>
                  <button 
                    onClick={() => handleRemoveParede(parede.id)}
                    disabled={paredes.length === 1}
                    style={{ background: 'transparent', border: 'none', color: paredes.length === 1 ? 'var(--border-color)' : 'var(--danger)', cursor: paredes.length === 1 ? 'not-allowed' : 'pointer', padding: '0.6rem' }}
                  >
                    <Trash2 size={18} />
                  </button>
                </div>
              ))}
            </div>
          </div>

          {/* Seção Descontos */}
          <div className="glass-panel" style={{ padding: '1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <h2 style={{ fontSize: '1.2rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Calculator size={18} color="var(--warning)" /> Descontos (Vãos)
              </h2>
              <button onClick={handleAddDesconto} className="btn-secondary" style={{ padding: '0.4rem 0.8rem', fontSize: '0.9rem', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                <Plus size={14} /> Adicionar
              </button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {descontos.map((desc) => (
                <div key={desc.id} style={{ display: 'grid', gridTemplateColumns: '2fr 0.8fr 1fr 1fr 40px', gap: '1rem', alignItems: 'end' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>Tipo (Janela/Porta)</label>
                    <input 
                      type="text" 
                      value={desc.nome} 
                      onChange={(e) => handleUpdateDesconto(desc.id, 'nome', e.target.value)}
                      style={{ width: '100%', padding: '0.6rem', borderRadius: '4px', border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', color: 'var(--text-primary)' }}
                    />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>Qtd.</label>
                    <input 
                      type="number" 
                      min="1" step="1"
                      value={desc.quantidade || ''} 
                      onChange={(e) => handleUpdateDesconto(desc.id, 'quantidade', parseInt(e.target.value) || 0)}
                      style={{ width: '100%', padding: '0.6rem', borderRadius: '4px', border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', color: 'var(--text-primary)' }}
                    />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>Larg. (m)</label>
                    <input 
                      type="number" 
                      min="0" step="0.01"
                      value={desc.comprimento || ''} 
                      onChange={(e) => handleUpdateDesconto(desc.id, 'comprimento', parseFloat(e.target.value) || 0)}
                      style={{ width: '100%', padding: '0.6rem', borderRadius: '4px', border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', color: 'var(--text-primary)' }}
                    />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>Altura (m)</label>
                    <input 
                      type="number" 
                      min="0" step="0.01"
                      value={desc.altura || ''} 
                      onChange={(e) => handleUpdateDesconto(desc.id, 'altura', parseFloat(e.target.value) || 0)}
                      style={{ width: '100%', padding: '0.6rem', borderRadius: '4px', border: '1px solid var(--border-color)', background: 'var(--bg-tertiary)', color: 'var(--text-primary)' }}
                    />
                  </div>
                  <button 
                    onClick={() => handleRemoveDesconto(desc.id)}
                    style={{ background: 'transparent', border: 'none', color: 'var(--danger)', cursor: 'pointer', padding: '0.6rem' }}
                  >
                    <Trash2 size={18} />
                  </button>
                </div>
              ))}
              {descontos.length === 0 && (
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', fontStyle: 'italic' }}>Nenhum desconto adicionado.</p>
              )}
            </div>
          </div>
        </div>

        {/* Direita: Painel de Resultados */}
        <div style={{ position: 'sticky', top: '2rem' }}>
          <div className="glass-panel" style={{ padding: '1.5rem', background: 'linear-gradient(145deg, rgba(30,41,59,0.7) 0%, rgba(15,23,42,0.9) 100%)', border: '1px solid rgba(255,255,255,0.1)' }}>
            <h2 style={{ fontSize: '1.2rem', marginBottom: '1.5rem', borderBottom: '1px solid rgba(255,255,255,0.1)', paddingBottom: '0.8rem' }}>Resumo do Cálculo</h2>
            
            <div style={{ marginBottom: '1.5rem' }}>
              <label style={{ display: 'block', fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>Tipo de Bloco / Material</label>
              <select 
                value={tipoBloco} 
                onChange={(e) => setTipoBloco(e.target.value)}
                style={{ width: '100%', padding: '0.6rem', borderRadius: '4px', border: '1px solid rgba(255,255,255,0.2)', background: 'rgba(0,0,0,0.2)', color: 'white' }}
              >
                <option value="Tijolo cerâmico 10x20x20">Tijolo cerâmico 10x20x20</option>
                <option value="Bloco Concreto 10x20x40">Bloco Concreto 10x20x40</option>
                <option value="Tijolo de vidro">Tijolo de vidro</option>
                <option value="Cobogó">Cobogó</option>
                <option value="DryWall">DryWall</option>
              </select>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)' }}>
                <span>Área Bruta (+)</span>
                <span>{areaBruta.toFixed(2)} m²</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--warning)' }}>
                <span>Descontos (-)</span>
                <span>{areaDesconto.toFixed(2)} m²</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)' }}>
                <span>Encunhamento</span>
                <span>{encunhamento.toFixed(2)} m</span>
              </div>
              
              <div style={{ marginTop: '1rem', paddingTop: '1rem', borderTop: '1px solid rgba(255,255,255,0.1)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: '500', color: 'white' }}>Área Líquida</span>
                <span style={{ fontSize: '1.5rem', fontWeight: '700', color: 'var(--success)' }}>{areaLiquida.toFixed(2)} m²</span>
              </div>
            </div>

            <div style={{ marginTop: '2rem' }}>
              <VincularOrcamentoButton
                tipo="ALVENARIA"
                descricao={`Levantamento de alvenaria (${tipoBloco}) — área líquida`}
                unidade="M2"
                resultado={areaLiquida}
                payload={{ paredes, descontos, tipoBloco, areaBruta, areaDesconto, encunhamento }}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
