-- ID unico da transacao no extrato bancario (OFX) -- so preenchido em lancamentos
-- importados (ver ImportacaoService). NULL nos demais; MySQL permite varios NULL
-- num indice unico, entao lancamentos manuais nao colidem entre si.
ALTER TABLE gastos ADD COLUMN fitid VARCHAR(255) NULL;
ALTER TABLE salarios ADD COLUMN fitid VARCHAR(255) NULL;

ALTER TABLE gastos ADD CONSTRAINT uk_gastos_usuario_fitid UNIQUE (usuario_id, fitid);
ALTER TABLE salarios ADD CONSTRAINT uk_salarios_usuario_fitid UNIQUE (usuario_id, fitid);
