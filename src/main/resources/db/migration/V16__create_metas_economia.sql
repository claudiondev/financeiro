CREATE TABLE metas_economia (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    valor_alvo DECIMAL(12,2) NOT NULL,
    prazo DATE NULL,
    usuario_id BIGINT NOT NULL,
    CONSTRAINT fk_metas_economia_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

-- Aporte pra uma meta nasce como Gasto normal (categoria Poupanca) vinculado aqui. Apagar a
-- meta preserva o historico do gasto, so desvincula (ON DELETE SET NULL), mesmo padrao ja
-- usado em gastos.gasto_fixo_id.
ALTER TABLE gastos ADD COLUMN meta_economia_id BIGINT NULL;
ALTER TABLE gastos ADD CONSTRAINT fk_gastos_meta_economia
    FOREIGN KEY (meta_economia_id) REFERENCES metas_economia(id) ON DELETE SET NULL;
