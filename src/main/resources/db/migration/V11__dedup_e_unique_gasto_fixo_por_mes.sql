-- Bug real em producao: duas requisicoes concorrentes chamando garantirGastosDoMesGerados
-- (ex.: Resumo e o alerta de contas vencendo, carregados em paralelo) geraram Gasto duplicado
-- pro mesmo GastoFixo no mesmo mes, quebrando a tela de Contas Fixas com 500 (a busca do
-- gasto do mes esperava no maximo um resultado). Corrigido no codigo (GastoFixoService passou
-- a tolerar duplicata na leitura, e a ignorar a violacao de constraint na escrita concorrente),
-- e aqui na estrutura: remove a duplicata existente e trava pra nao acontecer de novo.

-- Mantem a linha de menor id de cada grupo (gasto_fixo_id, data) duplicado; NULL nunca
-- entra nessa comparacao, entao gastos avulsos (gasto_fixo_id IS NULL) nao sao afetados.
DELETE g1 FROM gastos g1
INNER JOIN gastos g2
    ON g1.gasto_fixo_id = g2.gasto_fixo_id
    AND g1.data = g2.data
    AND g1.id > g2.id
WHERE g1.gasto_fixo_id IS NOT NULL;

ALTER TABLE gastos ADD CONSTRAINT uk_gasto_fixo_data UNIQUE (gasto_fixo_id, data);
