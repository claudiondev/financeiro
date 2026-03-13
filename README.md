# 💰 Sistema de Controle Financeiro

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green?style=for-the-badge&logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6.5-green?style=for-the-badge&logo=springsecurity)
![JWT](https://img.shields.io/badge/JWT-Auth-black?style=for-the-badge&logo=jsonwebtokens)
![Maven](https://img.shields.io/badge/Maven-Build-red?style=for-the-badge&logo=apachemaven)
![H2](https://img.shields.io/badge/H2-Database-blue?style=for-the-badge)

> API REST para controle de gastos e salários com autenticação JWT e recuperação de senha por email.

---

## 📋 Sobre o Projeto

O **Sistema de Controle Financeiro** é uma API backend desenvolvida em Java com Spring Boot, que permite ao usuário registrar seus gastos e salários, visualizar um resumo mensal com saldo e ainda conta com um sistema completo de autenticação com recuperação de senha por email.

---

## ✨ Funcionalidades

- ✅ Cadastro e login de usuários com JWT
- ✅ Recuperação de senha por email com código de verificação
- ✅ Cadastro, listagem e exclusão de gastos
- ✅ Cadastro, listagem e exclusão de salários
- ✅ Resumo mensal com total de gastos, salários e saldo
- ✅ Mensagem motivacional baseada no saldo do mês

---

## 🚀 Tecnologias Utilizadas

| Tecnologia | Descrição |
|---|---|
| Java 17 | Linguagem principal |
| Spring Boot 3.5 | Framework principal |
| Spring Security | Autenticação e autorização |
| JWT (jjwt 0.11.5) | Geração e validação de tokens |
| Spring Data JPA | Persistência de dados |
| H2 Database | Banco de dados em memória |
| JavaMailSender | Envio de emails |
| Maven | Gerenciador de dependências |

---

## 📁 Estrutura de Pacotes

```
com.claudio.financeiro
├── controller      # Endpoints da API
├── service         # Regras de negócio
├── repository      # Acesso ao banco de dados
├── model           # Entidades JPA
├── dto             # Objetos de transferência de dados
└── config          # Configurações de segurança
```

---

## 🔗 Endpoints

### 🔐 Autenticação
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/auth/registrar` | Cadastra novo usuário |
| POST | `/auth/login` | Realiza login e retorna token JWT |
| POST | `/auth/recuperar-senha` | Envia código de recuperação por email |
| POST | `/auth/redefinir-senha` | Redefine a senha com o código recebido |

### 💸 Gastos
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/gastos` | Cadastra um novo gasto |
| GET | `/gastos` | Lista todos os gastos |
| DELETE | `/gastos/{id}` | Remove um gasto |
| GET | `/gastos/resumo` | Retorna o resumo mensal |

### 💰 Salários
| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/salario` | Cadastra um novo salário |
| GET | `/salario` | Lista todos os salários |
| DELETE | `/salario/{id}` | Remove um salário |

---

## 📦 Como Rodar o Projeto

### Pré-requisitos
- Java 17+
- Maven

### Passos

```bash
# Clone o repositório
git clone https://github.com/claudiondev/financeiro

# Entre na pasta do projeto
cd financeiro

# Rode o projeto
./mvnw spring-boot:run
```

A API estará disponível em `http://localhost:8080`

---

## 🔒 Autenticação

A API utiliza **JWT (JSON Web Token)**. Após o login, você receberá um token que deve ser enviado no header de todas as requisições protegidas:

```
Authorization: Bearer {seu_token}
```

---

## 📧 Recuperação de Senha

1. Envie seu email para `/auth/recuperar-senha`
2. Você receberá um código de 6 dígitos no email
3. Use o código em `/auth/redefinir-senha` junto com a nova senha

---

## 👨‍💻 Autor

Feito com 💙 por **Claudio Nascimento**

[![GitHub](https://img.shields.io/badge/GitHub-claudiondev-black?style=for-the-badge&logo=github)](https://github.com/claudiondev)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Claudio%20Nascimento-blue?style=for-the-badge&logo=linkedin)](https://www.linkedin.com/in/claudionascimento-dev/)