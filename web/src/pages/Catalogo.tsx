import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Search, Loader2, Database, ChevronLeft, ChevronRight } from 'lucide-react';

interface CatalogoItem {
  id: string;
  codigo: string;
  descricao: string;
  unidade: string;
  valorTotal: number;
}

interface PageData {
  content: CatalogoItem[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

export const Catalogo: React.FC = () => {
  const [data, setData] = useState<PageData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [error, setError] = useState('');

  const { token } = useAuth();

  const fetchCatalog = async (currentPage: number, searchTerm: string) => {
    setIsLoading(true);
    setError('');
    try {
      const url = new URL('http://localhost:8080/api/v1/catalogos/itens');
      url.searchParams.append('page', currentPage.toString());
      url.searchParams.append('size', '10');
      if (searchTerm) {
        url.searchParams.append('search', searchTerm);
      }

      const response = await fetch(url.toString(), {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (!response.ok) {
        throw new Error('Falha ao buscar dados do catálogo');
      }

      const result = await response.json();
      setData(result.data);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  // Debounced search
  useEffect(() => {
    const timer = setTimeout(() => {
      setPage(0); // Reset to page 0 on new search
      fetchCatalog(0, search);
    }, 500);

    return () => clearTimeout(timer);
  }, [search]);

  // Handle page change
  useEffect(() => {
    fetchCatalog(page, search);
  }, [page]);

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
  };

  return (
    <div className="animate-fade-in" style={{ padding: '2rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <div>
          <h1 style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '1.8rem' }}>
            <Database color="var(--accent-primary)" /> Catálogo SINAPI
          </h1>
          <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem' }}>Consulte composições e insumos atualizados</p>
        </div>

        <div style={{ position: 'relative', width: '300px' }}>
          <Search size={18} color="var(--text-muted)" style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)' }} />
          <input
            type="text"
            placeholder="Buscar por código ou descrição..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ width: '100%', padding: '0.8rem 1rem 0.8rem 2.5rem', borderRadius: 'var(--border-radius-full)', border: '1px solid var(--border-color)', backgroundColor: 'var(--bg-tertiary)', color: 'var(--text-primary)', outline: 'none' }}
          />
        </div>
      </div>

      {error && (
        <div style={{ padding: '1rem', backgroundColor: 'rgba(239, 68, 68, 0.1)', border: '1px solid var(--danger)', borderRadius: 'var(--border-radius-md)', color: 'var(--danger)', marginBottom: '1.5rem' }}>
          {error}
        </div>
      )}

      <div className="glass-panel" style={{ overflow: 'hidden' }}>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ backgroundColor: 'var(--bg-tertiary)', borderBottom: '1px solid var(--border-color)' }}>
                <th style={{ padding: '1rem 1.5rem', color: 'var(--text-secondary)', fontWeight: '600', fontSize: '0.85rem', textTransform: 'uppercase' }}>Código</th>
                <th style={{ padding: '1rem 1.5rem', color: 'var(--text-secondary)', fontWeight: '600', fontSize: '0.85rem', textTransform: 'uppercase' }}>Descrição</th>
                <th style={{ padding: '1rem 1.5rem', color: 'var(--text-secondary)', fontWeight: '600', fontSize: '0.85rem', textTransform: 'uppercase' }}>Unidade</th>
                <th style={{ padding: '1rem 1.5rem', color: 'var(--text-secondary)', fontWeight: '600', fontSize: '0.85rem', textTransform: 'uppercase', textAlign: 'right' }}>Valor Total</th>
              </tr>
            </thead>
            <tbody>
              {isLoading && !data ? (
                <tr>
                  <td colSpan={4} style={{ padding: '3rem', textAlign: 'center' }}>
                    <Loader2 className="animate-spin" size={32} color="var(--accent-primary)" style={{ margin: '0 auto' }} />
                  </td>
                </tr>
              ) : data?.content.length === 0 ? (
                <tr>
                  <td colSpan={4} style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                    Nenhum item encontrado.
                  </td>
                </tr>
              ) : (
                data?.content.map((item) => (
                  <tr key={item.id} style={{ borderBottom: '1px solid var(--border-color)', transition: 'background-color 0.2s' }} className="table-row-hover">
                    <td style={{ padding: '1rem 1.5rem', color: 'var(--accent-primary)', fontWeight: '500' }}>{item.codigo}</td>
                    <td style={{ padding: '1rem 1.5rem', color: 'var(--text-primary)', maxWidth: '400px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }} title={item.descricao}>
                      {item.descricao}
                    </td>
                    <td style={{ padding: '1rem 1.5rem', color: 'var(--text-secondary)' }}>
                      <span style={{ padding: '0.2rem 0.6rem', backgroundColor: 'var(--bg-secondary)', borderRadius: '4px', fontSize: '0.8rem' }}>{item.unidade}</span>
                    </td>
                    <td style={{ padding: '1rem 1.5rem', color: 'var(--text-primary)', fontWeight: '600', textAlign: 'right' }}>
                      {formatCurrency(item.valorTotal)}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {data && data.totalPages > 0 && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem 1.5rem', borderTop: '1px solid var(--border-color)', backgroundColor: 'var(--bg-tertiary)' }}>
            <span style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
              Mostrando {data.number * data.size + 1} a {Math.min((data.number + 1) * data.size, data.totalElements)} de {data.totalElements} registros
            </span>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button 
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0 || isLoading}
                className="btn-secondary" 
                style={{ padding: '0.5rem', display: 'flex', alignItems: 'center' }}
              >
                <ChevronLeft size={18} />
              </button>
              <button 
                onClick={() => setPage(p => Math.min(data.totalPages - 1, p + 1))}
                disabled={page >= data.totalPages - 1 || isLoading}
                className="btn-secondary" 
                style={{ padding: '0.5rem', display: 'flex', alignItems: 'center' }}
              >
                <ChevronRight size={18} />
              </button>
            </div>
          </div>
        )}
      </div>
      <style>{`
        .table-row-hover:hover {
          background-color: var(--bg-tertiary);
          cursor: pointer;
        }
      `}</style>
    </div>
  );
};
