-- ==============================================================================
-- seed_demo_data.sql: Complete Seed Data for Fuel Station Management System
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

-- 2. Manager User (password: 'Admin@123456' hashed via crypt with salt)
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

-- 5. Tanks (خزان بنزين 91, خزان بنزين 95, خزان ديزل)
INSERT INTO tanks (id, station_id, name, number, fuel_type_id, capacity, current_quantity, minimum_quantity, critical_quantity, is_active)
VALUES
    ('d0000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'خزان بنزين 91 الرئيسي', 1, 'c0000000-0000-0000-0000-000000000001'::uuid, 50000.000, 25000.000, 5000.000, 2000.000, true),
    ('d0000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'خزان بنزين 95 ممتاز', 2, 'c0000000-0000-0000-0000-000000000002'::uuid, 35000.000, 18000.000, 4000.000, 1500.000, true),
    ('d0000000-0000-0000-0000-000000000003'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'خزان ديزل', 3, 'c0000000-0000-0000-0000-000000000003'::uuid, 60000.000, 32000.000, 6000.000, 2500.000, true)
ON CONFLICT (id) DO NOTHING;

-- 6. Pumps (طرمبة 1, طرمبة 2)
INSERT INTO pumps (id, station_id, name, number, is_active)
VALUES
    ('e0000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'طرمبة 1', 1, true),
    ('e0000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'طرمبة 2', 2, true)
ON CONFLICT (id) DO NOTHING;

-- 7. Nozzles (مسدسات مرتبطة بأنواع الوقود)
INSERT INTO nozzles (id, pump_id, fuel_type_id, name, number, is_active)
VALUES
    -- Pump 1 Nozzles
    ('f0000000-0000-0000-0000-000000000001'::uuid, 'e0000000-0000-0000-0000-000000000001'::uuid, 'c0000000-0000-0000-0000-000000000001'::uuid, 'مسدس 1 (بنزين 91)', 1, true),
    ('f0000000-0000-0000-0000-000000000002'::uuid, 'e0000000-0000-0000-0000-000000000002'::uuid, 'c0000000-0000-0000-0000-000000000002'::uuid, 'مسدس 2 (بنزين 95)', 2, true),
    -- Pump 2 Nozzles
    ('f0000000-0000-0000-0000-000000000003'::uuid, 'e0000000-0000-0000-0000-000000000002'::uuid, 'c0000000-0000-0000-0000-000000000003'::uuid, 'مسدس 1 (ديزل)', 1, true),
    ('f0000000-0000-0000-0000-000000000004'::uuid, 'e0000000-0000-0000-0000-000000000002'::uuid, 'c0000000-0000-0000-0000-000000000001'::uuid, 'مسدس 2 (بنزين 91)', 2, true)
ON CONFLICT (id) DO NOTHING;

-- 8. Payment Methods (نقد, حساب مالي / محفظة, آجل)
INSERT INTO payment_methods (id, name, code, is_active)
VALUES
    ('10000000-0000-0000-0000-000000000001'::uuid, 'نقد', 'cash', true),
    ('10000000-0000-0000-0000-000000000002'::uuid, 'حساب مالي / محفظة', 'financial_account', true),
    ('10000000-0000-0000-0000-000000000003'::uuid, 'آجل', 'credit', true)
ON CONFLICT (code) DO NOTHING;

-- 9. Expense Categories (صيانة, رواتب, كهرباء, ماء, نقل, مشتريات, أخرى)
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

-- 11. Financial Accounts (Bank & Mobile Wallet)
INSERT INTO financial_accounts (id, station_id, name, account_type, account_number, current_balance, is_active)
VALUES
    ('40000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'بنك التضامن الإسلامي', 'bank', 'YE-001-229988', 1200000.00, true),
    ('40000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'محفظة جوالي / الكريمي', 'wallet', '770112233', 350000.00, true)
ON CONFLICT (id) DO NOTHING;

-- 12. Suppliers
INSERT INTO suppliers (id, station_id, name, phone, address, notes, is_active)
VALUES
    ('50000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'شركة النفط الوطنية', '770987654', 'الإدارة العامة - صنعاء', 'المورد الاستراتيجي الرئيسي للمشتقات النفطية', true),
    ('50000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'شركة بترول اليمن', '771234888', 'شارع الستين - صنعاء', 'مورد شحنات إضافية', true)
ON CONFLICT (id) DO NOTHING;

-- 13. Customers
INSERT INTO customers (id, station_id, name, phone, address, notes, is_active)
VALUES
    ('60000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'شركة النقل السريع', '773445566', 'صنعاء', 'حساب آجل معتمد (سقف ائتماني 2,000,000 ر.ي)', true),
    ('60000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'مؤسسة الأفق للمقاولات', '772113344', 'عمران', 'عقد تزويد أسطول شاحنات', true)
ON CONFLICT (id) DO NOTHING;

-- 14. Employees (عامل طرمبة, فني صيانة)
INSERT INTO employees (id, station_id, name, job_title, phone, status, notes)
VALUES
    ('70000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'أحمد ناصر القدسي', 'عامل طرمبة (نوبة صباحية)', '773112244', 'active', 'المسؤول عن طرمبة 1'),
    ('70000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'محمد علي الحيمي', 'عامل طرمبة (نوبة مسائية)', '775223355', 'active', 'المسؤول عن طرمبة 2'),
    ('70000000-0000-0000-0000-000000000003'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'صالح العمري', 'فني صيانة مضخات', '771334466', 'active', 'فحص وصيانة دورية')
ON CONFLICT (id) DO NOTHING;

-- 15. Operational Day (Today's Open Day)
INSERT INTO operational_days (id, station_id, date, opening_balance, status, opened_at)
VALUES (
    '80000000-0000-0000-0000-000000000001'::uuid,
    'a0000000-0000-0000-0000-000000000001'::uuid,
    CURRENT_DATE,
    500000.00,
    'open',
    CURRENT_TIMESTAMP - INTERVAL '4 hours'
) ON CONFLICT (id) DO NOTHING;

-- 16. Initial Inventory Movements for Tank Opening Balances
INSERT INTO inventory_movements (
    id, station_id, tank_id, fuel_type_id, movement_type, quantity,
    reference_type, reference_id, quantity_before, quantity_after,
    created_by, notes
) VALUES
    ('90000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'd0000000-0000-0000-0000-000000000001'::uuid, 'c0000000-0000-0000-0000-000000000001'::uuid, 'opening', 25000.000, 'operational_days', '80000000-0000-0000-0000-000000000001'::uuid, 0.000, 25000.000, 'b0000000-0000-0000-0000-000000000001'::uuid, 'الرصيد الافتتاحي لخزان بنزين 91'),
    ('90000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'd0000000-0000-0000-0000-000000000002'::uuid, 'c0000000-0000-0000-0000-000000000002'::uuid, 'opening', 18000.000, 'operational_days', '80000000-0000-0000-0000-000000000001'::uuid, 0.000, 18000.000, 'b0000000-0000-0000-0000-000000000001'::uuid, 'الرصيد الافتتاحي لخزان بنزين 95'),
    ('90000000-0000-0000-0000-000000000003'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'd0000000-0000-0000-0000-000000000003'::uuid, 'c0000000-0000-0000-0000-000000000003'::uuid, 'opening', 32000.000, 'operational_days', '80000000-0000-0000-0000-000000000001'::uuid, 0.000, 32000.000, 'b0000000-0000-0000-0000-000000000001'::uuid, 'الرصيد الافتتاحي لخزان الديزل')
ON CONFLICT (id) DO NOTHING;

-- 17. Initial Nozzle Readings Baseline
INSERT INTO readings (
    id, operational_day_id, nozzle_id, previous_reading, current_reading,
    sold_quantity, fuel_price, sales_amount, recorded_by, notes
) VALUES
    ('91000000-0000-0000-0000-000000000001'::uuid, '80000000-0000-0000-0000-000000000001'::uuid, 'f0000000-0000-0000-0000-000000000001'::uuid, 125000.000, 125000.000, 0.000, 400.00, 0.00, 'b0000000-0000-0000-0000-000000000001'::uuid, 'القراءة الافتتاحية للمسدس'),
    ('91000000-0000-0000-0000-000000000002'::uuid, '80000000-0000-0000-0000-000000000001'::uuid, 'f0000000-0000-0000-0000-000000000002'::uuid, 82400.000, 82400.000, 0.000, 450.00, 0.00, 'b0000000-0000-0000-0000-000000000001'::uuid, 'القراءة الافتتاحية للمسدس'),
    ('91000000-0000-0000-0000-000000000003'::uuid, '80000000-0000-0000-0000-000000000001'::uuid, 'f0000000-0000-0000-0000-000000000003'::uuid, 210000.000, 210000.000, 0.000, 380.00, 0.00, 'b0000000-0000-0000-0000-000000000001'::uuid, 'القراءة الافتتاحية للمسدس'),
    ('91000000-0000-0000-0000-000000000004'::uuid, '80000000-0000-0000-0000-000000000001'::uuid, 'f0000000-0000-0000-0000-000000000004'::uuid, 64200.000, 64200.000, 0.000, 400.00, 0.00, 'b0000000-0000-0000-0000-000000000001'::uuid, 'القراءة الافتتاحية للمسدس')
ON CONFLICT (id) DO NOTHING;

-- 18. Station Settings
INSERT INTO settings (id, station_id, key, value, value_type)
VALUES
    ('92000000-0000-0000-0000-000000000001'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'station_code', 'FS-YE-001', 'string'),
    ('92000000-0000-0000-0000-000000000002'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'allow_overfill', 'false', 'boolean'),
    ('92000000-0000-0000-0000-000000000003'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'max_credit_days', '30', 'number'),
    ('92000000-0000-0000-0000-000000000004'::uuid, 'a0000000-0000-0000-0000-000000000001'::uuid, 'tax_rate_percent', '0.00', 'number')
ON CONFLICT (station_id, key) DO NOTHING;
