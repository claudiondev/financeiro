-- Troca os valores monetarios de DOUBLE (ponto flutuante, sujeito a erro de
-- arredondamento) para DECIMAL(12,2), que representa dinheiro com exatidao.

ALTER TABLE gastos MODIFY COLUMN valor DECIMAL(12,2) NOT NULL;

ALTER TABLE salarios MODIFY COLUMN valor DECIMAL(12,2) NOT NULL;
ALTER TABLE salarios MODIFY COLUMN comissao DECIMAL(12,2) NULL;
ALTER TABLE salarios MODIFY COLUMN adicional DECIMAL(12,2) NULL;
