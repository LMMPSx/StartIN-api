# StartIn Backend API — Java 21 + Spring Boot 3

Backend RESTful desenvolvido em **Java 21** e **Spring Boot 3.3.4** para a plataforma **StartIn**, com arquitetura em camadas (`Model`, `DTO`, `Service`, `Controller`, `Repository`) e integração direta em tempo real com o microserviço de **Inteligência Artificial em Python (Groq LLM + Web Scraping)**.

---

## 🏛 Arquitetura do Projeto

```
com.startin.api
├── StartInApplication.java          # Classe principal do Spring Boot
├── config/
│   ├── AiClientConfig.java          # Configuração do cliente HTTP para a IA
│   ├── CorsConfig.java              # Liberação CORS para React/Vite (localhost:5173 / 3000)
│   ├── DataInitializer.java        # Inicialização automática de dados compatíveis com o mockup
│   └── OpenApiConfig.java           # Documentação Swagger OpenAPI 3
├── controller/
│   ├── AuthController.java          # /api/auth (login, signup, reset-password)
│   ├── StartupProfileController.java# /api/startups (onboarding, perfil, preferências)
│   ├── OpportunityController.java   # /api/opportunities (editais, POCs, eventos, compras)
│   ├── MatchController.java         # /api/matches (matchmaking, atualização de status)
│   ├── CompanyController.java       # /api/companies (empresas e organizações)
│   ├── NotificationController.java  # /api/notifications (alertas de prazos e matches)
│   ├── DashboardController.java     # /api/dashboard (indicadores, prazos e métricas)
│   └── AiController.java            # /api/ai (assistente virtual, chat e recalculação)
├── dto/
│   ├── ai/                          # DTOs de comunicação com o Python (Match, Scrape, Chat)
│   ├── auth/                        # LoginRequest, SignupRequest, AuthResponse
│   ├── company/                     # CompanyDTO
│   ├── dashboard/                   # DashboardSummaryDTO
│   ├── match/                       # MatchResultDTO, UpdateStatusDTO
│   ├── notification/                # NotificationDTO
│   ├── opportunity/                 # OpportunityDTO, CreateOpportunityDTO
│   └── startup/                     # StartupProfileDTO, MatchingPreferencesDTO
├── model/
│   ├── User.java                    # Usuários da plataforma
│   ├── StartupProfile.java          # Perfil completo (6 etapas do Onboarding)
│   ├── Opportunity.java             # Editais, POCs, Eventos e Compras
│   ├── Company.java                 # Organizações contratantes / ofertantes
│   ├── MatchResult.java             # Resultados de compatibilidade com os 4 pilares
│   ├── Notification.java            # Notificações e avisos
│   ├── ChatMessage.java             # Histórico de conversas com o Assistente IA
│   └── enums/                       # OpportunityType, Status, MatchStatus, UserRole
├── repository/                      # 7 Repositórios Spring Data JPA
└── service/
    ├── AiIntegrationService.java    # Comunicação HTTP RestTemplate com IA Python (Porta 8000)
    ├── AuthService.java             # Lógica de cadastro e autenticação
    ├── StartupProfileService.java   # Gestão do perfil e preferências da startup
    ├── OpportunityService.java      # Busca e criação de oportunidades
    ├── MatchService.java            # Orquestração do matchmaking e pontuações
    ├── CompanyService.java          # Cadastro de empresas
    ├── NotificationService.java     # Gestão de notificações
    └── DashboardService.java        # Cálculo de métricas e KPIs
```

---

## 🤖 Integração com a IA em Python

1. **Envio de Dados**: O backend Spring Boot extrai todos os dados declarados pela startup (Setor, Tecnologias, Modelo de Negócio, Problema, Estágio, Tags) e a lista de oportunidades do banco.
2. **Processamento via IA**: Envia via `POST http://localhost:8000/api/ai/match`.
3. **Retorno Estruturado**: O Python processa no modelo Groq LLM (`openai/gpt-oss-20b` ou fallback semântico de alta disponibilidade) e retorna:
   - Score numérico de aderência (0 a 100);
   - Justificativa detalhada;
   - Ação recomendada para a startup;
   - Os 4 pilares de análise da UI: `perfil`, `requisitos`, `objetivo`, `contexto`.
4. **Persistência e Cache**: O Spring Boot atualiza os registros de `matches` e a pontuação nas `opportunities`.

---

## 🚀 Como Executar

### Pré-requisitos
- **Java 21**
- **Python 3.10+** (com dependências instaladas em `StartIN-IA-main`)

### Passo 1: Iniciar a IA em Python
```bash
cd StartIN-IA-main
python server.py
# Rodando em http://localhost:8000
```

### Passo 2: Iniciar o Backend Spring Boot
```bash
cd StartIN-api-main
# Opção A: Executar script pronto
start_backend.bat

# Opção B: Via JAR gerado
java -jar target/startin-api-1.0.0.jar
```

---

## 📖 Documentação e Swagger UI

- **Swagger UI interativo**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)
- **H2 Web Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:file:./data/startindb`, User: `sa`, Senha: em branco)
