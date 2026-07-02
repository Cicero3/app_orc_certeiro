import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import './index.css'; // Make sure we use our premium CSS

// Fictional Components to demonstrate routing and layout
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

const OrcamentosList = () => (
  <div className="animate-fade-in">
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <h1>Meus Orçamentos</h1>
      <button className="btn-primary">+ Novo Orçamento</button>
    </div>
    <div className="glass-panel" style={{ marginTop: '2rem', padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
      <p>Nenhum orçamento criado ainda.</p>
    </div>
  </div>
);

function App() {
  return (
    <Router>
      <div className="app-container">
        {/* Sidebar */}
        <aside className="sidebar">
          <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--border-color)' }}>
            <h2 style={{ fontSize: '1.2rem', color: 'var(--accent-primary)' }}>Orçamento Certeiro</h2>
          </div>
          <nav style={{ padding: '1rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <Link to="/" className="btn-secondary" style={{ textAlign: 'left', border: 'none', background: 'transparent' }}>📊 Dashboard</Link>
            <Link to="/orcamentos" className="btn-secondary" style={{ textAlign: 'left', border: 'none', background: 'transparent' }}>🏗️ Meus Orçamentos</Link>
            <Link to="/catalogo" className="btn-secondary" style={{ textAlign: 'left', border: 'none', background: 'transparent' }}>📚 Catálogo SINAPI</Link>
          </nav>
        </aside>

        {/* Main Content Area */}
        <main className="main-content">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/orcamentos" element={<OrcamentosList />} />
            <Route path="/catalogo" element={<div className="animate-fade-in"><h1>Catálogo Base</h1></div>} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
