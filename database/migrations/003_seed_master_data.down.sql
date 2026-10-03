-- ==============================================================================
-- Migration: 003_seed_master_data.down.sql
-- Description: Revert seeded master data
-- ==============================================================================

DELETE FROM financial_accounts WHERE station_id = 'a0000000-0000-0000-0000-000000000001'::uuid;
DELETE FROM cashboxes WHERE station_id = 'a0000000-0000-0000-0000-000000000001'::uuid;
DELETE FROM expense_categories WHERE name IN ('صيانة', 'رواتب', 'كهرباء', 'ماء', 'نقل', 'مشتريات', 'أخرى');
DELETE FROM payment_methods WHERE code IN ('cash', 'financial_account', 'credit');
DELETE FROM nozzles WHERE pump_id IN ('e0000000-0000-0000-0000-000000000001'::uuid, 'e0000000-0000-0000-0000-000000000002'::uuid);
DELETE FROM pumps WHERE station_id = 'a0000000-0000-0000-0000-000000000001'::uuid;
DELETE FROM tanks WHERE station_id = 'a0000000-0000-0000-0000-000000000001'::uuid;
DELETE FROM fuel_prices WHERE fuel_type_id IN ('c0000000-0000-0000-0000-000000000001'::uuid, 'c0000000-0000-0000-0000-000000000002'::uuid, 'c0000000-0000-0000-0000-000000000003'::uuid);
DELETE FROM fuel_types WHERE code IN ('G91', 'G95', 'DSL');
DELETE FROM users WHERE username = 'admin';
DELETE FROM stations WHERE id = 'a0000000-0000-0000-0000-000000000001'::uuid;
