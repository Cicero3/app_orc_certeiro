-- Nova trigger function para EAP Itens
CREATE OR REPLACE FUNCTION block_immutable_eap_itens_updates()
RETURNS TRIGGER AS $$
DECLARE
    orcamento_status VARCHAR(50);
BEGIN
    IF TG_OP = 'DELETE' THEN
        SELECT status INTO orcamento_status FROM orcamentos WHERE id = OLD.orcamento_id;
    ELSE
        SELECT status INTO orcamento_status FROM orcamentos WHERE id = NEW.orcamento_id;
    END IF;

    IF orcamento_status IN ('APROVADO', 'ENVIADO_CLIENTE', 'ACEITO', 'REJEITADO', 'CANCELADO') THEN
        RAISE EXCEPTION 'FRAUD PREVENTION: Cannot alter EAP items of an immutable budget (status: %).', orcamento_status;
    END IF;

    IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_block_immutable_eap_itens
BEFORE INSERT OR UPDATE OR DELETE ON eap_itens
FOR EACH ROW EXECUTE FUNCTION block_immutable_eap_itens_updates();


-- Nova trigger function para Ambientes Projeto
CREATE OR REPLACE FUNCTION block_immutable_ambientes_updates()
RETURNS TRIGGER AS $$
DECLARE
    orcamento_status VARCHAR(50);
BEGIN
    IF TG_OP = 'DELETE' THEN
        SELECT status INTO orcamento_status FROM orcamentos WHERE id = OLD.orcamento_id;
    ELSE
        SELECT status INTO orcamento_status FROM orcamentos WHERE id = NEW.orcamento_id;
    END IF;

    IF orcamento_status IN ('APROVADO', 'ENVIADO_CLIENTE', 'ACEITO', 'REJEITADO', 'CANCELADO') THEN
        RAISE EXCEPTION 'FRAUD PREVENTION: Cannot alter environments of an immutable budget.';
    END IF;

    IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_block_immutable_ambientes
BEFORE INSERT OR UPDATE OR DELETE ON ambientes_projeto
FOR EACH ROW EXECUTE FUNCTION block_immutable_ambientes_updates();


-- Nova trigger function para CPU (Composicoes Preco)
CREATE OR REPLACE FUNCTION block_immutable_cpu_updates()
RETURNS TRIGGER AS $$
DECLARE
    orcamento_status VARCHAR(50);
BEGIN
    -- Precisamos fazer o JOIN pela eap_itens para achar o orcamento_id
    IF TG_OP = 'DELETE' THEN
        SELECT o.status INTO orcamento_status 
        FROM orcamentos o JOIN eap_itens e ON o.id = e.orcamento_id WHERE e.id = OLD.eap_item_id;
    ELSE
        SELECT o.status INTO orcamento_status 
        FROM orcamentos o JOIN eap_itens e ON o.id = e.orcamento_id WHERE e.id = NEW.eap_item_id;
    END IF;

    IF orcamento_status IN ('APROVADO', 'ENVIADO_CLIENTE', 'ACEITO', 'REJEITADO', 'CANCELADO') THEN
        RAISE EXCEPTION 'FRAUD PREVENTION: Cannot alter Price Composition (CPU) of an immutable budget.';
    END IF;

    IF TG_OP = 'DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_block_immutable_cpu
BEFORE INSERT OR UPDATE OR DELETE ON composicoes_preco
FOR EACH ROW EXECUTE FUNCTION block_immutable_cpu_updates();
