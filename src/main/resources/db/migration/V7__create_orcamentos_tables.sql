CREATE TABLE orcamentos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    parent_id UUID,
    titulo VARCHAR(255) NOT NULL,
    bdi NUMERIC(5, 4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(50) NOT NULL DEFAULT 'RASCUNHO',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_orcamentos_bdi_positive CHECK (bdi >= 0),
    CONSTRAINT chk_orcamentos_status CHECK (status IN ('RASCUNHO', 'EM_REVISAO', 'APROVADO', 'ENVIADO_CLIENTE', 'ACEITO', 'REJEITADO', 'CANCELADO')),
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (parent_id) REFERENCES orcamentos(id) ON DELETE RESTRICT
);

CREATE TABLE orcamentos_itens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    orcamento_id UUID NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    quantidade NUMERIC(12, 4) NOT NULL,
    custo_unitario NUMERIC(15, 4) NOT NULL,
    
    CONSTRAINT chk_itens_quantidade_positive CHECK (quantidade >= 0),
    CONSTRAINT chk_itens_custo_positive CHECK (custo_unitario >= 0),
    FOREIGN KEY (orcamento_id) REFERENCES orcamentos(id) ON DELETE CASCADE
);

CREATE INDEX idx_orcamentos_tenant_id ON orcamentos(tenant_id);
CREATE INDEX idx_orcamentos_owner_id ON orcamentos(owner_id);
CREATE INDEX idx_orcamentos_itens_orcamento_id ON orcamentos_itens(orcamento_id);
