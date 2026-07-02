-- Trigger function para proteger a tabela de orçamentos
CREATE OR REPLACE FUNCTION block_immutable_orcamentos_updates()
RETURNS TRIGGER AS $$
BEGIN
    -- Se o status for um dos imutáveis
    IF OLD.status IN ('APROVADO', 'ENVIADO_CLIENTE', 'ACEITO', 'REJEITADO', 'CANCELADO') THEN
        
        -- Permite apenas alteração do status em si (transição permitida) e updated_at
        -- Mas bloqueia alteração de BDI, titulo, tenant, owner, parent.
        IF NEW.bdi != OLD.bdi OR 
           NEW.titulo != OLD.titulo OR 
           NEW.tenant_id != OLD.tenant_id OR 
           NEW.owner_id != OLD.owner_id OR 
           NEW.parent_id IS DISTINCT FROM OLD.parent_id THEN
            
            RAISE EXCEPTION 'FRAUD PREVENTION: Cannot alter financial or core values of an immutable budget (status: %).', OLD.status;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_block_immutable_orcamentos
BEFORE UPDATE ON orcamentos
FOR EACH ROW
EXECUTE FUNCTION block_immutable_orcamentos_updates();


-- Trigger function para proteger a tabela de itens de orçamentos
CREATE OR REPLACE FUNCTION block_immutable_orcamentos_itens_updates()
RETURNS TRIGGER AS $$
DECLARE
    orcamento_status VARCHAR(50);
BEGIN
    -- Busca o status do orçamento pai associado a este item.
    -- Se for INSERT, UPDATE ou DELETE no item, precisamos checar o pai.
    
    -- Para DELETE, usamos OLD, para UPDATE/INSERT usamos NEW.
    IF TG_OP = 'DELETE' THEN
        SELECT status INTO orcamento_status FROM orcamentos WHERE id = OLD.orcamento_id;
    ELSE
        SELECT status INTO orcamento_status FROM orcamentos WHERE id = NEW.orcamento_id;
    END IF;

    IF orcamento_status IN ('APROVADO', 'ENVIADO_CLIENTE', 'ACEITO', 'REJEITADO', 'CANCELADO') THEN
        RAISE EXCEPTION 'FRAUD PREVENTION: Cannot alter items of an immutable budget (status: %).', orcamento_status;
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_block_immutable_itens
BEFORE INSERT OR UPDATE OR DELETE ON orcamentos_itens
FOR EACH ROW
EXECUTE FUNCTION block_immutable_orcamentos_itens_updates();
