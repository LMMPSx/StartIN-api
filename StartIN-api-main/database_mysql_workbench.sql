-- =============================================================================
-- STARTIN - SCRIPT DE CRIAÇÃO DO BANCO DE DADOS PARA MYSQL WORKBENCH (MySQL 8+)
-- =============================================================================

-- 1. Criação do Schema (Banco de Dados)
CREATE DATABASE IF NOT EXISTS startin_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE startin_db;

-- =============================================================================
-- 2. TABELA DE USUÁRIOS (users)
-- =============================================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(180) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) DEFAULT NULL,
    cnpj VARCHAR(30) DEFAULT NULL,
    avatar_url LONGTEXT DEFAULT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- 3. TABELA DE PERFIS DE STARTUP (startup_profiles)
-- Campos descritivos longos utilizam TEXT para respeitar o limite de 65535 bytes do InnoDB utf8mb4
-- =============================================================================
CREATE TABLE IF NOT EXISTS startup_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT DEFAULT NULL,

    -- Etapa 01: Identidade
    name VARCHAR(255) NOT NULL,
    main_area VARCHAR(150) DEFAULT NULL,
    description TEXT DEFAULT NULL,
    location VARCHAR(150) DEFAULT NULL,
    cnpj VARCHAR(30) DEFAULT NULL,
    website VARCHAR(255) DEFAULT NULL,
    founded_year VARCHAR(20) DEFAULT NULL,

    -- Etapa 02: Solução
    problem TEXT DEFAULT NULL,
    how_it_works TEXT DEFAULT NULL,
    differentiator TEXT DEFAULT NULL,
    impact TEXT DEFAULT NULL,
    technologies TEXT DEFAULT NULL,
    business_model VARCHAR(150) DEFAULT NULL,

    -- Etapa 03: Mercado
    target_audience VARCHAR(255) DEFAULT NULL,
    main_sector VARCHAR(150) DEFAULT NULL,
    operation_region VARCHAR(150) DEFAULT NULL,
    state VARCHAR(100) DEFAULT NULL,
    customer_profile VARCHAR(255) DEFAULT NULL,
    market_size VARCHAR(150) DEFAULT NULL,
    competitors TEXT DEFAULT NULL,
    main_channel VARCHAR(150) DEFAULT NULL,

    -- Etapa 04: Momento e Tração
    current_stage VARCHAR(100) DEFAULT NULL,
    traction_customers VARCHAR(150) DEFAULT NULL,
    product_maturity VARCHAR(150) DEFAULT NULL,
    team_size VARCHAR(100) DEFAULT NULL,
    annual_revenue VARCHAR(150) DEFAULT NULL,
    recent_growth VARCHAR(150) DEFAULT NULL,
    investment_raised VARCHAR(150) DEFAULT NULL,

    -- Etapa 05: Objetivos
    main_objective TEXT DEFAULT NULL,
    investment_need VARCHAR(150) DEFAULT NULL,
    execution_deadline VARCHAR(100) DEFAULT NULL,
    resource_use TEXT DEFAULT NULL,
    twelve_month_goal TEXT DEFAULT NULL,
    strategic_priority VARCHAR(255) DEFAULT NULL,
    desired_partners TEXT DEFAULT NULL,

    -- Etapa 06: Preferências de Matching da IA
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
    CONSTRAINT fk_startup_profiles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- 4. TABELA DE EMPRESAS E ORGANIZAÇÕES PARCEIRAS (companies)
-- =============================================================================
CREATE TABLE IF NOT EXISTS companies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'EMPRESA',
    description TEXT DEFAULT NULL,
    tags VARCHAR(500) DEFAULT NULL,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- 5. TABELA DE OPORTUNIDADES: EDITAIS, POCs, EVENTOS E COMPRAS (opportunities)
-- =============================================================================
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
    color VARCHAR(120) DEFAULT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PUBLICADA',
    company_id BIGINT DEFAULT NULL,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_opportunities_type (type),
    KEY idx_opportunities_code (code),
    CONSTRAINT fk_opportunities_company FOREIGN KEY (company_id)
        REFERENCES companies (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- 6. TABELA DE MATCHES CALCULADOS PELA IA PYTHON (matches)
-- =============================================================================
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
    UNIQUE KEY uk_matches_startup_opportunity (startup_id, opportunity_id),
    KEY idx_matches_score (match_score DESC),
    CONSTRAINT fk_matches_startup FOREIGN KEY (startup_id)
        REFERENCES startup_profiles (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_matches_opportunity FOREIGN KEY (opportunity_id)
        REFERENCES opportunities (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- 7. TABELA DE NOTIFICAÇÕES (notifications)
-- =============================================================================
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT DEFAULT NULL,
    time_ago VARCHAR(100) DEFAULT NULL,
    type VARCHAR(50) DEFAULT 'TODAS',
    read_status BIT(1) DEFAULT b'0',
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_notifications_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- 8. USUÁRIO DE TESTE (admin / 0000) E PERFIL VINCULADO
-- =============================================================================
INSERT INTO users (email, password, name, cnpj, role)
SELECT 'admin', '0000', 'Administrador', '00.000.000/0001-00', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin');

INSERT INTO startup_profiles (user_id, name, cnpj, receive_ai_recommendations)
SELECT id, 'Startup Admin', '00.000.000/0001-00', b'1'
FROM users WHERE email = 'admin'
AND NOT EXISTS (SELECT 1 FROM startup_profiles WHERE user_id = (SELECT id FROM users WHERE email = 'admin' LIMIT 1));

-- =============================================================================
-- 9. CARGA INICIAL DE EMPRESAS E OPORTUNIDADES PARA O MATCHMAKING DA IA
-- =============================================================================
INSERT INTO companies (name, type, description, tags)
SELECT * FROM (
    SELECT 'Empresa Alpha' AS name, 'EMPRESA' AS type,
           'Grande conglomerado em busca de inovação aberta, inteligência operacional e automação de processos.' AS description,
           'IA, SaaS, B2B, Dados, Automação' AS tags
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM companies WHERE name = 'Empresa Alpha') LIMIT 1;

INSERT INTO opportunities (code, type, title, short_description, description, match_score, meta_info, tags, external_url, color, status, company_id)
SELECT 'edital', 'EDITAL', 'Edital Inovação Aberta 2026',
       'Apoio financeiro para startups com soluções B2B.',
       'Programa de inovação aberta para startups com soluções em dados, tecnologia aplicada e automação por IA. A oportunidade busca aplicações com potencial de validação em ambiente real e escalabilidade nacional.',
       90, 'Prazo 15/10/2026 · Brasil · Remoto', 'IA • Dados • Automação • SaaS B2B',
       'https://inovastartups.com.br/', 'bg-[#fff2b8] text-[#795900]', 'PUBLICADA', NULL
WHERE NOT EXISTS (SELECT 1 FROM opportunities WHERE code = 'edital');

INSERT INTO opportunities (code, type, title, short_description, description, match_score, meta_info, tags, external_url, color, status, company_id)
SELECT 'poc', 'POC', 'POC — Inteligência Operacional',
       'Empresa procura solução de IA para operação.',
       'Empresa Alpha busca startup para automatizar processos internos e testar inteligência operacional em um ambiente real com possibilidade de contrato corporativo.',
       88, 'Resposta até sexta · Brasil · Híbrido', 'IA • Operações • Automação • B2B',
       'https://empresa-alpha.com/poc', 'bg-[#d9f1e5] text-[#16734d]', 'PUBLICADA',
       (SELECT id FROM companies WHERE name = 'Empresa Alpha' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM opportunities WHERE code = 'poc');

INSERT INTO opportunities (code, type, title, short_description, description, match_score, meta_info, tags, external_url, color, status, company_id)
SELECT 'evento', 'EVENTO', 'Startup Connect Recife',
       'Networking com empresas e ecossistema local.',
       'Evento para conectar startups, corporações e investidores em uma agenda de networking, rodadas de negócios e apresentação de pitches.',
       85, '22/10/2026 · Recife · Presencial', 'Networking • Investimento • B2B • Ecossistema',
       'https://startupconnect.com.br', 'bg-[#dff1f5] text-[#1e7080]', 'PUBLICADA', NULL
WHERE NOT EXISTS (SELECT 1 FROM opportunities WHERE code = 'evento');

INSERT INTO opportunities (code, type, title, short_description, description, match_score, meta_info, tags, external_url, color, status, company_id)
SELECT 'compra', 'COMPRA', 'Compra — Dados & Analytics',
       'Demanda por plataforma de análise.',
       'Demanda empresarial por contratação direta de plataforma de análise de dados e indicadores para apoiar decisões operacionais e estratégicas.',
       86, 'Aberta · Brasil · Remoto', 'Dados • Analytics • SaaS • Gestão',
       'https://compras.startin.com.br', 'bg-[#ffe1c3] text-[#975524]', 'PUBLICADA', NULL
WHERE NOT EXISTS (SELECT 1 FROM opportunities WHERE code = 'compra');
