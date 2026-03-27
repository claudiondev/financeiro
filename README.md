# 💰 Sistema de Controle Financeiro - API Backend

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-green?style=for-the-badge&logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6.4-green?style=for-the-badge&logo=springsecurity)
![JWT](https://img.shields.io/badge/JWT-Auth-black?style=for-the-badge&logo=jsonwebtokens)
![MySQL](https://img.shields.io/badge/MySQL-Database-blue?style=for-the-badge&logo=mysql)
![Railway](https://img.shields.io/badge/Railway-Deploy-black?style=for-the-badge&logo=railway)

> API REST para controle de gastos e salários com autenticação JWT, hospedada no Railway e conectada a um banco MySQL real.

---

## 📋 Sobre o Projeto

O **Sistema de Controle Financeiro** é o backend do meu projeto Full Stack. Desenvolvido em Java com Spring Boot, ele gerencia toda a lógica de gastos, salários e autenticação. Esta API está **100% online** e salva os dados de forma persistente no MySQL.

---

## ✨ Funcionalidades

- ✅ **Autenticação Segura:** Cadastro e login de usuários com JWT.
- ✅ **Recuperação de Senha:** Sistema de envio de código por e-mail para redefinição de senha.
- ✅ **Gestão Financeira:** Cadastro, listagem e exclusão de gastos e salários.
- ✅ **Inteligência Mensal:** Resumo com total de gastos, entradas, saldo e mensagem motivacional.
- ✅ **Persistência Real:** Dados salvos no MySQL do Railway.

---

## 🚀 Tecnologias Utilizadas

| Tecnologia | Descrição |
|---|---|
| Java 17 | Linguagem principal do projeto |
| Spring Boot 3.4.3 | Framework para a construção da API |
| Spring Security | Proteção de rotas e criptografia de senhas |
| JWT (jjwt) | Tokens seguros para manter o usuário logado |
| Spring Data JPA | Interface para comunicação com o banco de dados |
| MySQL | Banco de dados relacional (Produção) |
| JavaMailSender | Serviço para envio de e-mails de recuperação |
| Railway | Plataforma de Cloud para o Deploy do Backend |

---

## 📁 Estrutura de Pacotes

com.claudio.financeiro
├── controller      # Endpoints da API
├── service         # Regras de negócio
├── repository      # Acesso ao banco de dados MySQL
├── model           # Entidades JPA
├── dto             # Objetos de transferência de dados
└── config          # Configurações de segurança e CORS


---

## 🔗 Endpoints Principais

### 🔐 Autenticação
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/auth/registrar` | Cadastra novo usuário |
| POST | `/auth/login` | Realiza login e retorna token JWT |
| POST | `/auth/recuperar-senha` | Envia código de recuperação por email |

### 💸 Gestão (Protegido por JWT)
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/gastos` | Cadastra um novo gasto |
| GET | `/gastos` | Lista todos os gastos |
| GET | `/gastos/resumo` | Retorna o resumo mensal financeiro |

---

## 📦 Como Rodar o Projeto

### Pré-requisitos
- Java 17
- Maven

### Passos
```bash
# Clone o repositório
git clone [https://github.com/claudiondev/financeiro-backend](https://github.com/claudiondev/financeiro-backend)

# Entre na pasta e rode o projeto
./mvnw spring-boot:run
A API estará disponível em http://localhost:8080 ou pelo link de produção do Railway.

👨‍💻 Autor
Feito por Claudio Nascimento