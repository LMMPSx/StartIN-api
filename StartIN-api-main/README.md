# StartIn Backend API — Spring Boot 3 & MySQL

Backend RESTful corporativo desenvolvido em **Java 21** e **Spring Boot 3.3.4** para a plataforma **StartIn**, responsável pelo gerenciamento de usuários, perfis de Startups e Empresas, catálogo de oportunidades (editais, POCs, compras corporativas), orquestração de matchmaking e integração síncrona/assíncrona com o microserviço de **Inteligência Artificial (FastAPI + Groq LLM)** e persistência em **MySQL**.

---

## 🏛 Arquitetura do Sistema

A API segue o padrão de arquitetura em camadas do Spring Framework:

```
StartIN-api-main/
├── src/main/java/com/startin/api/
│   ├── StartInApplication.java            # Ponto de entrada do Spring Boot
│   ├── config/
│   │   ├── AiClientConfig.java            # Configuração do RestTemplate para o serviço de IA
│   │   ├── CorsConfig.java                # CORS liberado para o front (localhost:3000 e 5173)
│   │   ├── DataInitializer.java          # Carga inicial e sincronização de dados
│   │   └── OpenApiConfig.java             # Documentação OpenAPI 3 / Swagger
│   ├── controller/
│   │   ├── AuthController.java            # /api/auth (login, signup, reset de senha)
│   │   ├── StartupProfileController.java  # /api/startups (onboarding 6 etapas, preferências)
│   │   ├── CompanyController.java         # /api/companies (perfil de empresas contratantes)
│   │   ├── OpportunityController.java     # /api/opportunities (CRUD de editais, POCs, compras)
│   │   ├── MatchController.java           # /api/matches (gestão e status de matches)
│   │   ├── DashboardController.java       # /api/dashboard (métricas consolidadas e KPIs)
│   │   ├── NotificationController.java    # /api/notifications (alertas de prazos e novidades)
│   │   └── AiController.java              # /api/ai (chat, trigger de sincronização diária)
│   ├── dto/                               # DTOs de Request, Response e integração de IA
│   ├── model/                             # Entidades JPA (User, StartupProfile, Company, Opportunity, MatchResult...)
│   ├── repository/                        # Interfaces Spring Data JPA
│   └── service/                           # Regras de negócio e comunicação com IA Python
├── database_mysql_workbench.sql           # Script DDL/DML completo para MySQL 8
├── start_backend.bat                      # Inicializador rápido para ambiente Windows
├── pom.xml                                # Dependências Maven
└── application.properties                 # Configurações de conexão e profiles
```

---

## 🛠 Requisitos de Ambiente

- **Java JDK**: Versão 21 ou superior (ex: OpenJDK / Oracle JDK 21).
- **Apache Maven**: 3.8+ (ou utilize o wrapper `./mvnw` incluso).
- **MySQL Server**: 8.0+ em execução local na porta `3306`.
- **Microserviço de IA Python**: Executando na porta `8000` (diretório `StartIN-IA-main`).

---

## 🗄 Configuração do Banco de Dados (MySQL)

1. Certifique-se de que o serviço do MySQL está ativo na porta padrão `3306`.
2. Abra o **MySQL Workbench** ou seu terminal MySQL preferido.
3. Execute o script contido na raiz do projeto:
   ```sql
   source C:/Users/vinic/Downloads/StartIn/StartIN-api-main/database_mysql_workbench.sql;
   ```
   *Esse script cria o schema `startin_db`, todas as tabelas necessárias e popula os dados iniciais de demonstração.*
4. As credenciais configuradas em `src/main/resources/application.properties` são:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/startin_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   spring.datasource.username=root
   spring.datasource.password=Vinicius145060@
   spring.jpa.hibernate.ddl-auto=update
   ```

---

## 🚀 Como Executar

### Opção A: Script Rápido (Recomendado no Windows)
Basta clicar duas vezes ou rodar via terminal:
```powershell
.\start_backend.bat
```

### Opção B: Via Maven Wrapper
```powershell
.\mvnw clean spring-boot:run
```

A API estará disponível em: **`http://localhost:8080`**.

---

## 📡 Principais Endpoints da API

### 1. Autenticação (`/api/auth`)
- `POST /api/auth/signup`: Cadastro de novos usuários segregando o papel (`STARTUP` ou `COMPANY`), além de nome, e-mail, senha e telefone.
- `POST /api/auth/login`: Autenticação e retorno do token/sessão do usuário.
- `POST /api/auth/forgot-password` e `reset-password`: Recuperação segura de senha.

### 2. Startups & Onboarding (`/api/startups`)
- `GET /api/startups`: Lista de todas as startups cadastradas para a busca pública.
- `GET /api/startups/{id}`: Obter detalhes do perfil da startup.
- `POST /api/startups/onboarding`: Salvar ou atualizar as 6 etapas do perfil (Identidade, Solução, Mercado, Momento/Tração, Objetivos e Preferências).
- `PUT /api/startups/{id}/avatar`: Atualizar a foto de perfil exclusiva da startup.

### 3. Empresas (`/api/companies`)
- `GET /api/companies`: Lista de empresas contratantes cadastradas para a busca pública.
- `GET /api/companies/{id}`: Detalhes da empresa (resumo de atuação, contatos, foto, tags).
- `POST /api/companies`: Cadastro ou atualização da empresa.
- `PUT /api/companies/{id}/avatar`: Atualizar a foto de perfil exclusiva da empresa.

### 4. Oportunidades (`/api/opportunities`)
- `GET /api/opportunities`: Consulta todas as oportunidades (editais públicos raspados pela IA, chamadas de POC e compras corporativas).
- `POST /api/opportunities`: Criação de oportunidade (exclusivo para empresas no fluxo segregado).
- `GET /api/opportunities/{id}`: Detalhes de um edital ou oportunidade.

### 5. Matches Inteligentes (`/api/matches`)
- `GET /api/matches/startup/{startupId}`: Retorna matches calculados pela IA com notas nos 4 pilares (`perfil`, `requisitos`, `objetivo`, `contexto`).
- `PUT /api/matches/{id}/status`: Atualiza o status do match (Interesse, Descartado, Salvo).
- `DELETE /api/matches/{id}`: Remove ou descarta uma oportunidade salva.

### 6. Sincronização Diária de Editais (`/api/ai/daily-sync`)
- `POST /api/ai/daily-sync`: Endpoint que recebe os novos editais raspados e os matches calculados pelo script das 08:00 do microserviço de IA, persistindo de forma transacional no MySQL.

---

## 📖 Documentação Interativa (Swagger / OpenAPI)

Com o backend em execução, acesse no navegador:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Especificação OpenAPI JSON**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

---

## ⚙ Configuração de CORS
O CORS está pré-configurado em `CorsConfig.java` para aceitar requisições de origens locais de desenvolvimento:
- `http://localhost:3000` (porta padrão do Vite Frontend)
- `http://localhost:5173` (porta alternativa do Vite)
- Headers: `Authorization`, `Content-Type`, `Accept`.
- Métodos: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, `PATCH`.
