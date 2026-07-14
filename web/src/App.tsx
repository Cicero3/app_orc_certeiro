import { BrowserRouter as Router, Routes, Route, Link, Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Catalogo } from './pages/Catalogo';
import { OrcamentosList } from './pages/OrcamentosList';
import { OrcamentoDetail } from './pages/OrcamentoDetail';
import { CpusPage } from './pages/CpusPage';
import { OrcamentoPrecificacao } from './pages/OrcamentoPrecificacao';
import { OrcamentoDimensionamento } from './pages/OrcamentoDimensionamento';
import { OrcamentoRiscos } from './pages/OrcamentoRiscos';
import { OrcamentoCronograma } from './pages/OrcamentoCronograma';
import { OrcamentoDiario } from './pages/OrcamentoDiario';
import { EnergiaCalculator } from './pages/calculadoras/EnergiaCalculator';
import { AlvenariaCalculator } from './pages/calculadoras/AlvenariaCalculator';
import {
  BaldrameCalculator, BlocosCalculator, LajesCalculator, PilaresCalculator,
  RadierCalculator, SapatasCalculator, TubuloesCalculator, VigasSuperioresCalculator,
} from './pages/calculadoras/estruturas';
import {
  EletricaDistribuicaoCalculator, EletricaPrumadasCalculator,
  EsgotoPluvialCalculator, HidraulicaCalculator,
} from './pages/calculadoras/instalacoes';
import { CoberturaCalculator } from './pages/calculadoras/CoberturaCalculator';
import { EsquadriasCalculator } from './pages/calculadoras/EsquadriasCalculator';
import { ChecklistOrcamento } from './pages/calculadoras/ChecklistOrcamento';
import { PrecificacaoCalculator } from './pages/calculadoras/PrecificacaoCalculator';
import { WhatsAppButton } from './components/WhatsAppButton';
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

const SidebarLink = ({ to, label }: { to: string; label: string }) => (
  <Link to={to} className="btn-secondary" style={{ textAlign: 'left', border: 'none', background: 'transparent', padding: '0.45rem 0.75rem', fontSize: '0.9rem' }}>
    {label}
  </Link>
);

const SidebarGroup = ({ titulo, children }: { titulo: string; children: React.ReactNode }) => (
  <>
    <div style={{ marginTop: '1rem', marginBottom: '0.3rem' }}>
      <span style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-muted)', letterSpacing: '0.05em', paddingLeft: '0.5rem' }}>
        {titulo}
      </span>
    </div>
    {children}
  </>
);

// Layout base para páginas privadas (com Sidebar)
const MainLayout = ({ children }: { children: React.ReactNode }) => {
  const { logout, user } = useAuth();

  return (
    <div className="app-container">
      <aside className="sidebar">
        <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--border-color)' }}>
          <h2 style={{ fontSize: '1.2rem', color: 'var(--accent-primary)' }}>Orçamento Certeiro</h2>
        </div>
        <nav style={{ padding: '1rem', display: 'flex', flexDirection: 'column', gap: '0.3rem', flex: 1, overflowY: 'auto' }}>
          <Link to="/" className="btn-secondary" style={{ textAlign: 'left', border: 'none', background: 'transparent' }}>📊 Dashboard</Link>
          <Link to="/orcamentos" className="btn-secondary" style={{ textAlign: 'left', border: 'none', background: 'transparent' }}>🏗️ Meus Orçamentos</Link>
          <Link to="/catalogo" className="btn-secondary" style={{ textAlign: 'left', border: 'none', background: 'transparent' }}>📚 Catálogo SINAPI</Link>
          <Link to="/cpus" className="btn-secondary" style={{ textAlign: 'left', border: 'none', background: 'transparent' }}>🧮 CPUs & Mão de Obra</Link>
          
          <SidebarGroup titulo="Fundações">
            <SidebarLink to="/calculadoras/baldrame" label="📐 Baldrame" />
            <SidebarLink to="/calculadoras/blocos" label="🧊 Blocos" />
            <SidebarLink to="/calculadoras/sapatas" label="⚓ Sapatas" />
            <SidebarLink to="/calculadoras/radier" label="🟦 Radier" />
            <SidebarLink to="/calculadoras/tubuloes" label="🕳️ Tubulões" />
          </SidebarGroup>

          <SidebarGroup titulo="Estrutura e Vedação">
            <SidebarLink to="/calculadoras/pilares" label="🏛️ Pilares" />
            <SidebarLink to="/calculadoras/vigas" label="🌉 Vigas Superiores" />
            <SidebarLink to="/calculadoras/lajes" label="⬜ Lajes" />
            <SidebarLink to="/calculadoras/alvenaria" label="🧱 Alvenaria" />
            <SidebarLink to="/calculadoras/esquadrias" label="🚪 Esquadrias" />
            <SidebarLink to="/calculadoras/cobertura" label="🏠 Cobertura" />
          </SidebarGroup>

          <SidebarGroup titulo="Instalações">
            <SidebarLink to="/calculadoras/eletrica-distribuicao" label="⚡ Elétrica Distribuição" />
            <SidebarLink to="/calculadoras/eletrica-prumadas" label="🔌 Elétrica Prumadas" />
            <SidebarLink to="/calculadoras/hidraulica" label="💧 Hidráulica" />
            <SidebarLink to="/calculadoras/esgoto-pluvial" label="🌊 Esgoto e Pluvial" />
          </SidebarGroup>

          <SidebarGroup titulo="Gestão do Orçamento">
            <SidebarLink to="/checklist" label="✅ Checklist de Orçamento" />
            <SidebarLink to="/precificacao" label="💰 Precificação e BDI" />
            <SidebarLink to="/calculadoras/energia" label="⚡ Energia do Canteiro" />
          </SidebarGroup>
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

      <WhatsAppButton />
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
        <Route path="/orcamentos/:id" element={<ProtectedRoute><MainLayout><OrcamentoDetail /></MainLayout></ProtectedRoute>} />
        <Route path="/orcamentos/:id/precificacao" element={<ProtectedRoute><MainLayout><OrcamentoPrecificacao /></MainLayout></ProtectedRoute>} />
        <Route path="/orcamentos/:id/dimensionamento" element={<ProtectedRoute><MainLayout><OrcamentoDimensionamento /></MainLayout></ProtectedRoute>} />
        <Route path="/orcamentos/:id/riscos" element={<ProtectedRoute><MainLayout><OrcamentoRiscos /></MainLayout></ProtectedRoute>} />
        <Route path="/orcamentos/:id/cronograma" element={<ProtectedRoute><MainLayout><OrcamentoCronograma /></MainLayout></ProtectedRoute>} />
        <Route path="/orcamentos/:id/diario" element={<ProtectedRoute><MainLayout><OrcamentoDiario /></MainLayout></ProtectedRoute>} />
        <Route path="/calculadoras/energia" element={<ProtectedRoute><MainLayout><EnergiaCalculator /></MainLayout></ProtectedRoute>} />
        <Route path="/catalogo" element={<ProtectedRoute><MainLayout><Catalogo /></MainLayout></ProtectedRoute>} />
        <Route path="/cpus" element={<ProtectedRoute><MainLayout><CpusPage /></MainLayout></ProtectedRoute>} />
        <Route path="/calculadoras/alvenaria" element={<ProtectedRoute><MainLayout><AlvenariaCalculator /></MainLayout></ProtectedRoute>} />
        <Route path="/calculadoras/baldrame" element={<ProtectedRoute><MainLayout><BaldrameCalculator /></MainLayout></ProtectedRoute>} />
        <Route path="/calculadoras/blocos" element={<ProtectedRoute><MainLayout><BlocosCalculator /></MainLayout></ProtectedRoute>} />
        <Route path="/calculadoras/sapatas" element={<ProtectedRoute><MainLayout><SapatasCalculator /></MainLayout></ProtectedRoute>} />
        <Route path="/calculadoras/radier" element={<ProtectedRoute><MainLayout><RadierCalculator /></MainLayout></ProtectedRoute>} />
        <Route path="/calculadoras/tubuloes" element={<ProtectedRoute><MainLayout><TubuloesCalculator /></MainLayout></ProtectedRoute>} />
        <Route path="/calculadoras/pilares" element={<ProtectedRoute><MainLayout><PilaresCalculator /></MainLayout></ProtectedRoute>} />
        <Route path="/calculadoras/vigas" element={<ProtectedRoute><MainLayout><VigasSuperioresCalculator /></MainLayout></ProtectedRoute>} />
        <Route path="/calculadoras/lajes" element={<ProtectedRoute><MainLayout><LajesCalculator /></MainLayout></ProtectedRoute>} />
        <Route path="/calculadoras/esquadrias" element={<ProtectedRoute><MainLayout><EsquadriasCalculator /></MainLayout></ProtectedRoute>} />
        <Route path="/calculadoras/cobertura" element={<ProtectedRoute><MainLayout><CoberturaCalculator /></MainLayout></ProtectedRoute>} />
        <Route path="/calculadoras/eletrica-distribuicao" element={<ProtectedRoute><MainLayout><EletricaDistribuicaoCalculator /></MainLayout></ProtectedRoute>} />
        <Route path="/calculadoras/eletrica-prumadas" element={<ProtectedRoute><MainLayout><EletricaPrumadasCalculator /></MainLayout></ProtectedRoute>} />
        <Route path="/calculadoras/hidraulica" element={<ProtectedRoute><MainLayout><HidraulicaCalculator /></MainLayout></ProtectedRoute>} />
        <Route path="/calculadoras/esgoto-pluvial" element={<ProtectedRoute><MainLayout><EsgotoPluvialCalculator /></MainLayout></ProtectedRoute>} />
        <Route path="/checklist" element={<ProtectedRoute><MainLayout><ChecklistOrcamento /></MainLayout></ProtectedRoute>} />
        <Route path="/precificacao" element={<ProtectedRoute><MainLayout><PrecificacaoCalculator /></MainLayout></ProtectedRoute>} />
      </Routes>
    </Router>
  );
}

export default App;
