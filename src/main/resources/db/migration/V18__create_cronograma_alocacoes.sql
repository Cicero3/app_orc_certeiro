-- Módulo Cronograma Físico-Financeiro (planilha 005: previsto × real por período + curva S)
-- Alocação percentual por módulo da EAP e período (1..N). Percentuais como fração (0.25 = 25%).

CREATE TABLE IF NOT EXISTS cronograma_alocacoes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    orcamento_id UUID NOT NULL,
    modulo_id UUID NOT NULL,
    periodo INTEGER NOT NULL,
    previsto_pct NUMERIC(5, 4) NOT NULL DEFAULT 0,
    real_pct NUMERIC(5, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_cron_periodo CHECK (periodo >= 1 AND periodo <= 120),
    CONSTRAINT chk_cron_pct CHECK (
        previsto_pct >= 0 AND previsto_pct <= 1 AND real_pct >= 0 AND real_pct <= 1
    ),
    CONSTRAINT uq_cron_orc_mod_periodo UNIQUE (orcamento_id, modulo_id, periodo),
    FOREIGN KEY (orcamento_id) REFERENCES orcamentos(id) ON DELETE CASCADE,
    FOREIGN KEY (modulo_id) REFERENCES orcamentos_modulos(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_cron_orcamento ON cronograma_alocacoes(orcamento_id);
