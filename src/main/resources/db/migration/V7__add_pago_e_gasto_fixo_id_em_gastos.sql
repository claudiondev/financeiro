-- Gasto passa a poder nascer "pendente" (pago=false) quando gerado a partir de um
-- gasto fixo. Gasto avulso continua nascendo pago=true, sem mudanca de comportamento
-- (default TRUE cobre as linhas existentes tambem).

ALTER TABLE gastos ADD COLUMN pago BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE gastos ADD COLUMN gasto_fixo_id BIGINT NULL;
ALTER TABLE gastos ADD COLUMN ultimo_lembrete_enviado_em DATE NULL;
ALTER TABLE gastos ADD CONSTRAINT fk_gasto_gasto_fixo FOREIGN KEY (gasto_fixo_id) REFERENCES gastos_fixos(id) ON DELETE SET NULL;
