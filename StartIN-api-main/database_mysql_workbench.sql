-- ============================================================================
-- STARTIN — ESTRUTURA PADRÃO DO BANCO DE DADOS (MYSQL 8.0+)
-- Schema oficial e padronizado para toda a plataforma StartIn:
--   1. users                   (Autenticação e Contas: STARTUP, COMPANY, PROVIDER, ADMIN)
--   2. startup_profiles        (Perfis completos de Startups - 6 etapas + atestação de tração)
--   3. companies               (Perfis de Empresas, Corporações, Investidores, Governos e Hubs)
--   4. service_providers       (Perfis completos de Prestadores de Serviço - 4 etapas)
--   5. provider_saved_startups (Startups de Tração favoritadas/salvas pelos Prestadores)
--   6. service_connections     (Solicitações e Conexões entre Startups e Prestadores)
--   7. opportunities           (Oportunidades de Empresas e Demandas de Serviço de Startups)
--   8. matches                 (Matchmaking IA entre Startups e Oportunidades)
--   9. notifications           (Notificações em tempo real para Startups, Empresas e Prestadores)
--  10. chat_messages           (Mensagens e anexos do Chat Contextual de Conexões e Matches)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS startin_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE startin_db;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 1. TABELA: users
-- ============================================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(180) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) DEFAULT NULL,
    cnpj VARCHAR(50) DEFAULT NULL,
    phone VARCHAR(50) DEFAULT NULL,
    avatar_url LONGTEXT DEFAULT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 2. TABELA: startup_profiles
-- ============================================================================
CREATE TABLE IF NOT EXISTS startup_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT DEFAULT NULL,

    -- Etapa 1: Identidade
    name VARCHAR(255) NOT NULL DEFAULT 'Minha Startup',
    main_area VARCHAR(150) DEFAULT NULL,
    description TEXT DEFAULT NULL,
    location VARCHAR(150) DEFAULT NULL,
    cnpj VARCHAR(30) DEFAULT NULL,
    phone VARCHAR(50) DEFAULT NULL,
    website VARCHAR(255) DEFAULT NULL,
    founded_year VARCHAR(20) DEFAULT NULL,

    -- Etapa 2: Solução
    problem TEXT DEFAULT NULL,
    how_it_works TEXT DEFAULT NULL,
    differentiator TEXT DEFAULT NULL,
    impact TEXT DEFAULT NULL,
    technologies TEXT DEFAULT NULL,
    business_model VARCHAR(150) DEFAULT NULL,

    -- Etapa 3: Mercado
    target_audience VARCHAR(255) DEFAULT NULL,
    main_sector VARCHAR(150) DEFAULT NULL,
    operation_region VARCHAR(150) DEFAULT NULL,
    state VARCHAR(100) DEFAULT NULL,
    customer_profile VARCHAR(255) DEFAULT NULL,
    market_size VARCHAR(150) DEFAULT NULL,
    competitors TEXT DEFAULT NULL,
    main_channel VARCHAR(150) DEFAULT NULL,

    -- Etapa 4: Momento & Atestação de Tração
    current_stage VARCHAR(100) DEFAULT NULL,
    traction_customers VARCHAR(150) DEFAULT NULL,
    product_maturity VARCHAR(150) DEFAULT NULL,
    team_size VARCHAR(100) DEFAULT NULL,
    annual_revenue VARCHAR(150) DEFAULT NULL,
    recent_growth VARCHAR(150) DEFAULT NULL,
    investment_raised VARCHAR(150) DEFAULT NULL,
    trl_score INT DEFAULT NULL,
    traction_attested BIT(1) DEFAULT b'0',
    traction_attestation_info TEXT DEFAULT NULL,
    cumulative_opportunities_found INT DEFAULT 0,
    last_update_new_matches_count INT DEFAULT 0,

    -- Etapa 5: Objetivos
    main_objective TEXT DEFAULT NULL,
    investment_need VARCHAR(150) DEFAULT NULL,
    execution_deadline VARCHAR(100) DEFAULT NULL,
    resource_use TEXT DEFAULT NULL,
    twelve_month_goal TEXT DEFAULT NULL,
    strategic_priority VARCHAR(255) DEFAULT NULL,
    desired_partners TEXT DEFAULT NULL,

    -- Etapa 6: Preferências de Matching
    areas_of_interest TEXT DEFAULT NULL,
    opportunity_types VARCHAR(255) DEFAULT NULL,
    receive_ai_recommendations BIT(1) DEFAULT b'1',
    email_notifications BIT(1) DEFAULT b'1',
    match_alerts BIT(1) DEFAULT b'1',

    avatar_url LONGTEXT DEFAULT NULL,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_startup_profiles_user (user_id),
    KEY idx_startup_profiles_stage (current_stage),
    KEY idx_startup_profiles_area (main_area),
    CONSTRAINT fk_startup_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 3. TABELA: companies
-- ============================================================================
CREATE TABLE IF NOT EXISTS companies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT DEFAULT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'EMPRESA',
    sector VARCHAR(150) DEFAULT NULL,
    location VARCHAR(150) DEFAULT NULL,
    cnpj VARCHAR(30) DEFAULT NULL,
    phone VARCHAR(50) DEFAULT NULL,
    website VARCHAR(255) DEFAULT NULL,
    description TEXT DEFAULT NULL,
    demands TEXT DEFAULT NULL,
    tags VARCHAR(500) DEFAULT NULL,
    preferred_stage VARCHAR(150) DEFAULT NULL,
    operation_mode VARCHAR(150) DEFAULT 'POC, Compra, POC + Compra',
    avatar_url LONGTEXT DEFAULT NULL,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_companies_user (user_id),
    KEY idx_companies_sector (sector),
    CONSTRAINT fk_companies_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 4. TABELA: service_providers
-- ============================================================================
CREATE TABLE IF NOT EXISTS service_providers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT DEFAULT NULL,

    -- Etapa 1: Identificação e Apresentação
    name VARCHAR(255) NOT NULL,
    provider_type VARCHAR(50) DEFAULT 'PROFISSIONAL',
    category VARCHAR(150) DEFAULT 'Design',
    description TEXT DEFAULT NULL,
    location VARCHAR(150) DEFAULT NULL,
    portfolio_url VARCHAR(500) DEFAULT NULL,
    linkedin_url VARCHAR(500) DEFAULT NULL,
    availability VARCHAR(150) DEFAULT 'Disponível imediatamente',
    avatar_url LONGTEXT DEFAULT NULL,

    -- Etapa 2: Serviços e Competências
    main_service TEXT DEFAULT NULL,
    service_experience TEXT DEFAULT NULL,
    tools_and_skills TEXT DEFAULT NULL,
    complementary_category VARCHAR(255) DEFAULT NULL,
    target_segment TEXT DEFAULT NULL,
    experience_years VARCHAR(100) DEFAULT NULL,

    -- Etapa 3: Portfólio e Casos de Sucesso
    project_name VARCHAR(255) DEFAULT NULL,
    project_link VARCHAR(500) DEFAULT NULL,
    project_description TEXT DEFAULT NULL,
    project_year VARCHAR(20) DEFAULT '2026',
    project_repo_url VARCHAR(500) DEFAULT NULL,
    project_tools TEXT DEFAULT NULL,
    project_type VARCHAR(150) DEFAULT NULL,
    project_cover_url LONGTEXT DEFAULT NULL,

    -- Etapa 4: Preferências de Parceria
    interest_sectors TEXT DEFAULT NULL,
    connection_preference TEXT DEFAULT NULL,
    opportunities_sought TEXT DEFAULT NULL,
    interest_region VARCHAR(150) DEFAULT 'Brasil',
    target_startup_size VARCHAR(150) DEFAULT 'Tração',
    communication_language VARCHAR(150) DEFAULT 'Português',

    tags TEXT DEFAULT NULL,
    saved_startup_ids TEXT DEFAULT NULL,
    match_score INT DEFAULT 90,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_service_providers_user (user_id),
    KEY idx_service_providers_category (category),
    CONSTRAINT fk_service_providers_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 5. TABELA: provider_saved_startups
-- ============================================================================
CREATE TABLE IF NOT EXISTS provider_saved_startups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider_id BIGINT NOT NULL,
    startup_id BIGINT NOT NULL,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_provider_saved_startup (provider_id, startup_id),
    KEY idx_pss_provider (provider_id),
    KEY idx_pss_startup (startup_id),
    CONSTRAINT fk_pss_provider FOREIGN KEY (provider_id) REFERENCES service_providers (id) ON DELETE CASCADE,
    CONSTRAINT fk_pss_startup FOREIGN KEY (startup_id) REFERENCES startup_profiles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 6. TABELA: service_connections
-- ============================================================================
CREATE TABLE IF NOT EXISTS service_connections (
    id BIGINT NOT NULL AUTO_INCREMENT,
    startup_id BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    sender_type VARCHAR(50) NOT NULL DEFAULT 'STARTUP',
    message TEXT DEFAULT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    match_score INT DEFAULT 90,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_service_connections_startup (startup_id),
    KEY idx_service_connections_provider (provider_id),
    KEY idx_service_connections_status (status),
    CONSTRAINT fk_service_connections_startup FOREIGN KEY (startup_id) REFERENCES startup_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_service_connections_provider FOREIGN KEY (provider_id) REFERENCES service_providers (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 7. TABELA: opportunities
-- ============================================================================
CREATE TABLE IF NOT EXISTS opportunities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(100) DEFAULT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'EDITAL',
    title VARCHAR(255) NOT NULL,
    short_description VARCHAR(500) DEFAULT NULL,
    description TEXT DEFAULT NULL,
    match_score INT DEFAULT 85,
    meta_info VARCHAR(255) DEFAULT NULL,
    tags VARCHAR(500) DEFAULT NULL,
    external_url VARCHAR(500) DEFAULT NULL,
    color VARCHAR(50) DEFAULT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PUBLICADA',
    company_id BIGINT DEFAULT NULL,
    startup_id BIGINT DEFAULT NULL,
    target_category VARCHAR(150) DEFAULT NULL,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_opportunities_company (company_id),
    KEY idx_opportunities_startup (startup_id),
    KEY idx_opportunities_type_status (type, status),
    CONSTRAINT fk_opportunities_company FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE SET NULL,
    CONSTRAINT fk_opportunities_startup FOREIGN KEY (startup_id) REFERENCES startup_profiles (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 8. TABELA: matches
-- ============================================================================
CREATE TABLE IF NOT EXISTS matches (
    id BIGINT NOT NULL AUTO_INCREMENT,
    startup_id BIGINT NOT NULL,
    opportunity_id BIGINT NOT NULL,
    match_score INT NOT NULL,
    justification TEXT DEFAULT NULL,
    recommended_action TEXT DEFAULT NULL,
    reason_profile TEXT DEFAULT NULL,
    reason_requirements TEXT DEFAULT NULL,
    reason_objective TEXT DEFAULT NULL,
    reason_context TEXT DEFAULT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NOVO',
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_matches_startup (startup_id),
    KEY idx_matches_opportunity (opportunity_id),
    KEY idx_matches_status (status),
    CONSTRAINT fk_matches_startup FOREIGN KEY (startup_id) REFERENCES startup_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_matches_opportunity FOREIGN KEY (opportunity_id) REFERENCES opportunities (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 9. TABELA: notifications
-- ============================================================================
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT DEFAULT NULL,
    time_ago VARCHAR(100) DEFAULT 'Agora',
    type VARCHAR(80) NOT NULL DEFAULT 'TODAS',
    read_status BIT(1) DEFAULT b'0',
    recipient_role VARCHAR(30) DEFAULT 'STARTUP',
    recipient_user_id BIGINT DEFAULT NULL,
    startup_id BIGINT DEFAULT NULL,
    company_id BIGINT DEFAULT NULL,
    provider_id BIGINT DEFAULT NULL,
    target_screen VARCHAR(80) DEFAULT NULL,
    target_id VARCHAR(80) DEFAULT NULL,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_notifications_recipient (recipient_user_id, recipient_role),
    KEY idx_notifications_startup (startup_id),
    KEY idx_notifications_company (company_id),
    KEY idx_notifications_provider (provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 10. TABELA: chat_messages
-- ============================================================================
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    context_type VARCHAR(40) NOT NULL,
    context_id BIGINT NOT NULL,
    sender_user_id BIGINT DEFAULT NULL,
    sender_role VARCHAR(40) NOT NULL,
    sender_name VARCHAR(255) DEFAULT NULL,
    content TEXT NOT NULL,
    attachment_name VARCHAR(255) DEFAULT NULL,
    attachment_type VARCHAR(120) DEFAULT NULL,
    attachment_data LONGTEXT DEFAULT NULL,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_chat_messages_context (context_type, context_id),
    KEY idx_chat_messages_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
