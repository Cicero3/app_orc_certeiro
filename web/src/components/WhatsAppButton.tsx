import React, { useEffect, useRef, useState } from 'react';

const STORAGE_KEY = 'whatsapp-numero';
const MENSAGEM_PADRAO = 'Olá! Gostaria de solicitar um orçamento de obra.';

// Logo oficial do WhatsApp (lucide não inclui ícones de marca)
const WhatsAppIcon = ({ size = 28 }: { size?: number }) => (
  <svg viewBox="0 0 32 32" width={size} height={size} fill="currentColor" aria-hidden="true">
    <path d="M16.004 3C8.832 3 3 8.83 3 16c0 2.29.6 4.53 1.74 6.5L3 29l6.66-1.72A13 13 0 0 0 16 29h.004C23.176 29 29 23.17 29 16S23.176 3 16.004 3zm0 23.7h-.003a10.7 10.7 0 0 1-5.45-1.49l-.39-.23-4.05 1.05 1.08-3.95-.25-.4A10.63 10.63 0 0 1 5.3 16c0-5.9 4.8-10.7 10.71-10.7 2.86 0 5.55 1.11 7.57 3.13a10.64 10.64 0 0 1 3.13 7.57c0 5.9-4.8 10.7-10.7 10.7zm5.87-8.02c-.32-.16-1.9-.94-2.2-1.05-.29-.11-.51-.16-.72.16-.21.32-.83 1.05-1.02 1.26-.19.21-.37.24-.7.08-.32-.16-1.36-.5-2.58-1.6-.96-.85-1.6-1.9-1.79-2.22-.19-.32-.02-.5.14-.66.15-.14.32-.37.48-.56.16-.19.21-.32.32-.53.11-.21.06-.4-.03-.56-.08-.16-.72-1.73-.98-2.37-.26-.62-.52-.54-.72-.55l-.61-.01c-.21 0-.56.08-.85.4-.29.32-1.12 1.09-1.12 2.66 0 1.57 1.14 3.09 1.3 3.3.16.21 2.25 3.43 5.45 4.81.76.33 1.36.53 1.82.67.77.25 1.46.21 2.01.13.61-.09 1.9-.78 2.16-1.53.27-.75.27-1.39.19-1.53-.08-.13-.29-.21-.61-.37z" />
  </svg>
);

// Botão flutuante do WhatsApp: número editável (persistido no navegador) e
// clique abre a conversa com mensagem de pedido de orçamento pré-preenchida.
export const WhatsAppButton: React.FC = () => {
  const [numero, setNumero] = useState(() => localStorage.getItem(STORAGE_KEY) ?? '');
  const [aberto, setAberto] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, numero);
  }, [numero]);

  // Fecha o painel ao clicar fora dele
  useEffect(() => {
    if (!aberto) return;
    const onClick = (e: MouseEvent) => {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) setAberto(false);
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, [aberto]);

  const digitos = numero.replace(/\D/g, '');
  const numeroValido = digitos.length >= 10; // DDI + DDD + número

  const abrirConversa = () => {
    if (!numeroValido) return;
    const url = `https://wa.me/${digitos}?text=${encodeURIComponent(MENSAGEM_PADRAO)}`;
    window.open(url, '_blank', 'noopener,noreferrer');
  };

  return (
    <div ref={panelRef} style={{ position: 'fixed', bottom: '1.5rem', right: '1.5rem', zIndex: 1000 }}>
      {aberto && (
        <div
          className="glass-panel"
          style={{
            position: 'absolute',
            bottom: '4.5rem',
            right: 0,
            width: '300px',
            padding: '1.2rem',
            background: 'var(--bg-secondary, rgba(15,23,42,0.95))',
            border: '1px solid var(--border-color)',
            boxShadow: '0 10px 30px rgba(0,0,0,0.4)',
          }}
        >
          <h3 style={{ fontSize: '1rem', marginBottom: '0.3rem', display: 'flex', alignItems: 'center', gap: '0.4rem', color: '#25D366' }}>
            <WhatsAppIcon size={18} /> Orçamento via WhatsApp
          </h3>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.8rem' }}>
            Informe o número com DDI e DDD (só dígitos).
          </p>
          <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '0.3rem' }}>
            Número do WhatsApp
          </label>
          <input
            type="tel"
            value={numero}
            placeholder="Ex.: 5511999999999"
            onChange={(e) => setNumero(e.target.value)}
            style={{
              width: '100%',
              padding: '0.6rem',
              borderRadius: '4px',
              border: '1px solid var(--border-color)',
              background: 'var(--bg-tertiary)',
              color: 'var(--text-primary)',
              marginBottom: '0.8rem',
            }}
          />
          {!numeroValido && numero.length > 0 && (
            <p style={{ fontSize: '0.75rem', color: 'var(--danger)', marginBottom: '0.8rem' }}>
              Número incompleto — use DDI + DDD + telefone (ex.: 5511999999999).
            </p>
          )}
          <button
            onClick={abrirConversa}
            disabled={!numeroValido}
            style={{
              width: '100%',
              padding: '0.7rem',
              borderRadius: '6px',
              border: 'none',
              fontWeight: 600,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.5rem',
              background: numeroValido ? '#25D366' : 'var(--border-color)',
              color: numeroValido ? '#fff' : 'var(--text-muted)',
              cursor: numeroValido ? 'pointer' : 'not-allowed',
            }}
          >
            <WhatsAppIcon size={18} /> Conversar e pedir orçamento
          </button>
        </div>
      )}

      <button
        onClick={() => setAberto(!aberto)}
        title="Pedir orçamento pelo WhatsApp"
        aria-label="Pedir orçamento pelo WhatsApp"
        style={{
          width: '56px',
          height: '56px',
          borderRadius: '50%',
          border: 'none',
          background: '#25D366',
          color: '#fff',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          cursor: 'pointer',
          boxShadow: '0 4px 15px rgba(37, 211, 102, 0.45)',
        }}
      >
        <WhatsAppIcon />
      </button>
    </div>
  );
};
