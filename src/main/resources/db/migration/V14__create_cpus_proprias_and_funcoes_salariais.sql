-- Fase 2: CPU própria do usuário + tabela salarial por função (aba "3. CPU" da planilha de referência)

-- Tabela de salários-hora por função (Pedreiro, Servente, Encanador...)
CREATE TABLE IF NOT EXISTS funcoes_salariais (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id UUID NOT NULL,
    nome VARCHAR(100) NOT NULL,
    valor_hora NUMERIC(15, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_funcoes_valor_hora_positive CHECK (valor_hora >= 0),
    CONSTRAINT uq_funcoes_owner_nome UNIQUE (owner_id, nome),
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_funcoes_salariais_owner ON funcoes_salariais(owner_id);

-- Composições de preço unitário próprias, reutilizáveis entre orçamentos.
-- is_auxiliar = composição auxiliar (ex.: argamassa 1:2:8) que pode ser usada como insumo de outra CPU.
CREATE TABLE IF NOT EXISTS cpus_proprias (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    owner_id UUID NOT NULL,
    codigo VARCHAR(50) NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    unidade VARCHAR(20) NOT NULL,
    is_auxiliar BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_cpus_owner_codigo UNIQUE (owner_id, codigo),
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_cpus_proprias_owner ON cpus_proprias(owner_id);

-- Insumos de cada CPU própria.
-- funcao_salarial_id: insumo de MO ligado à tabela salarial (custo-hora "vivo").
-- cpu_referencia_id: insumo que referencia uma composição auxiliar (profundidade 1, validada na aplicação).
CREATE TABLE IF NOT EXISTS cpus_proprias_insumos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cpu_id UUID NOT NULL,
    tipo_insumo VARCHAR(20) NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    unidade VARCHAR(20) NOT NULL,
    coeficiente NUMERIC(12, 6) NOT NULL DEFAULT 0,
    custo_unitario NUMERIC(15, 4) NOT NULL DEFAULT 0,
    funcao_salarial_id UUID,
    cpu_referencia_id UUID,

    CONSTRAINT chk_cpu_insumo_tipo CHECK (tipo_insumo IN ('MAO_DE_OBRA', 'MATERIAL', 'EQUIPAMENTO', 'SERVICO')),
    CONSTRAINT chk_cpu_insumo_positive CHECK (coeficiente >= 0 AND custo_unitario >= 0),
    FOREIGN KEY (cpu_id) REFERENCES cpus_proprias(id) ON DELETE CASCADE,
    FOREIGN KEY (funcao_salarial_id) REFERENCES funcoes_salariais(id) ON DELETE SET NULL,
    FOREIGN KEY (cpu_referencia_id) REFERENCES cpus_proprias(id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_cpus_insumos_cpu ON cpus_proprias_insumos(cpu_id);
