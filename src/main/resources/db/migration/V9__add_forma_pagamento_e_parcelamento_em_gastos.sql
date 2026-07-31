-- Forma de pagamento e parcelamento. Gastos existentes ficam com forma_pagamento NULL
-- (a UI mostra "-"); grupo_parcelamento agrupa as N parcelas de uma mesma compra
-- parcelada -- NULL quando o gasto nao e parcelado.

ALTER TABLE gastos ADD COLUMN forma_pagamento VARCHAR(20) NULL;
ALTER TABLE gastos ADD COLUMN grupo_parcelamento VARCHAR(36) NULL;
ALTER TABLE gastos ADD COLUMN numero_parcela INT NULL;
ALTER TABLE gastos ADD COLUMN total_parcelas INT NULL;

CREATE INDEX idx_gasto_grupo_parcelamento ON gastos (grupo_parcelamento);
