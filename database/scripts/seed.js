#!/usr/bin/env node

/**
 * seed.js: Seed Runner for Fuel Station PostgreSQL
 * Usage:
 *   node database/scripts/seed.js
 */

const path = require('path');
const { execSync } = require('child_process');

require('dotenv').config({ path: path.join(__dirname, '..', '..', '.env') });

const DB_HOST = process.env.DB_HOST || 'localhost';
const DB_PORT = process.env.DB_PORT || '5432';
const DB_NAME = process.env.DB_NAME || 'fuel_station_db';
const DB_USER = process.env.DB_USER || 'fuel_station';
const DB_PASSWORD = process.env.DB_PASSWORD || 'fuel_station_secret_pass';

const SEED_FILE = path.join(__dirname, '..', 'seed', 'seed_demo_data.sql');

console.log(`[INFO] Seeding demo data to ${DB_NAME} (${DB_HOST}:${DB_PORT})...`);

try {
    const cmd = `PGPASSWORD="${DB_PASSWORD}" psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} -f "${SEED_FILE}"`;
    execSync(cmd, { stdio: 'inherit' });
    console.log('\n[SUCCESS] Seed demo data inserted successfully.');
} catch (err) {
    console.error('[ERROR] Failed to seed demo data:', err.message);
    process.exit(1);
}
