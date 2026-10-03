#!/usr/bin/env node

/**
 * migrate.js: Database Migration Runner for Fuel Station PostgreSQL
 * Usage:
 *   node database/scripts/migrate.js up
 *   node database/scripts/migrate.js down
 *   node database/scripts/migrate.js status
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const MIGRATIONS_DIR = path.join(__dirname, '..', 'migrations');

// Parse environment variables
require('dotenv').config({ path: path.join(__dirname, '..', '..', '.env') });

const DB_HOST = process.env.DB_HOST || 'localhost';
const DB_PORT = process.env.DB_PORT || '5432';
const DB_NAME = process.env.DB_NAME || 'fuel_station_db';
const DB_USER = process.env.DB_USER || 'fuel_station';
const DB_PASSWORD = process.env.DB_PASSWORD || 'fuel_station_secret_pass';

function getPsqlCmd(sqlFilePath) {
    return `PGPASSWORD="${DB_PASSWORD}" psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} -f "${sqlFilePath}"`;
}

function runSqlInline(sql) {
    const escapedSql = sql.replace(/"/g, '\\"');
    return `PGPASSWORD="${DB_PASSWORD}" psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} -c "${escapedSql}"`;
}

function getMigrationFiles() {
    const files = fs.readdirSync(MIGRATIONS_DIR);
    const upFiles = files.filter(f => f.endsWith('.up.sql')).sort();
    const downFiles = files.filter(f => f.endsWith('.down.sql')).sort();
    return { upFiles, downFiles };
}

async function migrateUp() {
    console.log(`[INFO] Running migrations UP on ${DB_NAME} (${DB_HOST}:${DB_PORT})...`);
    const { upFiles } = getMigrationFiles();

    for (const file of upFiles) {
        const filePath = path.join(MIGRATIONS_DIR, file);
        console.log(`[APPLYING] ${file}...`);
        try {
            execSync(getPsqlCmd(filePath), { stdio: 'inherit' });
            console.log(`[DONE] ${file} applied successfully.\n`);
        } catch (err) {
            console.error(`[ERROR] Failed to apply ${file}:`, err.message);
            process.exit(1);
        }
    }
    console.log('[SUCCESS] All migrations applied successfully.');
}

async function migrateDown() {
    console.log(`[INFO] Running migrations DOWN on ${DB_NAME} (${DB_HOST}:${DB_PORT})...`);
    const { downFiles } = getMigrationFiles();
    // Reverse order for rollback
    const reverseDown = [...downFiles].reverse();

    for (const file of reverseDown) {
        const filePath = path.join(MIGRATIONS_DIR, file);
        console.log(`[ROLLING BACK] ${file}...`);
        try {
            execSync(getPsqlCmd(filePath), { stdio: 'inherit' });
            console.log(`[DONE] ${file} rolled back successfully.\n`);
        } catch (err) {
            console.error(`[ERROR] Failed to rollback ${file}:`, err.message);
            process.exit(1);
        }
    }
    console.log('[SUCCESS] All migrations rolled back.');
}

function showStatus() {
    console.log(`[INFO] Database: ${DB_NAME} at ${DB_HOST}:${DB_PORT} (User: ${DB_USER})`);
    const { upFiles, downFiles } = getMigrationFiles();
    console.log(`Available UP migrations (${upFiles.length}):`);
    upFiles.forEach(f => console.log(`  - ${f}`));
    console.log(`Available DOWN migrations (${downFiles.length}):`);
    downFiles.forEach(f => console.log(`  - ${f}`));
}

const command = process.argv[2] || 'status';

switch (command) {
    case 'up':
        migrateUp();
        break;
    case 'down':
        migrateDown();
        break;
    case 'status':
        showStatus();
        break;
    default:
        console.log(`Unknown command: ${command}. Use 'up', 'down', or 'status'.`);
        process.exit(1);
}
