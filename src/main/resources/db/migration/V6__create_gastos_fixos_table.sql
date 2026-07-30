-- Molde de conta recorrente (aluguel, assinatura). O Gasto de cada mes e gerado sob
-- demanda a partir daqui (ver GastoFixoService.garantirGastosDoMesGerados), nao por
-- rotina agendada -- o servidor nao roda 24/7 hoje.

CREATE TABLE gastos_fixos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    categoria VARCHAR(20) NOT NULL,
    valor DECIMAL(12,2) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    dia_vencimento INT NOT NULL,
    data_inicio DATE NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    usuario_id BIGINT NOT NULL,
    CONSTRAINT fk_gasto_fixo_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);
