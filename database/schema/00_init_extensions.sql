-- ==============================================================================
-- 00_init_extensions.sql: PostgreSQL Extensions Initialization
-- ==============================================================================

-- Enable UUID extension for robust distributed unique identifiers
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enable pgcrypto for secure cryptographic hashing (e.g. password_hash)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Enable btree_gist for exclusion constraints (e.g. non-overlapping date ranges)
CREATE EXTENSION IF NOT EXISTS "btree_gist";
