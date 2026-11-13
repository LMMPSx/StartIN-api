# StartIn Backend API - Spring Boot 3, Java 21 & MySQL

Backend RESTful desenvolvido em Java 21 e Spring Boot 3.3.4 para a plataforma StartIn, responsavel pelo gerenciamento de usuarios, controle de planos (Free e Pro), perfis de Startups, Empresas e Prestadores de Servicos, catalogo de oportunidades corporativas e demandas tecnicas, moderacao administrativa (`ADMIN`), orquestracao de matchmaking simetrico e integracao com o microservico de Inteligencia Artificial (FastAPI + Groq LLM) e persistencia em MySQL.

---

## 1. Arquitetura do Sistema

A API segue o padrao de arquitetura em camadas do Spring Framework:

```text
StartIN-api-main/
|-- src/main/java/com/startin/api/
|   |-- StartInApplication.java              # Ponto de entrada do Spring Boot
|   |-- config/
|   |   |-- AiClientConfig.java              # Configuracao do RestTemplate para o servico de IA
|   |   |-- CorsConfig.java                  # CORS liberado para o frontend (localhost:3000 e 5173)
|   |   |-- DataInitializer.java             # Carga inicial, conta admin e sincronizacao de dados
|   |   `-- OpenApiConfig.java               # Documentacao OpenAPI 3 / Swagger
|   |-- controller/
|   |   |-- AuthController.java              # /api/auth (login, signup, upgrade de plano, admin)
|   |   |-- AdminController.java             # /api/admin (moderacao geral de usuarios e oportunidades)
|   |   |-- StartupProfileController.java    # /api/startups (onboarding, preferencias e estagio TRL)
|   |   |-- CompanyController.java           # /api/companies (perfil de empresas e matches)
|   |   |-- ServiceProviderController.java   # /api/providers (prestadores, demandas e conexoes)
|   |   |-- OpportunityController.java       # /api/opportunities (CRUD de editais, POCs, compras, servicos)
|   |   |-- MatchController.java             # /api/matches (gestao e status de matches)
|   |   |-- DashboardController.java         # /api/dashboard (metricas consolidadas e KPIs)
|   |   |-- NotificationController.java      # /api/notifications (alertas de prazos e conexoes)
|   |   `-- AiController.java                # /api/ai (chat, extracao de curriculo PDF, TRL PDF, sync)
|   |-- dto/                                 # DTOs de Request, Response e integracao de IA
|   |-- model/                               # Entidades JPA (User, StartupProfile, Company, ServiceProvider, Opportunity...)
|   |-- repository/                          # Interfaces Spring Data JPA
|   `-- service/                             # Regras de negocio, limites mensais Free/Pro e integracao IA
|-- database_mysql_workbench.sql             # Script DDL/DML para MySQL 8
|-- start_backend.bat                        # Inicializador rapido para ambiente Windows
|-- pom.xml                                  # Dependencias Maven
`-- src/main/resources/application.properties# Configuracoes de conexao MySQL e porta 8080
```

---

## 2. Requisitos de Ambiente

- Java JDK: Versao 21 ou superior.
- Apache Maven: 3.8+ (ou utilize o wrapper `.\mvnw.cmd` incluso).
- MySQL Server: 8.0+ em execucao local na porta `3306`.
- Microservico de IA Python: Executando na porta `8000` (diretorio `StartIN-IA-main`).

---

## 3. Configuracao do Banco de Dados (MySQL)

1. Certifique-se de que o servico do MySQL esta ativo na porta padrao `3306`.
2. Crie o banco `startin_db` caso ainda nao exista:
   ```sql
   CREATE DATABASE IF NOT EXISTS startin_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. As credenciais vem de variaveis de ambiente (`MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, `MYSQLDATABASE`). Localmente, crie `local.properties` na raiz deste modulo (ignorado pelo Git):
   ```properties
   MYSQLUSER=root
   MYSQLPASSWORD=sua_senha_local
   ```
   Sem variaveis definidas, o backend usa `localhost:3306/startin_db` e `ddl-auto=update`.

4. Deploy no Railway: `Dockerfile` + `railway.json` (healthcheck em `/api/health`). Veja o passo a passo em [../DEPLOY.md](../DEPLOY.md).

---

## 4. Como Executar

### Opcao A: Script Rapido (Windows)
```powershell
.\start_backend.bat
```

### Opcao B: Via Maven Wrapper
```powershell
.\mvnw.cmd spring-boot:run
```

Para apenas compilar e validar o codigo sem executar os testes:
```powershell
.\mvnw.cmd compile -DskipTests
```

A API estara disponivel em `http://localhost:8080`.
Caso a porta `8080` ja esteja em uso por uma instancia anterior, encerre o processo ativo na porta 8080 antes de iniciar uma nova instancia.

---

## 5. Regras de Negocio e Alteracoes Recentes no Backend

### 5.1. Controle de Planos (`FREE` vs `PRO`) e Limites Mensais de Oportunidades
- Cada entidade `User` possui o campo `plan` (`FREE` ou `PRO`), retornado na autenticacao e sincronizado em todas as listagens.
- **Limites Mensais de Lancamento (`OpportunityService.createOpportunity`)**:
  - Usuarios no **Plano Free** podem lancar ate **5 oportunidades por mes** (valido tanto para Empresas contratantes quanto para Startups em Tracao que lancam demandas de servico).
  - Usuarios no **Plano Pro** podem lancar ate **15 oportunidades por mes**, alem de receberem a flag `proMember = true` para impulsionamento automatico no topo das listagens.

### 5.2. Extracao de Curriculo em PDF para Prestadores (`POST /api/ai/extract-provider-resume`)
- Novo endpoint em `AiController` e metodo `extractProviderResume` em `AiIntegrationService` que encaminha o PDF anexado no Passo 1 do onboarding do prestador ao microservico Python de IA (com fallback deterministico local em Java caso o servico Python esteja offline).
- Extrai `category`, `mainService`, `toolsAndSkills`, `tags`, `experienceYears`, `location`, `projectName`, `interestSectors`, `connectionPreference` e gera a `description` profissional previa em 1a pessoa.

### 5.3. Consistencia e Simetria Bidirecional dos Scores de Match
- Os metodos de compatibilidade e fallback em `AiIntegrationService` e `ServiceProviderService` foram unificados sob a mesma regra deterministica do motor Python (`40% Area + 30% Tecnologias + 30% Contexto/Maturidade`), garantindo que o percentual de match entre `Empresa <-> Startup` e `Startup em Tracao <-> Prestador de Servicos / Demandas` seja identico em ambos os sentidos e consistente com o texto da justificativa.

### 5.4. Modulo Administrativo Geral (`AdminController` - `/api/admin`)
- Conta administrativa semeada automaticamente (`user: admin` / `password: 0000` com papel `ADMIN` e plano `PRO`).
- Endpoints dedicados para listagem de metricas globais (`GET /api/admin/overview`), edicao/alteracao de plano e exclusao de usuarios (`PUT /api/admin/users/{id}`, `DELETE /api/admin/users/{id}`), moderacao e exclusao de oportunidades e demandas (`PUT /api/admin/opportunities/{id}`, `DELETE /api/admin/opportunities/{id}`).

---

## 6. Principais Endpoints da API

- **Autenticacao (`/api/auth`)**: `POST /api/auth/login`, `POST /api/auth/signup`, `PUT /api/auth/users/{id}/plan`.
- **Administracao (`/api/admin`)**: `GET /api/admin/overview`, `GET /api/admin/users`, `PUT /api/admin/users/{id}`, `DELETE /api/admin/users/{id}`, `PUT /api/admin/opportunities/{id}`, `DELETE /api/admin/opportunities/{id}`.
- **Startups (`/api/startups`)**: `GET /api/startups`, `GET /api/startups/{id}`, `PUT /api/startups/{id}`, `PUT /api/startups/{id}/preferences`.
- **Empresas (`/api/companies`)**: `GET /api/companies`, `GET /api/companies/{id}`, `POST /api/companies`, `PUT /api/companies/{id}`, `GET /api/companies/{id}/matching-startups`.
- **Prestadores e Conexoes (`/api/providers`)**: `GET /api/providers`, `POST /api/providers`, `PUT /api/providers/{id}`, `GET /api/providers/{id}/recommendations`, `POST /api/providers/connections`, `DELETE /api/providers/connections/{id}`.
- **Oportunidades (`/api/opportunities`)**: `GET /api/opportunities`, `POST /api/opportunities`, `PUT /api/opportunities/{id}`, `DELETE /api/opportunities/{id}`.
- **Inteligencia Artificial (`/api/ai`)**:
  - `POST /api/ai/chat`: Assistente IA (Chatbot Pro) com suporte a estilo de template personalizado.
  - `POST /api/ai/extract-provider-resume`: Leitura e extracao estruturada de curriculo em PDF do prestador.
  - `POST /api/ai/enhance-provider-services`: Aprimoramento coeso em 1a pessoa da descricao profissional do prestador.
  - `POST /api/ai/enhance-problem`: Estruturacao de problema/solucao de startups e demandas.
  - `POST /api/ai/enhance-company-demands`: Estruturacao executiva de demandas corporativas.
  - `POST /api/ai/verify-traction-pdf`: Auditoria de Atestado de Capacidade Tecnica em PDF aliado ao TRL da NASA.
  - `POST /api/ai/match`: Execucao do matchmaking auditado.

---

## 7. Documentacao Interativa (Swagger / OpenAPI)

Com o backend em execucao, acesse no navegador:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Especificacao OpenAPI JSON: `http://localhost:8080/api-docs`
