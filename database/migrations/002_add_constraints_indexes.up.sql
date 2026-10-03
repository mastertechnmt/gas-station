-- ==============================================================================
-- Migration: 002_add_constraints_indexes.up.sql
-- Description: Indexes, Exclusion Constraints, and Triggers
-- ==============================================================================

-- 1. Operational Days Constraints & Date Indexes
CREATE UNIQUE INDEX IF NOT EXISTS idx_uq_open_day_per_station
    ON operational_days (station_id)
    WHERE status = 'open';

CREATE INDEX IF NOT EXISTS idx_operational_days_station_id ON operational_days (station_id);
CREATE INDEX IF NOT EXISTS idx_operational_days_date ON operational_days (date);
CREATE INDEX IF NOT EXISTS idx_operational_days_status ON operational_days (status);
CREATE INDEX IF NOT EXISTS idx_operational_days_created_at ON operational_days (created_at);

-- 2. Fuel Types & Prices Indexes
CREATE INDEX IF NOT EXISTS idx_fuel_prices_fuel_type_id ON fuel_prices (fuel_type_id);
CREATE INDEX IF NOT EXISTS idx_fuel_prices_effective_dates ON fuel_prices (effective_from, effective_to);

-- 3. Pumps & Nozzles Indexes
CREATE INDEX IF NOT EXISTS idx_pumps_station_id ON pumps (station_id);
CREATE INDEX IF NOT EXISTS idx_pumps_status ON pumps (is_active);

CREATE INDEX IF NOT EXISTS idx_nozzles_pump_id ON nozzles (pump_id);
CREATE INDEX IF NOT EXISTS idx_nozzles_fuel_type_id ON nozzles (fuel_type_id);

-- 4. Tanks Indexes
CREATE INDEX IF NOT EXISTS idx_tanks_station_id ON tanks (station_id);
CREATE INDEX IF NOT EXISTS idx_tanks_fuel_type_id ON tanks (fuel_type_id);

-- 5. Readings Indexes
CREATE INDEX IF NOT EXISTS idx_readings_operational_day_id ON readings (operational_day_id);
CREATE INDEX IF NOT EXISTS idx_readings_nozzle_id ON readings (nozzle_id);
CREATE INDEX IF NOT EXISTS idx_readings_recorded_at ON readings (recorded_at);
CREATE INDEX IF NOT EXISTS idx_readings_created_at ON readings (created_at);

-- 6. Sales & Sale Items Indexes
CREATE INDEX IF NOT EXISTS idx_sales_operational_day_id ON sales (operational_day_id);
CREATE INDEX IF NOT EXISTS idx_sales_station_id ON sales (station_id);
CREATE INDEX IF NOT EXISTS idx_sales_customer_id ON sales (customer_id);
CREATE INDEX IF NOT EXISTS idx_sales_payment_method_id ON sales (payment_method_id);
CREATE INDEX IF NOT EXISTS idx_sales_sale_date ON sales (sale_date);
CREATE INDEX IF NOT EXISTS idx_sales_status ON sales (status);
CREATE INDEX IF NOT EXISTS idx_sales_created_at ON sales (created_at);

CREATE INDEX IF NOT EXISTS idx_sale_items_sale_id ON sale_items (sale_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_fuel_type_id ON sale_items (fuel_type_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_nozzle_id ON sale_items (nozzle_id);

-- 7. Purchases & Purchase Items Indexes
CREATE INDEX IF NOT EXISTS idx_purchases_supplier_id ON purchases (supplier_id);
CREATE INDEX IF NOT EXISTS idx_purchases_station_id ON purchases (station_id);
CREATE INDEX IF NOT EXISTS idx_purchases_operational_day_id ON purchases (operational_day_id);
CREATE INDEX IF NOT EXISTS idx_purchases_purchase_date ON purchases (purchase_date);
CREATE INDEX IF NOT EXISTS idx_purchases_status ON purchases (status);

CREATE INDEX IF NOT EXISTS idx_purchase_items_purchase_id ON purchase_items (purchase_id);
CREATE INDEX IF NOT EXISTS idx_purchase_items_tank_id ON purchase_items (tank_id);
CREATE INDEX IF NOT EXISTS idx_purchase_items_fuel_type_id ON purchase_items (fuel_type_id);

-- 8. Inventory Movements Indexes
CREATE INDEX IF NOT EXISTS idx_inv_movements_station_id ON inventory_movements (station_id);
CREATE INDEX IF NOT EXISTS idx_inv_movements_tank_id ON inventory_movements (tank_id);
CREATE INDEX IF NOT EXISTS idx_inv_movements_fuel_type_id ON inventory_movements (fuel_type_id);
CREATE INDEX IF NOT EXISTS idx_inv_movements_type ON inventory_movements (movement_type);
CREATE INDEX IF NOT EXISTS idx_inv_movements_ref ON inventory_movements (reference_type, reference_id);
CREATE INDEX IF NOT EXISTS idx_inv_movements_created_at ON inventory_movements (created_at);

-- 9. Cashboxes & Cash Transactions Indexes
CREATE INDEX IF NOT EXISTS idx_cashboxes_station_id ON cashboxes (station_id);

CREATE INDEX IF NOT EXISTS idx_cash_tx_cashbox_id ON cash_transactions (cashbox_id);
CREATE INDEX IF NOT EXISTS idx_cash_tx_operational_day_id ON cash_transactions (operational_day_id);
CREATE INDEX IF NOT EXISTS idx_cash_tx_type ON cash_transactions (transaction_type);
CREATE INDEX IF NOT EXISTS idx_cash_tx_ref ON cash_transactions (reference_type, reference_id);
CREATE INDEX IF NOT EXISTS idx_cash_tx_created_at ON cash_transactions (created_at);

-- 10. Financial Accounts & Financial Transactions Indexes
CREATE INDEX IF NOT EXISTS idx_fin_accounts_station_id ON financial_accounts (station_id);

CREATE INDEX IF NOT EXISTS idx_fin_tx_account_id ON financial_transactions (financial_account_id);
CREATE INDEX IF NOT EXISTS idx_fin_tx_operational_day_id ON financial_transactions (operational_day_id);
CREATE INDEX IF NOT EXISTS idx_fin_tx_type ON financial_transactions (transaction_type);
CREATE INDEX IF NOT EXISTS idx_fin_tx_ref ON financial_transactions (reference_type, reference_id);
CREATE INDEX IF NOT EXISTS idx_fin_tx_created_at ON financial_transactions (created_at);

-- 11. Customer & Customer Transactions Indexes
CREATE INDEX IF NOT EXISTS idx_customers_station_id ON customers (station_id);

CREATE INDEX IF NOT EXISTS idx_customer_tx_customer_id ON customer_transactions (customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_tx_operational_day_id ON customer_transactions (operational_day_id);
CREATE INDEX IF NOT EXISTS idx_customer_tx_type ON customer_transactions (transaction_type);
CREATE INDEX IF NOT EXISTS idx_customer_tx_ref ON customer_transactions (reference_type, reference_id);
CREATE INDEX IF NOT EXISTS idx_customer_tx_created_at ON customer_transactions (created_at);

-- 12. Supplier & Supplier Transactions Indexes
CREATE INDEX IF NOT EXISTS idx_suppliers_station_id ON suppliers (station_id);

CREATE INDEX IF NOT EXISTS idx_supplier_tx_supplier_id ON supplier_transactions (supplier_id);
CREATE INDEX IF NOT EXISTS idx_supplier_tx_operational_day_id ON supplier_transactions (operational_day_id);
CREATE INDEX IF NOT EXISTS idx_supplier_tx_type ON supplier_transactions (transaction_type);
CREATE INDEX IF NOT EXISTS idx_supplier_tx_ref ON supplier_transactions (reference_type, reference_id);
CREATE INDEX IF NOT EXISTS idx_supplier_tx_created_at ON supplier_transactions (created_at);

-- 13. Employees & Expenses Indexes
CREATE INDEX IF NOT EXISTS idx_employees_station_id ON employees (station_id);
CREATE INDEX IF NOT EXISTS idx_employees_status ON employees (status);

CREATE INDEX IF NOT EXISTS idx_expenses_station_id ON expenses (station_id);
CREATE INDEX IF NOT EXISTS idx_expenses_operational_day_id ON expenses (operational_day_id);
CREATE INDEX IF NOT EXISTS idx_expenses_category_id ON expenses (category_id);
CREATE INDEX IF NOT EXISTS idx_expenses_date ON expenses (expense_date);
CREATE INDEX IF NOT EXISTS idx_expenses_created_at ON expenses (created_at);

-- 14. Variances Indexes
CREATE INDEX IF NOT EXISTS idx_variances_station_id ON variances (station_id);
CREATE INDEX IF NOT EXISTS idx_variances_operational_day_id ON variances (operational_day_id);
CREATE INDEX IF NOT EXISTS idx_variances_type ON variances (variance_type);
CREATE INDEX IF NOT EXISTS idx_variances_status ON variances (status);
CREATE INDEX IF NOT EXISTS idx_variances_ref ON variances (reference_type, reference_id);
CREATE INDEX IF NOT EXISTS idx_variances_created_at ON variances (created_at);

-- 15. Attachments & Audit Logs Indexes
CREATE INDEX IF NOT EXISTS idx_attachments_station_id ON attachments (station_id);
CREATE INDEX IF NOT EXISTS idx_attachments_entity ON attachments (entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_station_id ON audit_logs (station_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs (action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at);

-- 16. Settings Indexes
CREATE INDEX IF NOT EXISTS idx_settings_station_id ON settings (station_id);
CREATE INDEX IF NOT EXISTS idx_settings_key ON settings (key);

-- 17. Automated Updated At Function & Triggers
CREATE OR REPLACE FUNCTION fn_update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    t text;
BEGIN
    FOR t IN
        SELECT table_name
        FROM information_schema.columns
        WHERE column_name = 'updated_at'
          AND table_schema = 'public'
    LOOP
        EXECUTE format('
            DROP TRIGGER IF EXISTS trg_update_timestamp_%I ON %I;
            CREATE TRIGGER trg_update_timestamp_%I
            BEFORE UPDATE ON %I
            FOR EACH ROW
            EXECUTE FUNCTION fn_update_timestamp();
        ', t, t, t, t);
    END LOOP;
END;
$$;
