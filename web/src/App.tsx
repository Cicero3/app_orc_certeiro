import { BrowserRouter as Router, Routes, Route, Link, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Catalogo } from './pages/Catalogo';
import { OrcamentosList } from './pages/OrcamentosList';
import { LogOut } from 'lucide-react';
import './index.css'; 

const Dashboard = () => (
  <div className="animate-fade-in">
    <h1>Dashboard</h1>
    <p style={{ color: 'var(--text-secondary)', marginTop: '0.5rem' }}>Visão geral dos seus orçamentos</p>
    
    <div style={{ marginTop: '2rem', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1.5rem' }}>
      <div className="glass-panel" style={{ padding: '1.5rem' }}>
        <h3>Orçamentos Ativos</h3>
        <p style={{ fontSize: '2rem', fontWeight: 'bold', color: 'var(--accent-primary)', marginTop: '0.5rem' }}>12</p>
      </div>
      <div className="glass-panel" style={{ padding: '1.5rem' }}>
        <h3>Base SINAPI</h3>
        <p style={{ fontSize: '2rem', fontWeight: 'bold', color: 'var(--success)', marginTop: '0.5rem' }}>Jul/2026</p>
      </div>
    </div>
  </div>
);

// Removed dummy OrcamentosList

const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', backgroundColor: 'var(--bg-primary)' }}>Carregando...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
};

// Layout base para páginas privadas (com Sidebar)
const MainLayout = ({ children }: { children: React.ReactNode }) => {
  const { logout, user } = useAuth();

  return (
    <div className="app-container">
      <aside className="sidebar">
        <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--border-color)' }}>
          <h2 style={{ fontSize: '1.2rem', color: 'var(--accent-primary)' }}>Orçamento Certeiro</h2>
        </div>
        <nav style={{ padding: '1rem', display: 'flex', flexDirection: 'column', gap: '0.5rem', flex: 1 }}>
          <Link to="/" className="btn-secondary" style={{ textAlign: 'left', border: 'none', background: 'transparent' }}>📊 Dashboard</Link>
          <Link to="/orcamentos" className="btn-secondary" style={{ textAlign: 'left', border: 'none', background: 'transparent' }}>🏗️ Meus Orçamentos</Link>
          <Link to="/catalogo" className="btn-secondary" style={{ textAlign: 'left', border: 'none', background: 'transparent' }}>📚 Catálogo SINAPI</Link>
        </nav>
        
        <div style={{ padding: '1.5rem', borderTop: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', wordBreak: 'break-all' }}>
            Logado como:<br/>
            <strong style={{ color: 'var(--text-primary)' }}>{user?.email}</strong>
          </div>
          <button onClick={logout} className="btn-secondary" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', border: '1px solid var(--danger)', color: 'var(--danger)', background: 'rgba(239, 68, 68, 0.1)' }}>
            <LogOut size={16} /> Sair
          </button>
        </div>
      </aside>

      <main className="main-content">
        {children}
      </main>
    </div>
  );
};

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        
        {/* Rotas Privadas */}
        <Route path="/" element={<ProtectedRoute><MainLayout><Dashboard /></MainLayout></ProtectedRoute>} />
        <Route path="/orcamentos" element={<ProtectedRoute><MainLayout><OrcamentosList /></MainLayout></ProtectedRoute>} />
        <Route path="/catalogo" element={<ProtectedRoute><MainLayout><Catalogo /></MainLayout></ProtectedRoute>} />
      </Routes>
    </Router>
  );
}

export default App;
