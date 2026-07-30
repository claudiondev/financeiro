-- Tabela nova: sem dado legado a migrar. Um orcamento por (usuario, categoria) —
-- o usuario define o limite mensal uma vez e ele vale todo mes ate ser alterado.

CREATE TABLE orcamentos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    categoria VARCHAR(20) NOT NULL,
    limite_mensal DECIMAL(12,2) NOT NULL,
    usuario_id BIGINT NOT NULL,
    CONSTRAINT uk_orcamento_usuario_categoria UNIQUE (usuario_id, categoria),
    CONSTRAINT fk_orcamento_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);
