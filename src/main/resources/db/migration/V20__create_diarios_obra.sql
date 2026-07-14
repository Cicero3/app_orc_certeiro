-- Diário de Obra / RDO (planilha de referência 006):
-- registro diário com clima por turno, efetivo (MO direta/indireta), equipamentos,
-- atividades executadas e ocorrências. Um registro por orçamento+data.

CREATE TABLE IF NOT EXISTS diarios_obra (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    orcamento_id UUID NOT NULL,
    data DATE NOT NULL,
    clima_manha VARCHAR(20),
    clima_tarde VARCHAR(20),
    clima_noite VARCHAR(20),
    -- Listas serializadas em JSON: [{"funcao":"Pedreiro","qtde":3}], [{"descricao":"Betoneira","qtde":1}]
    mao_de_obra TEXT,
    equipamentos TEXT,
    atividades TEXT,
    ocorrencias TEXT,
    observacoes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_diario_orcamento_data UNIQUE (orcamento_id, data),
    CONSTRAINT chk_diario_clima CHECK (
        (clima_manha IS NULL OR clima_manha IN ('BOM', 'NUBLADO', 'CHUVOSO', 'IMPRATICAVEL')) AND
        (clima_tarde IS NULL OR clima_tarde IN ('BOM', 'NUBLADO', 'CHUVOSO', 'IMPRATICAVEL')) AND
        (clima_noite IS NULL OR clima_noite IN ('BOM', 'NUBLADO', 'CHUVOSO', 'IMPRATICAVEL'))
    ),
    FOREIGN KEY (orcamento_id) REFERENCES orcamentos(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_diarios_orcamento ON diarios_obra(orcamento_id);
