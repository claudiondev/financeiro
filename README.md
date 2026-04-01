
# 💰 Sistema de Controle Financeiro - API Backend

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-green?style=for-the-badge&logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6.4-green?style=for-the-badge&logo=springsecurity)
![JWT](https://img.shields.io/badge/JWT-Auth-black?style=for-the-badge&logo=jsonwebtokens)
![MySQL](https://img.shields.io/badge/MySQL-Database-blue?style=for-the-badge&logo=mysql)
![Railway](https://img.shields.io/badge/Railway-Deploy-black?style=for-the-badge&logo=railway)

> API REST completa para controle financeiro pessoal, com autenticação JWT, recuperação de senha por e-mail e persistência em banco de dados MySQL em produção.

---

## 🔗 API Online

🚀 **Acesse:** https://financeiro-production-e0e0.up.railway.app

> ⚠️ A rota principal retorna 403 por segurança.  
Utilize os endpoints `/auth` e `/gastos` para testar a API.

---

## 📋 Sobre o Projeto

O Sistema de Controle Financeiro é o backend de uma aplicação Full Stack, responsável por gerenciar autenticação, dados financeiros e regras de negócio.

A API foi desenvolvida com Java e Spring Boot, seguindo boas práticas de arquitetura em camadas, e está hospedada em produção no Railway, utilizando banco de dados MySQL para persistência real dos dados.

---

## ✨ Funcionalidades

- 🔐 Autenticação segura com JWT (login e cadastro)
- 📧 Recuperação de senha via e-mail com código de verificação
- 💰 CRUD de gastos e salários
- 📊 Resumo financeiro mensal com saldo e estatísticas
- 💾 Persistência de dados com MySQL em produção

---

## 🔗 Principais Endpoints

### 🔐 Autenticação
- POST /auth/register → Cadastro de usuário
- POST /auth/login → Login e geração de token JWT

### 💰 Gastos
- GET /gastos → Listar gastos
- POST /gastos → Criar gasto
- DELETE /gastos/{id} → Remover gasto

### 💼 Salários
- GET /salarios → Listar salários
- POST /salarios → Criar salário

---

## 📁 Estrutura de Pacotes


src/main/java/com/seuprojeto/financeiro
├── controller # Endpoints da API (requisições HTTP)
├── service # Regras de negócio
├── repository # Acesso ao banco de dados
├── model # Entidades (JPA)
├── dto # Objetos de transferência de dados
├── security # Configurações de segurança (JWT, filtros)
└── config # Configurações gerais da aplicação


---

## 🏗️ Arquitetura

A aplicação segue o padrão de arquitetura em camadas:

- **Controller** → recebe requisições HTTP
- **Service** → contém a lógica de negócio
- **Repository** → acesso ao banco de dados

---

## 🚀 Tecnologias Utilizadas

| Tecnologia | Descrição |
|---|---|
| Java 17 | Linguagem principal do projeto |
| Spring Boot 3.4.3 | Framework para construção da API |
| Spring Security | Proteção de rotas e criptografia de senhas |
| JWT (jjwt) | Autenticação baseada em token |
| Spring Data JPA | Comunicação com banco de dados |
| MySQL | Banco de dados relacional |
| JavaMailSender | Envio de e-mails |
| Railway | Deploy da aplicação |

---
## 📌 Autor

**Claudio Nascimento**  
🔗 https://github.com/claudiondev