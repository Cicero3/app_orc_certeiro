-- Fase 3: custos indiretos (aba "4. CUSTO IND") + formação de preço de venda (aba "0. P. VENDA")

CREATE TABLE IF NOT EXISTS custos_indiretos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    orcamento_id UUID NOT NULL,
    categoria VARCHAR(40) NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    quantidade NUMERIC(12, 4) NOT NULL DEFAULT 1,
    valor_unitario NUMERIC(15, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_ci_positive CHECK (quantidade >= 0 AND valor_unitario >= 0),
    CONSTRAINT chk_ci_categoria CHECK (categoria IN (
        'EQUIPE_TECNICA', 'EQUIPE_SUPORTE', 'EQUIPE_ADMINISTRATIVA', 'MOBILIZACAO',
        'EQP_CANTEIRO', 'EQP_ADMINISTRATIVO', 'PROTECAO_COLETIVA', 'EPI', 'FERRAMENTAS',
        'DESPESAS_CORRENTES', 'DESPESAS_PESSOAL', 'SERVICOS_TERCEIROS', 'TAXAS', 'DIVERSOS'
    )),
    FOREIGN KEY (orcamento_id) REFERENCES orcamentos(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_custos_indiretos_orcamento ON custos_indiretos(orcamento_id);

-- Parâmetros da formação de preço de venda (1:1 com o orçamento).
-- Percentuais armazenados como fração (0.05 = 5%).
CREATE TABLE IF NOT EXISTS formacoes_preco (
    orcamento_id UUID PRIMARY KEY,
    adm_central NUMERIC(5, 4) NOT NULL DEFAULT 0,
    custo_financeiro NUMERIC(5, 4) NOT NULL DEFAULT 0,
    contingencia NUMERIC(5, 4) NOT NULL DEFAULT 0,
    comissao NUMERIC(5, 4) NOT NULL DEFAULT 0,
    lucro NUMERIC(5, 4) NOT NULL DEFAULT 0,
    -- Tributos sobre o preço de venda (incidem no divisor do markup)
    cofins NUMERIC(5, 4) NOT NULL DEFAULT 0,
    pis NUMERIC(5, 4) NOT NULL DEFAULT 0,
    icms NUMERIC(5, 4) NOT NULL DEFAULT 0,
    iss NUMERIC(5, 4) NOT NULL DEFAULT 0,
    irpj NUMERIC(5, 4) NOT NULL DEFAULT 0,
    csll NUMERIC(5, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_fp_positive CHECK (
        adm_central >= 0 AND custo_financeiro >= 0 AND contingencia >= 0 AND comissao >= 0 AND
        lucro >= 0 AND cofins >= 0 AND pis >= 0 AND icms >= 0 AND iss >= 0 AND irpj >= 0 AND csll >= 0
    ),
    FOREIGN KEY (orcamento_id) REFERENCES orcamentos(id) ON DELETE CASCADE
);
