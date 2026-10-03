-- ==============================================================================
-- Migration: 003_seed_master_data.up.sql
-- Description: Seed initial master entities for station, fuels, tanks, pumps, users
-- ==============================================================================

-- 1. Main Station
INSERT INTO stations (id, name, address, phone, logo, currency, timezone, is_active)
VALUES (
    'a0000000-0000-0000-0000-000000000001'::uuid,
    'محطة الوقود الرئيسية',
    'صنعاء - تقاطع شارع الستين مع شارع الزبيري',
    '+967 770 123 456',
    '/assets/station_logo.png',
    'YER',
    'Asia/Aden',
    true
) ON CONFLICT (id) DO NOTHING;

-- 2. Manager User (password: 'Admin@123456')
INSERT INTO users (id, username, email, password_hash, full_name, role, is_active)
VALUES (
    'b0000000-0000-0000-0000-000000000001'::uuid,
    'admin',
    'admin@fuelstation.local',
    crypt('Admin@123456', gen_salt('bf', 8)),
    'مدير المحطة العام',
    'manager',
    true
) ON CONFLICT (username) DO NOTHING;

-- 3. Fuel Types (بنزين 91, بنزين 95, ديزل)
INSERT INTO fuel_types (id, name, code, unit, is_active)
VALUES 
    ('c0000000-0000-0000-0000-000000000001'::uuid, 'بنزين 91', 'G91', 'liter', true),
    ('c0000000-0000-0000-0000-000000000002'::uuid, 'بنزين 95', 'G95', 'liter', true),
    ('c0000000-0000-0000-0000-000000000003'::uuid, 'ديزل', 'DSL', 'liter', true)
ON CONFLICT (code) DO NOTHING;

-- 4. Fuel Prices
INSERT INTO fuel_prices (id, fuel_type_id, price, effective_from, effective_to)
VALUES
    ('c1000000-0000-0000-0000-000000000001'::uuid, 'c0000000-0000-0000-0000-000000000001'::uuid, 400.00, CURRENT_TIMESTAMP - INTERVAL '30 days', NULL),
    ('c1000000-0000-0000-0000-000000000002'::uuid, 'c0000000-0000-0000-0000-000000000002'::uuid, 450.00, CURRENT_TIMESTAMP - INTERVAL '30 days', NULL),
    ('c1000000-0000-0000-0000-000000000003'::uuid, 'c0000000-0000-0000-0000-000000000003'::uuid, 380.00, CURRENT_TIMESTAMP - INTERVAL '30 days', NULL)
ON CONFLICT (id) DO NOTHING;

-- 5. Tanks
INSERT INTO tanks (id, station_id, name, number, fuel_type_id, capacity, current_quantity, minimum_quantity, critical_quantity, is_active)
VALUES
    ('d0000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'خزان بنزين 91 الرئيسي', 1, 'c0000000-0000-0000-0000-000000000001'::uuid, 50000.000, 25000.000, 5000.000, 2000.000, true),
    ('d0000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'خزان بنزين 95 ممتاز', 2, 'c0000000-0000-0000-0000-000000000002'::uuid, 35000.000, 18000.000, 4000.000, 1500.000, true),
    ('d0000000-0000-0000-0000-000000000003'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'خزان ديزل', 3, 'c0000000-0000-0000-0000-000000000003'::uuid, 60000.000, 32000.000, 6000.000, 2500.000, true)
ON CONFLICT (id) DO NOTHING;

-- 6. Pumps
INSERT INTO pumps (id, station_id, name, number, is_active)
VALUES
    ('e0000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'طرمبة 1', 1, true),
    ('e0000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'طرمبة 2', 2, true)
ON CONFLICT (id) DO NOTHING;

-- 7. Nozzles
INSERT INTO nozzles (id, pump_id, fuel_type_id, name, number, is_active)
VALUES
    ('f0000000-0000-0000-0000-000000000001'::uuid, 'e0000000-0000-0000-0000-000000000001'::uuid, 'c0000000-0000-0000-0000-000000000001'::uuid, 'مسدس 1 (بنزين 91)', 1, true),
    ('f0000000-0000-0000-0000-000000000002'::uuid, 'e0000000-0000-0000-0000-000000000001'::uuid, 'c0000000-0000-0000-0000-000000000002'::uuid, 'مسدس 2 (بنزين 95)', 2, true),
    ('f0000000-0000-0000-0000-000000000003'::uuid, 'e0000000-0000-0000-0000-000000000002'::uuid, 'c0000000-0000-0000-0000-000000000003'::uuid, 'مسدس 1 (ديزل)', 1, true),
    ('f0000000-0000-0000-0000-000000000004'::uuid, 'e0000000-0000-0000-0000-000000000002'::uuid, 'c0000000-0000-0000-0000-000000000001'::uuid, 'مسدس 2 (بنزين 91)', 2, true)
ON CONFLICT (id) DO NOTHING;

-- 8. Payment Methods
INSERT INTO payment_methods (id, name, code, is_active)
VALUES
    ('10000000-0000-0000-0000-000000000001'::uuid, 'نقد', 'cash', true),
    ('10000000-0000-0000-0000-000000000002'::uuid, 'حساب مالي / محفظة', 'financial_account', true),
    ('10000000-0000-0000-0000-000000000003'::uuid, 'آجل', 'credit', true)
ON CONFLICT (code) DO NOTHING;

-- 9. Expense Categories
INSERT INTO expense_categories (id, name, is_active)
VALUES
    ('20000000-0000-0000-0000-000000000001'::uuid, 'صيانة', true),
    ('20000000-0000-0000-0000-000000000002'::uuid, 'رواتب', true),
    ('20000000-0000-0000-0000-000000000003'::uuid, 'كهرباء', true),
    ('20000000-0000-0000-0000-000000000004'::uuid, 'ماء', true),
    ('20000000-0000-0000-0000-000000000005'::uuid, 'نقل', true),
    ('20000000-0000-0000-0000-000000000006'::uuid, 'مشتريات', true),
    ('20000000-0000-0000-0000-000000000007'::uuid, 'أخرى', true)
ON CONFLICT (name) DO NOTHING;

-- 10. Cashbox
INSERT INTO cashboxes (id, station_id, name, opening_balance, current_balance, is_active)
VALUES (
    '30000000-0000-0000-0000-000000000001'::uuid,
    'a0000000-0000-0000-0000-000000000001'::uuid,
    'الخزينة النقدية الرئيسية',
    500000.00,
    500000.00,
    true
) ON CONFLICT (id) DO NOTHING;

-- 11. Financial Accounts
INSERT INTO financial_accounts (id, station_id, name, account_type, account_number, current_balance, is_active)
VALUES
    ('40000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'بنك التضامن الإسلامي', 'bank', 'YE-001-229988', 1200000.00, true),
    ('40000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'محفظة جوالي / الكريمي', 'wallet', '770112233', 350000.00, true)
ON CONFLICT (id) DO NOTHING;
