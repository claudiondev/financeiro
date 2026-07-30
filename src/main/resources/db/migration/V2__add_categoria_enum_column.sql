-- Passo 1 (expand) da migracao de categoria de texto livre para enum fixo.
-- Cria a coluna nova ao lado da antiga e faz o melhor mapeamento possivel a partir
-- do texto ja cadastrado. Nao apaga nada aqui -- a V3 so deve rodar depois de
-- conferir manualmente o resultado (ver instrucoes no plano de implementacao).

ALTER TABLE gastos ADD COLUMN categoria_enum VARCHAR(20) NULL;

UPDATE gastos SET categoria_enum = CASE
    WHEN LOWER(categoria) IN ('mercado', 'alimentacao', 'alimentação', 'comida', 'supermercado') THEN 'ALIMENTACAO'
    WHEN LOWER(categoria) IN ('transporte', 'combustivel', 'combustível', 'uber', 'gasolina', 'posto') THEN 'TRANSPORTE'
    WHEN LOWER(categoria) IN ('moradia', 'aluguel', 'casa', 'condominio', 'condomínio') THEN 'MORADIA'
    WHEN LOWER(categoria) IN ('lazer', 'entretenimento', 'cinema') THEN 'LAZER'
    WHEN LOWER(categoria) IN ('saude', 'saúde', 'farmacia', 'farmácia', 'remedio', 'remédio') THEN 'SAUDE'
    WHEN LOWER(categoria) IN ('educacao', 'educação', 'curso', 'livro') THEN 'EDUCACAO'
    ELSE 'OUTROS'
END;
