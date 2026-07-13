-- Fase 4: levantamentos de quantitativo (aba "2. LEV QNT" + calculadoras do app)
-- payload guarda os inputs da calculadora (JSON serializado) para reabrir/auditar a memória de cálculo.

CREATE TABLE IF NOT EXISTS levantamentos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    orcamento_id UUID NOT NULL,
    eap_item_id UUID,
    tipo VARCHAR(50) NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    unidade VARCHAR(20) NOT NULL,
    resultado NUMERIC(12, 4) NOT NULL DEFAULT 0,
    payload TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_levantamentos_resultado_positive CHECK (resultado >= 0),
    FOREIGN KEY (orcamento_id) REFERENCES orcamentos(id) ON DELETE CASCADE,
    FOREIGN KEY (eap_item_id) REFERENCES eap_itens(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_levantamentos_orcamento ON levantamentos(orcamento_id);
