-- Script de bootstrap para um banco NOVO E VAZIO (ex: TiDB Serverless em produção).
--
-- Por que isso existe: as migrations do Flyway (V2 em diante) partem do princípio de que
-- o schema já existia ANTES do Flyway entrar no projeto -- a V1 nunca foi um arquivo real,
-- foi uma baseline manual (spring.flyway.baseline-version=1) marcada em cima do MySQL local
-- que já tinha essas tabelas. Um banco novo não tem esse schema, então a V2 (que faz ALTER
-- TABLE gastos...) quebraria tentando alterar uma tabela inexistente.
--
-- Este script recria o schema exatamente como ele era ANTES da V2 -- só as 3 tabelas
-- originais, nos tipos originais (categoria como texto livre, valores em DOUBLE). Depois
-- de rodar isto uma vez, o Flyway (com baseline-on-migrate=true, baseline-version=1, já
-- configurado no application.properties) detecta o schema existente, marca a baseline em
-- V1 automaticamente no primeiro boot da aplicação, e aplica V2..V10 por cima -- chegando
-- exatamente no schema final usado hoje.
--
-- COMO USAR: cole isto no SQL Editor do TiDB Cloud (ou console MySQL equivalente) UMA VEZ,
-- antes do primeiro deploy do backend. Não faz parte do fluxo normal de migrations -- não
-- roda pelo Flyway, não precisa (e não deve) ser executado numa base que já tem dados.

CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    codigo_recuperacao VARCHAR(255) NULL,
    codigo_recuperacao_expiracao DATETIME NULL,
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

CREATE TABLE gastos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    descricao VARCHAR(255) NOT NULL,
    valor DOUBLE NOT NULL,
    categoria VARCHAR(255) NOT NULL,
    data DATE NOT NULL,
    usuario_id BIGINT NOT NULL,
    CONSTRAINT fk_gasto_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE salarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    valor DOUBLE NOT NULL,
    comissao DOUBLE NULL,
    adicional DOUBLE NULL,
    descricao VARCHAR(255) NULL,
    data DATE NOT NULL,
    usuario_id BIGINT NOT NULL,
    CONSTRAINT fk_salario_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);
