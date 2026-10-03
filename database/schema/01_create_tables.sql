-- ==============================================================================
-- 01_create_tables.sql: Full Normalized Relational Schema for Fuel Station System
-- Database: fuel_station_db
-- ==============================================================================

-- 1. USERS
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'manager',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_role CHECK (role IN ('manager', 'supervisor', 'accountant', 'admin'))
);

-- 2. STATIONS
CREATE TABLE IF NOT EXISTS stations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    address TEXT,
    phone VARCHAR(50),
    logo VARCHAR(500),
    currency VARCHAR(10) NOT NULL DEFAULT 'YER',
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Aden',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. OPERATIONAL DAYS
CREATE TABLE IF NOT EXISTS operational_days (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    station_id UUID NOT NULL REFERENCES stations(id) ON DELETE RESTRICT,
    date DATE NOT NULL,
    opening_balance DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'open',
    opened_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_day_status CHECK (status IN ('open', 'closed')),
    CONSTRAINT chk_day_opening_balance CHECK (opening_balance >= 0)
);

-- 4. FUEL TYPES
CREATE TABLE IF NOT EXISTS fuel_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(30) NOT NULL UNIQUE,
    unit VARCHAR(30) NOT NULL DEFAULT 'liter',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. FUEL PRICES
CREATE TABLE IF NOT EXISTS fuel_prices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    fuel_type_id UUID NOT NULL REFERENCES fuel_types(id) ON DELETE RESTRICT,
    price DECIMAL(12, 2) NOT NULL,
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_fuel_price_positive CHECK (price > 0),
    CONSTRAINT chk_price_effective_range CHECK (effective_to IS NULL OR effective_to > effective_from)
);

-- 6. PUMPS
CREATE TABLE IF NOT EXISTS pumps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    station_id UUID NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    number INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_station_pump_number UNIQUE (station_id, number)
);

-- 7. NOZZLES
CREATE TABLE IF NOT EXISTS nozzles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    pump_id UUID NOT NULL REFERENCES pumps(id) ON DELETE CASCADE,
    fuel_type_id UUID NOT NULL REFERENCES fuel_types(id) ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL,
    number INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pump_nozzle_number UNIQUE (pump_id, number)
);

-- 8. TANKS
CREATE TABLE IF NOT EXISTS tanks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    station_id UUID NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    number INT NOT NULL,
    fuel_type_id UUID NOT NULL REFERENCES fuel_types(id) ON DELETE RESTRICT,
    capacity DECIMAL(14, 3) NOT NULL,
    current_quantity DECIMAL(14, 3) NOT NULL DEFAULT 0.000,
    minimum_quantity DECIMAL(14, 3) NOT NULL DEFAULT 0.000,
    critical_quantity DECIMAL(14, 3) NOT NULL DEFAULT 0.000,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_station_tank_number UNIQUE (station_id, number),
    CONSTRAINT chk_tank_capacity_positive CHECK (capacity > 0),
    CONSTRAINT chk_tank_current_quantity CHECK (current_quantity >= 0 AND current_quantity <= capacity),
    CONSTRAINT chk_tank_alert_levels CHECK (critical_quantity <= minimum_quantity AND minimum_quantity <= capacity)
);

-- 9. READINGS
CREATE TABLE IF NOT EXISTS readings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    operational_day_id UUID NOT NULL REFERENCES operational_days(id) ON DELETE RESTRICT,
    nozzle_id UUID NOT NULL REFERENCES nozzles(id) ON DELETE RESTRICT,
    previous_reading DECIMAL(14, 3) NOT NULL,
    current_reading DECIMAL(14, 3) NOT NULL,
    sold_quantity DECIMAL(14, 3) NOT NULL,
    fuel_price DECIMAL(12, 2) NOT NULL,
    sales_amount DECIMAL(14, 2) NOT NULL,
    recorded_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reading_sequence CHECK (current_reading >= previous_reading),
    CONSTRAINT chk_sold_quantity_calc CHECK (sold_quantity = (current_reading - previous_reading)),
    CONSTRAINT chk_sold_quantity_non_negative CHECK (sold_quantity >= 0),
    CONSTRAINT chk_sales_amount_positive CHECK (sales_amount >= 0)
);

-- 10. PAYMENT METHODS
CREATE TABLE IF NOT EXISTS payment_methods (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT true
);

-- 11. CUSTOMERS
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    station_id UUID NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(50),
    address TEXT,
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 12. SALES
CREATE TABLE IF NOT EXISTS sales (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    operational_day_id UUID NOT NULL REFERENCES operational_days(id) ON DELETE RESTRICT,
    station_id UUID NOT NULL REFERENCES stations(id) ON DELETE RESTRICT,
    sale_number VARCHAR(100) NOT NULL UNIQUE,
    sale_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(14, 2) NOT NULL,
    total_quantity DECIMAL(14, 3) NOT NULL,
    payment_method_id UUID NOT NULL REFERENCES payment_methods(id) ON DELETE RESTRICT,
    customer_id UUID REFERENCES customers(id) ON DELETE SET NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'completed',
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sale_status CHECK (status IN ('completed', 'cancelled', 'refunded')),
    CONSTRAINT chk_sale_amounts CHECK (total_amount >= 0 AND total_quantity >= 0)
);

-- 13. SALE ITEMS
CREATE TABLE IF NOT EXISTS sale_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sale_id UUID NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    fuel_type_id UUID NOT NULL REFERENCES fuel_types(id) ON DELETE RESTRICT,
    nozzle_id UUID NOT NULL REFERENCES nozzles(id) ON DELETE RESTRICT,
    quantity DECIMAL(14, 3) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    total_amount DECIMAL(14, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sale_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_sale_item_price CHECK (unit_price >= 0),
    CONSTRAINT chk_sale_item_total CHECK (total_amount >= 0)
);

-- 14. SUPPLIERS
CREATE TABLE IF NOT EXISTS suppliers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    station_id UUID NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(50),
    address TEXT,
    notes TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 15. PURCHASES
CREATE TABLE IF NOT EXISTS purchases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT,
    station_id UUID NOT NULL REFERENCES stations(id) ON DELETE RESTRICT,
    operational_day_id UUID NOT NULL REFERENCES operational_days(id) ON DELETE RESTRICT,
    invoice_number VARCHAR(100) NOT NULL,
    purchase_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(14, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'received',
    notes TEXT,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_purchase_status CHECK (status IN ('received', 'pending', 'cancelled')),
    CONSTRAINT chk_purchase_total CHECK (total_amount >= 0)
);

-- 16. PURCHASE ITEMS
CREATE TABLE IF NOT EXISTS purchase_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    purchase_id UUID NOT NULL REFERENCES purchases(id) ON DELETE CASCADE,
    fuel_type_id UUID NOT NULL REFERENCES fuel_types(id) ON DELETE RESTRICT,
    tank_id UUID NOT NULL REFERENCES tanks(id) ON DELETE RESTRICT,
    quantity DECIMAL(14, 3) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    total_amount DECIMAL(14, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_purchase_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_purchase_item_price CHECK (unit_price >= 0),
    CONSTRAINT chk_purchase_item_total CHECK (total_amount >= 0)
);

-- 17. INVENTORY MOVEMENTS (Source of Truth for Fuel Inventory)
CREATE TABLE IF NOT EXISTS inventory_movements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    station_id UUID NOT NULL REFERENCES stations(id) ON DELETE RESTRICT,
    tank_id UUID NOT NULL REFERENCES tanks(id) ON DELETE RESTRICT,
    fuel_type_id UUID NOT NULL REFERENCES fuel_types(id) ON DELETE RESTRICT,
    movement_type VARCHAR(30) NOT NULL,
    quantity DECIMAL(14, 3) NOT NULL,
    reference_type VARCHAR(50) NOT NULL,
    reference_id UUID NOT NULL,
    quantity_before DECIMAL(14, 3) NOT NULL,
    quantity_after DECIMAL(14, 3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    notes TEXT,
    CONSTRAINT chk_inventory_movement_type CHECK (movement_type IN ('opening', 'purchase', 'sale', 'adjustment')),
    CONSTRAINT chk_inventory_qty_positive CHECK (quantity >= 0),
    CONSTRAINT chk_inventory_balances_positive CHECK (quantity_before >= 0 AND quantity_after >= 0)
);

-- 18. CASHBOXES
CREATE TABLE IF NOT EXISTS cashboxes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    station_id UUID NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    opening_balance DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    current_balance DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 19. CASH TRANSACTIONS
CREATE TABLE IF NOT EXISTS cash_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cashbox_id UUID NOT NULL REFERENCES cashboxes(id) ON DELETE RESTRICT,
    operational_day_id UUID NOT NULL REFERENCES operational_days(id) ON DELETE RESTRICT,
    transaction_type VARCHAR(30) NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    balance_before DECIMAL(14, 2) NOT NULL,
    balance_after DECIMAL(14, 2) NOT NULL,
    reference_type VARCHAR(50),
    reference_id UUID,
    description TEXT,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_cash_tx_type CHECK (transaction_type IN ('opening', 'sale', 'expense', 'deposit', 'withdrawal', 'adjustment')),
    CONSTRAINT chk_cash_tx_amount CHECK (amount >= 0)
);

-- 20. FINANCIAL ACCOUNTS (Banks & Wallets)
CREATE TABLE IF NOT EXISTS financial_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    station_id UUID NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    account_number VARCHAR(100),
    current_balance DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_financial_account_type CHECK (account_type IN ('bank', 'wallet', 'other'))
);

-- 21. FINANCIAL TRANSACTIONS
CREATE TABLE IF NOT EXISTS financial_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    financial_account_id UUID NOT NULL REFERENCES financial_accounts(id) ON DELETE RESTRICT,
    operational_day_id UUID NOT NULL REFERENCES operational_days(id) ON DELETE RESTRICT,
    transaction_type VARCHAR(40) NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    balance_before DECIMAL(14, 2) NOT NULL,
    balance_after DECIMAL(14, 2) NOT NULL,
    reference_type VARCHAR(50),
    reference_id UUID,
    description TEXT,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_fin_tx_type CHECK (transaction_type IN ('deposit', 'withdrawal', 'transfer_in', 'transfer_out', 'sale', 'expense', 'supplier_payment', 'customer_payment', 'adjustment')),
    CONSTRAINT chk_fin_tx_amount CHECK (amount >= 0)
);

-- 22. CUSTOMER TRANSACTIONS
CREATE TABLE IF NOT EXISTS customer_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    operational_day_id UUID NOT NULL REFERENCES operational_days(id) ON DELETE RESTRICT,
    transaction_type VARCHAR(30) NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    balance_before DECIMAL(14, 2) NOT NULL,
    balance_after DECIMAL(14, 2) NOT NULL,
    reference_type VARCHAR(50),
    reference_id UUID,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_customer_tx_type CHECK (transaction_type IN ('credit_sale', 'payment', 'adjustment')),
    CONSTRAINT chk_customer_tx_amount CHECK (amount >= 0)
);

-- 23. SUPPLIER TRANSACTIONS
CREATE TABLE IF NOT EXISTS supplier_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    supplier_id UUID NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT,
    operational_day_id UUID NOT NULL REFERENCES operational_days(id) ON DELETE RESTRICT,
    transaction_type VARCHAR(30) NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    balance_before DECIMAL(14, 2) NOT NULL,
    balance_after DECIMAL(14, 2) NOT NULL,
    reference_type VARCHAR(50),
    reference_id UUID,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_supplier_tx_type CHECK (transaction_type IN ('purchase', 'payment', 'adjustment')),
    CONSTRAINT chk_supplier_tx_amount CHECK (amount >= 0)
);

-- 24. EMPLOYEES
CREATE TABLE IF NOT EXISTS employees (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    station_id UUID NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    job_title VARCHAR(100) NOT NULL,
    phone VARCHAR(50),
    status VARCHAR(30) NOT NULL DEFAULT 'active',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_employee_status CHECK (status IN ('active', 'inactive', 'on_leave', 'terminated'))
);

-- 25. EXPENSE CATEGORIES
CREATE TABLE IF NOT EXISTS expense_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 26. EXPENSES
CREATE TABLE IF NOT EXISTS expenses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    station_id UUID NOT NULL REFERENCES stations(id) ON DELETE RESTRICT,
    operational_day_id UUID NOT NULL REFERENCES operational_days(id) ON DELETE RESTRICT,
    category_id UUID NOT NULL REFERENCES expense_categories(id) ON DELETE RESTRICT,
    amount DECIMAL(14, 2) NOT NULL,
    payment_source_type VARCHAR(30) NOT NULL,
    cashbox_id UUID REFERENCES cashboxes(id) ON DELETE RESTRICT,
    financial_account_id UUID REFERENCES financial_accounts(id) ON DELETE RESTRICT,
    beneficiary VARCHAR(150),
    expense_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    voucher_number VARCHAR(100),
    description TEXT,
    notes TEXT,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_expense_payment_source CHECK (payment_source_type IN ('cash', 'financial_account')),
    CONSTRAINT chk_expense_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_expense_source_consistency CHECK (
        (payment_source_type = 'cash' AND cashbox_id IS NOT NULL) OR
        (payment_source_type = 'financial_account' AND financial_account_id IS NOT NULL)
    )
);

-- 27. VARIANCES (Discrepancy Ledger for Audit & Reconciliation)
CREATE TABLE IF NOT EXISTS variances (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    station_id UUID NOT NULL REFERENCES stations(id) ON DELETE RESTRICT,
    operational_day_id UUID NOT NULL REFERENCES operational_days(id) ON DELETE RESTRICT,
    variance_type VARCHAR(30) NOT NULL,
    expected_amount DECIMAL(14, 2) NOT NULL,
    actual_amount DECIMAL(14, 2) NOT NULL,
    difference DECIMAL(14, 2) NOT NULL,
    reference_type VARCHAR(50),
    reference_id UUID,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'pending',
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_variance_type CHECK (variance_type IN ('reading', 'inventory', 'cash', 'sales')),
    CONSTRAINT chk_variance_status CHECK (status IN ('pending', 'resolved', 'written_off'))
);

-- 28. ATTACHMENTS (File Metadata & File Paths Only)
CREATE TABLE IF NOT EXISTS attachments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    station_id UUID NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    mime_type VARCHAR(100),
    file_size BIGINT,
    uploaded_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_attachment_file_size CHECK (file_size >= 0)
);

-- 29. AUDIT LOGS (Security & Audit Trail with JSONB)
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    station_id UUID REFERENCES stations(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 30. SETTINGS (Key-Value Dynamic Configuration per Station)
CREATE TABLE IF NOT EXISTS settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    station_id UUID NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    key VARCHAR(100) NOT NULL,
    value TEXT NOT NULL,
    value_type VARCHAR(30) NOT NULL DEFAULT 'string',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_station_setting_key UNIQUE (station_id, key),
    CONSTRAINT chk_setting_value_type CHECK (value_type IN ('string', 'number', 'boolean', 'json'))
);
