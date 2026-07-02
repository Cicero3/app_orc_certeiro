-- Script para inserir dados falsos simulando a base SINAPI

-- IDs estáticos para a base mockada
-- Catalogo SINAPI 01/2026 SP Desonerado:
-- ID: d729b867-0c75-4752-95cd-8a6ea2db7936

INSERT INTO catalogos_bases (id, nome, mes_ano, estado, is_desonerado)
VALUES ('d729b867-0c75-4752-95cd-8a6ea2db7936', 'SINAPI', '2026-01', 'SP', TRUE);

-- Serviço 1: Alvenaria de Vedação
-- ID: 51b3a1a6-2007-4ba3-ab29-7ba55e09b1f7
INSERT INTO catalogos_itens (id, catalogo_id, codigo, descricao, unidade, valor_mo, valor_mat, valor_srv)
VALUES (
    '51b3a1a6-2007-4ba3-ab29-7ba55e09b1f7',
    'd729b867-0c75-4752-95cd-8a6ea2db7936',
    '87316',
    'ALVENARIA DE VEDAÇÃO DE BLOCOS CERÂMICOS FURADOS NA HORIZONTAL DE 9X19X19 CM (ESPESSURA 9 CM) E ARGAMASSA DE ASSENTAMENTO COM PREPARO EM BETONEIRA. AF_06/2014',
    'm2',
    25.50, -- Mão de obra somada
    18.20, -- Material somado
    0.0
);

-- Insumos do Serviço 1
-- Pedreiro
INSERT INTO catalogos_insumos (id, item_id, tipo_insumo, codigo, descricao, unidade, coeficiente, custo_unitario)
VALUES (
    gen_random_uuid(), '51b3a1a6-2007-4ba3-ab29-7ba55e09b1f7', 'MAO_DE_OBRA', '4750', 'PEDREIRO COM ENCARGOS COMPLEMENTARES', 'H', 1.5, 17.00
);

-- Bloco
INSERT INTO catalogos_insumos (id, item_id, tipo_insumo, codigo, descricao, unidade, coeficiente, custo_unitario)
VALUES (
    gen_random_uuid(), '51b3a1a6-2007-4ba3-ab29-7ba55e09b1f7', 'MATERIAL', '7266', 'BLOCO CERAMICO / TIJOLO VAZADO PARA ALVENARIA DE VEDACAO, FUROS NA HORIZONTAL, 9 X 19 X 19 CM (L X A X C)', 'UN', 25.0, 0.60
);

-- Argamassa
INSERT INTO catalogos_insumos (id, item_id, tipo_insumo, codigo, descricao, unidade, coeficiente, custo_unitario)
VALUES (
    gen_random_uuid(), '51b3a1a6-2007-4ba3-ab29-7ba55e09b1f7', 'MATERIAL', '87292', 'ARGAMASSA TRAÇO 1:2:8 (EM VOLUME DE CIMENTO, CAL E AREIA MÉDIA ÚMIDA) PARA ASSENTAMENTO DE ALVENARIA DE VEDAÇÃO', 'm3', 0.011, 290.90
);

-- Serviço 2: Concreto Armado (Fundação)
-- ID: f4d37c9b-63fc-4de7-9159-21655b3bc914
INSERT INTO catalogos_itens (id, catalogo_id, codigo, descricao, unidade, valor_mo, valor_mat, valor_srv)
VALUES (
    'f4d37c9b-63fc-4de7-9159-21655b3bc914',
    'd729b867-0c75-4752-95cd-8a6ea2db7936',
    '96546',
    'ARMAÇÃO DE BLOCO, VIGA BALDRAME OU SAPATA UTILIZANDO AÇO CA-50 DE 10 MM - MONTAGEM. AF_06/2017',
    'KG',
    3.80,
    11.20,
    0.0
);

-- Insumos Serviço 2
-- Armador
INSERT INTO catalogos_insumos (id, item_id, tipo_insumo, codigo, descricao, unidade, coeficiente, custo_unitario)
VALUES (
    gen_random_uuid(), 'f4d37c9b-63fc-4de7-9159-21655b3bc914', 'MAO_DE_OBRA', '378', 'ARMADOR COM ENCARGOS COMPLEMENTARES', 'H', 0.2, 19.00
);

-- Aço
INSERT INTO catalogos_insumos (id, item_id, tipo_insumo, codigo, descricao, unidade, coeficiente, custo_unitario)
VALUES (
    gen_random_uuid(), 'f4d37c9b-63fc-4de7-9159-21655b3bc914', 'MATERIAL', '34', 'ACO CA-50, 10,0 MM, VERGALHAO', 'KG', 1.05, 10.66
);
