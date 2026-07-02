CREATE TABLE catalogos_bases (
    id UUID PRIMARY KEY,
    nome VARCHAR(100) NOT NULL, -- SINAPI, TCPO, BASE_PROPRIA
    mes_ano VARCHAR(20) NOT NULL, -- 2026-01
    estado VARCHAR(2) NOT NULL, -- SP, RJ
    is_desonerado BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE catalogos_itens (
    id UUID PRIMARY KEY,
    catalogo_id UUID NOT NULL REFERENCES catalogos_bases(id) ON DELETE CASCADE,
    codigo VARCHAR(50) NOT NULL,
    descricao TEXT NOT NULL,
    unidade VARCHAR(10) NOT NULL,
    valor_mo NUMERIC(19, 4) NOT NULL DEFAULT 0,
    valor_mat NUMERIC(19, 4) NOT NULL DEFAULT 0,
    valor_srv NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_catalogo_codigo UNIQUE(catalogo_id, codigo)
);

CREATE TABLE catalogos_insumos (
    id UUID PRIMARY KEY,
    item_id UUID NOT NULL REFERENCES catalogos_itens(id) ON DELETE CASCADE,
    tipo_insumo VARCHAR(50) NOT NULL, -- MAO_DE_OBRA, MATERIAL, EQUIPAMENTO, SERVICO
    codigo VARCHAR(50), -- Opcional, o código do insumo no SINAPI
    descricao TEXT NOT NULL,
    unidade VARCHAR(10) NOT NULL,
    coeficiente NUMERIC(19, 6) NOT NULL, -- Ex: 0.1250000 m3/m2
    custo_unitario NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_catalogos_itens_codigo ON catalogos_itens(codigo);
CREATE INDEX idx_catalogos_insumos_item ON catalogos_insumos(item_id);
