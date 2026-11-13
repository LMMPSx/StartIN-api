# Startup Hub API - Backend

API RESTful desenvolvida em Java com Spring Boot para a gestão e registo de startups. O projeto disponibiliza um CRUD completo com validações de regras de negócio, persistência de dados e arquitetura em camadas.

---

## Tecnologias Utilizadas

* Java 17
* Spring Boot 3.x
* Spring Data JPA (Persistência de Dados)
* MySQL / H2 Database (Base de dados)
* Lombok (Produtividade e redução de código boilerplate)
* Maven (Gestão de dependências e build)

---

##  Estrutura do Projeto

src/main/java/com/api/backend/
├── StartupHubApplication.java      # Classe principal de inicialização
├── controller/                      # Controladores REST (End-points HTTP)
├── dto/                             # Data Transfer Objects (Request e Response)
├── model/                           # Entidades JPA de Mapeamento O/R
├── repository/                      # Interfaces Spring Data JPA
├── service/                         # Camada de Regras de Negócio
└── utils/ / config/                 # Configurações gerais e utilitários

---

##  Como Executar a Aplicação

### Pré-requisitos
* JDK 17 ou superior instalado.
* Maven instalado (ou utilizar o wrapper ./mvnw).
* MySQL a rodar na porta 3306 (ou base de dados H2 configurada).

### Passos de Instalação

1. Clonar o repositório:
   git clone <URL_DO_SEU_REPOSITORIO>
   cd backendd-crud-simples

2. Configurar a Base de Dados:
   No ficheiro src/main/resources/application.properties, ajuste os dados de conexão com o seu MySQL:
   
   spring.datasource.url=jdbc:mysql://localhost:3306/SEU_BANCO?useSSL=false&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=SUA_SENHA
   spring.jpa.hibernate.ddl-auto=update

3. Executar o projeto:
   Via terminal:
   ./mvnw spring-boot:run

   Ou executando a classe StartupHubApplication.java diretamente na sua IDE.

4. O servidor estará ativo no endereço: http://localhost:8080

---

##  Documentação dos Endpoints (Rotas da API)

Base URL: /api/startups

* POST   /api/startups      - Regista uma nova startup (Status: 201 Created)
* GET    /api/startups      - Lista todas as startups cadastradas (Status: 200 OK)
* GET    /api/startups/{id} - Procura uma startup por ID (Status: 200 OK)
* PUT    /api/startups/{id} - Atualiza os dados de uma startup (Status: 200 OK)
* DELETE /api/startups/{id} - Remove uma startup por ID (Status: 204 No Content)
