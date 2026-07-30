-- Passo 2 (contract) da migracao de categoria. So rode isto depois de validar
-- manualmente o mapeamento da V2, por exemplo com:
--   SELECT categoria, categoria_enum, COUNT(*) FROM gastos GROUP BY categoria, categoria_enum;
-- Sem essa checagem, um mapeamento errado (tudo caindo em OUTROS, por exemplo)
-- viraria dado definitivo aqui.

ALTER TABLE gastos DROP COLUMN categoria;
ALTER TABLE gastos CHANGE categoria_enum categoria VARCHAR(20) NOT NULL;
