-- Remove a tabela antiga que era muito simples
DROP TRIGGER IF EXISTS trg_block_immutable_itens ON orcamentos_itens;
DROP FUNCTION IF EXISTS block_immutable_orcamentos_itens_updates();
DROP TABLE IF EXISTS orcamentos_itens CASCADE;

-- Tabela de Módulos (Menu lateral)
CREATE TABLE orcamentos_modulos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    orcamento_id UUID NOT NULL,
    tipo_modulo VARCHAR(50) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    
    FOREIGN KEY (orcamento_id) REFERENCES orcamentos(id) ON DELETE CASCADE
);
CREATE INDEX idx_modulos_orcamento_id ON orcamentos_modulos(orcamento_id);

-- Tabela EAP (Estrutura Analítica do Projeto)
CREATE TABLE eap_itens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    orcamento_id UUID NOT NULL,
    modulo_id UUID,
    parent_id UUID,
    codigo_item VARCHAR(50) NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    marca VARCHAR(255),
    unidade VARCHAR(20),
    quantidade NUMERIC(12, 4) NOT NULL DEFAULT 0.0000,
    
    -- Valores em reais
    valor_mo NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    valor_mat NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    valor_srv NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    
    observacoes TEXT,
    
    CONSTRAINT chk_eap_quant_positive CHECK (quantidade >= 0),
    FOREIGN KEY (orcamento_id) REFERENCES orcamentos(id) ON DELETE CASCADE,
    FOREIGN KEY (modulo_id) REFERENCES orcamentos_modulos(id) ON DELETE SET NULL,
    FOREIGN KEY (parent_id) REFERENCES eap_itens(id) ON DELETE CASCADE
);

CREATE INDEX idx_eap_orcamento_id ON eap_itens(orcamento_id);
CREATE INDEX idx_eap_parent_id ON eap_itens(parent_id);
