-- Encargos sociais nas funções salariais (planilha de referência 004):
-- tipo de contratação (horista/mensalista) + % de encargos aplicado sobre o salário-hora.
-- Ex.: encargos desonerados da planilha 004: horista 88,28%, mensalista 49,82%.

ALTER TABLE funcoes_salariais
    ADD COLUMN IF NOT EXISTS tipo_contratacao VARCHAR(20) NOT NULL DEFAULT 'HORISTA',
    ADD COLUMN IF NOT EXISTS encargos_pct NUMERIC(6, 4) NOT NULL DEFAULT 0;

ALTER TABLE funcoes_salariais
    DROP CONSTRAINT IF EXISTS chk_funcoes_tipo_contratacao;
ALTER TABLE funcoes_salariais
    ADD CONSTRAINT chk_funcoes_tipo_contratacao CHECK (tipo_contratacao IN ('HORISTA', 'MENSALISTA'));

ALTER TABLE funcoes_salariais
    DROP CONSTRAINT IF EXISTS chk_funcoes_encargos_positive;
ALTER TABLE funcoes_salariais
    ADD CONSTRAINT chk_funcoes_encargos_positive CHECK (encargos_pct >= 0);
