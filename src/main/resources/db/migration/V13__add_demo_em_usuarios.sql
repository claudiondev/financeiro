-- Marca a conta compartilhada de demonstracao (ver DemoDataSeeder). Usuarios com
-- demo=true tem qualquer escrita bloqueada pelo DemoReadOnlyInterceptor.
ALTER TABLE usuarios ADD COLUMN demo TINYINT(1) NOT NULL DEFAULT 0;
