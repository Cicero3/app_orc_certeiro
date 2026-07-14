-- Módulo Riscos & Contingência (planilhas 008/009: matriz de risco + Simulação de Monte Carlo)
-- Impacto em 3 pontos (mínimo / mais provável / máximo) para distribuição triangular;
-- quando só o provável é informado, o impacto é determinístico.

CREATE TABLE IF NOT EXISTS riscos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    orcamento_id UUID NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    categoria VARCHAR(100),
    probabilidade NUMERIC(5, 4) NOT NULL,
    impacto_min NUMERIC(15, 4),
    impacto_provavel NUMERIC(15, 4) NOT NULL,
    impacto_max NUMERIC(15, 4),
    resposta VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_riscos_probabilidade CHECK (probabilidade >= 0 AND probabilidade <= 1),
    CONSTRAINT chk_riscos_impacto CHECK (impacto_provavel >= 0),
    FOREIGN KEY (orcamento_id) REFERENCES orcamentos(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_riscos_orcamento ON riscos(orcamento_id);
