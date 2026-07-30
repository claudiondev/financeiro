-- Nome de exibicao do usuario, opcional. Contas existentes nascem com nome=NULL;
-- o frontend usa a parte antes do @ do e-mail como fallback ate a pessoa preencher.
ALTER TABLE usuarios ADD COLUMN nome VARCHAR(100) NULL;
