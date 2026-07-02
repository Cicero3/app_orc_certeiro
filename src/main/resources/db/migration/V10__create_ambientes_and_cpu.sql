-- Tabela Resumo do Projeto (Ambientes)
CREATE TABLE ambientes_projeto (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    orcamento_id UUID NOT NULL,
    nome_ambiente VARCHAR(255) NOT NULL,
    largura NUMERIC(10, 4) NOT NULL DEFAULT 0.0000,
    comprimento NUMERIC(10, 4) NOT NULL DEFAULT 0.0000,
    pe_direito NUMERIC(10, 4) NOT NULL DEFAULT 0.0000,
    
    FOREIGN KEY (orcamento_id) REFERENCES orcamentos(id) ON DELETE CASCADE
);
CREATE INDEX idx_ambientes_orcamento_id ON ambientes_projeto(orcamento_id);

-- Tabela Composição de Preço Unitário (CPU)
CREATE TABLE composicoes_preco (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    eap_item_id UUID NOT NULL,
    tipo_insumo VARCHAR(50) NOT NULL, -- MAO_DE_OBRA, MATERIAL, EQUIPAMENTO, SERVICO
    descricao VARCHAR(500) NOT NULL,
    unidade VARCHAR(20) NOT NULL,
    coeficiente NUMERIC(12, 6) NOT NULL DEFAULT 0.000000,
    custo_unitario_insumo NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    
    CONSTRAINT chk_cpu_coef_positive CHECK (coeficiente >= 0),
    CONSTRAINT chk_cpu_custo_positive CHECK (custo_unitario_insumo >= 0),
    FOREIGN KEY (eap_item_id) REFERENCES eap_itens(id) ON DELETE CASCADE
);
CREATE INDEX idx_cpu_eap_item_id ON composicoes_preco(eap_item_id);
