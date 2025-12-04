-- ===================================
-- SENTINEL PROJECT-SERVICE DATABASE
-- ===================================

-- Drop tables if exist (for clean setup)
DROP TABLE IF EXISTS repositories CASCADE;
DROP TABLE IF EXISTS domains CASCADE;
DROP TABLE IF EXISTS projects CASCADE;
DROP TABLE IF EXISTS tenant_limits_cache CASCADE;

-- ===================================
-- PROJECTS TABLE
-- ===================================
CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    owner_id UUID NOT NULL,
    domain_count INT NOT NULL DEFAULT 0,
    repo_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_project_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'))
);

-- ===================================
-- DOMAINS TABLE
-- ===================================
CREATE TABLE IF NOT EXISTS domains (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    domain_url VARCHAR(255) NOT NULL UNIQUE,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    verification_method VARCHAR(20),
    verification_token VARCHAR(64),
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_domains_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT chk_verification_status CHECK (verification_status IN ('PENDING', 'VERIFIED', 'FAILED')),
    CONSTRAINT chk_verification_method CHECK (verification_method IN ('DNS_TXT', 'META_TAG', 'FILE_UPLOAD'))
);

-- ===================================
-- REPOSITORIES TABLE
-- ===================================
CREATE TABLE IF NOT EXISTS repositories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    repo_url VARCHAR(500) NOT NULL UNIQUE,
    repo_type VARCHAR(20) NOT NULL,
    access_token_encrypted TEXT,
    branch VARCHAR(100) NOT NULL DEFAULT 'main',
    last_scan_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_repositories_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT chk_repo_type CHECK (repo_type IN ('GITHUB', 'GITLAB', 'BITBUCKET'))
);

-- ===================================
-- TENANT LIMITS CACHE TABLE
-- ===================================
CREATE TABLE IF NOT EXISTS tenant_limits_cache (
    tenant_id UUID PRIMARY KEY,
    max_projects INT NOT NULL,
    max_domains INT NOT NULL,
    max_repos INT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===================================
-- INDEXES
-- ===================================

-- Projects indexes
CREATE INDEX IF NOT EXISTS idx_projects_tenant_id ON projects(tenant_id);
CREATE INDEX IF NOT EXISTS idx_projects_owner_id ON projects(owner_id);
CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status);

-- Domains indexes
CREATE INDEX IF NOT EXISTS idx_domains_project_id ON domains(project_id);
CREATE INDEX IF NOT EXISTS idx_domains_status ON domains(verification_status);
CREATE INDEX IF NOT EXISTS idx_domains_url ON domains(domain_url);

-- Repositories indexes
CREATE INDEX IF NOT EXISTS idx_repositories_project_id ON repositories(project_id);
CREATE INDEX IF NOT EXISTS idx_repositories_type ON repositories(repo_type);

-- ===================================
-- FUNCTIONS & TRIGGERS
-- ===================================

-- Drop existing function and trigger if they exist
DROP TRIGGER IF EXISTS update_projects_updated_at ON projects;
DROP TRIGGER IF EXISTS update_tenant_limits_cache_updated_at ON tenant_limits_cache;
DROP FUNCTION IF EXISTS update_updated_at_column();

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for projects table
CREATE TRIGGER update_projects_updated_at 
BEFORE UPDATE ON projects
FOR EACH ROW 
EXECUTE FUNCTION update_updated_at_column();

-- Trigger for tenant_limits_cache table
CREATE TRIGGER update_tenant_limits_cache_updated_at 
BEFORE UPDATE ON tenant_limits_cache
FOR EACH ROW 
EXECUTE FUNCTION update_updated_at_column();

-- ===================================
-- SAMPLE DATA (opcional para testing)
-- ===================================

-- Uncomment para insertar datos de prueba
-- INSERT INTO tenant_limits_cache (tenant_id, max_projects, max_domains, max_repos) 
-- VALUES ('00000000-0000-0000-0000-000000000001', 5, 10, 5)
-- ON CONFLICT (tenant_id) DO NOTHING;