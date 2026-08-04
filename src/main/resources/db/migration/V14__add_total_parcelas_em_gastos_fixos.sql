-- Null = conta fixa recorrente pra sempre (comportamento original). Preenchido = tem fim
-- (financiamento/divida) -- a geracao automatica para sozinha ao atingir esse numero de
-- parcelas ja criadas.
ALTER TABLE gastos_fixos ADD COLUMN total_parcelas INT NULL;
