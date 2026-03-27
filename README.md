# 💰 Sistema de Controle Financeiro - API Backend

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-green?style=for-the-badge&logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6.4-green?style=for-the-badge&logo=springsecurity)
![JWT](https://img.shields.io/badge/JWT-Auth-black?style=for-the-badge&logo=jsonwebtokens)
![MySQL](https://img.shields.io/badge/MySQL-Database-blue?style=for-the-badge&logo=mysql)
![Railway](https://img.shields.io/badge/Railway-Deploy-black?style=for-the-badge&logo=railway)

> API REST para controle de gastos e salários com autenticação JWT, hospedada no Railway e conectada a um banco MySQL real.

---

## 🔗 Link do Deploy (API Online)
🚀 **Acesse o Backend aqui:** [https://financeiro-production-e0e0.up.railway.app](https://financeiro-production-e0e0.up.railway.app)
*(Nota: O endpoint principal retorna 403 por segurança, utilize os caminhos de `/auth` ou `/gastos` conforme a documentação abaixo).*

---

## 📋 Sobre o Projeto

O **Sistema de Controle Financeiro** é o backend do meu projeto Full Stack. Desenvolvido em Java com Spring Boot, ele gerencia toda a lógica de gastos, salários e autenticação. Esta API está **100% online** e salva os dados de forma persistente no MySQL do Railway.

---

## ✨ Funcionalidades

- ✅ **Autenticação Segura:** Cadastro e login de usuários com JWT.
- ✅ **Recuperação de Senha:** Sistema de envio de código por e-mail para redefinição de senha.
- ✅ **Gestão Financeira:** Cadastro, listagem e exclusão de gastos e salários.
- ✅ **Inteligência Mensal:** Resumo com total de gastos, entradas, saldo e mensagem motivacional.
- ✅ **Persistência Real:** Dados salvos no MySQL (nada de perder tudo ao dar F5).

---

## 🚀 Tecnologias Utilizadas

| Tecnologia | Descrição |
|---|---|
| Java 17 | Linguagem principal do projeto |
| Spring Boot 3.4.3 | Framework para a construção da API |
| Spring Security | Proteção de rotas e criptografia de senhas |
| JWT (jjwt) | Tokens seguros para manter o usuário logado |
| Spring Data JPA | Interface para comunicação com o banco de dados |
| MySQL | Banco de dados relacional oficial do projeto |
| JavaMailSender | Serviço para envio de e-mails de recuperação |
| Railway | Plataforma de Cloud para o Deploy do Backend |

---

## 📁 Estrutura de Pacotes
