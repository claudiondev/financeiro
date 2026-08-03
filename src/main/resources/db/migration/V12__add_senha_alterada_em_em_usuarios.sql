-- Suporta revogacao de token: qualquer JWT emitido antes da senha ser trocada
-- passa a ser rejeitado, mesmo que ainda nao tenha expirado (ver JwtService/JwtFilter).
ALTER TABLE usuarios ADD COLUMN senha_alterada_em DATETIME NULL;
