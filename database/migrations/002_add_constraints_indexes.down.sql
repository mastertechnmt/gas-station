-- ==============================================================================
-- Migration: 002_add_constraints_indexes.down.sql
-- Description: Drop indexes and triggers
-- ==============================================================================

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
        EXECUTE format('DROP TRIGGER IF EXISTS trg_update_timestamp_%I ON %I;', t, t);
    END LOOP;
END;
$$;

DROP FUNCTION IF EXISTS fn_update_timestamp();

DROP INDEX IF EXISTS idx_uq_open_day_per_station;
DROP INDEX IF EXISTS idx_operational_days_station_id;
DROP INDEX IF EXISTS idx_operational_days_date;
DROP INDEX IF EXISTS idx_operational_days_status;
DROP INDEX IF EXISTS idx_operational_days_created_at;
DROP INDEX IF EXISTS idx_fuel_prices_fuel_type_id;
DROP INDEX IF EXISTS idx_fuel_prices_effective_dates;
DROP INDEX IF EXISTS idx_pumps_station_id;
DROP INDEX IF EXISTS idx_pumps_status;
DROP INDEX IF EXISTS idx_nozzles_pump_id;
DROP INDEX IF EXISTS idx_nozzles_fuel_type_id;
DROP INDEX IF EXISTS idx_tanks_station_id;
DROP INDEX IF EXISTS idx_tanks_fuel_type_id;
DROP INDEX IF EXISTS idx_readings_operational_day_id;
DROP INDEX IF EXISTS idx_readings_nozzle_id;
DROP INDEX IF EXISTS idx_readings_recorded_at;
DROP INDEX IF EXISTS idx_readings_created_at;
DROP INDEX IF EXISTS idx_sales_operational_day_id;
DROP INDEX IF EXISTS idx_sales_station_id;
DROP INDEX IF EXISTS idx_sales_customer_id;
DROP INDEX IF EXISTS idx_sales_payment_method_id;
DROP INDEX IF EXISTS idx_sales_sale_date;
DROP INDEX IF EXISTS idx_sales_status;
DROP INDEX IF EXISTS idx_sales_created_at;
DROP INDEX IF EXISTS idx_sale_items_sale_id;
DROP INDEX IF EXISTS idx_sale_items_fuel_type_id;
DROP INDEX IF EXISTS idx_sale_items_nozzle_id;
DROP INDEX IF EXISTS idx_purchases_supplier_id;
DROP INDEX IF EXISTS idx_purchases_station_id;
DROP INDEX IF EXISTS idx_purchases_operational_day_id;
DROP INDEX IF EXISTS idx_purchases_purchase_date;
DROP INDEX IF EXISTS idx_purchases_status;
DROP INDEX IF EXISTS idx_purchase_items_purchase_id;
DROP INDEX IF EXISTS idx_purchase_items_tank_id;
DROP INDEX IF EXISTS idx_purchase_items_fuel_type_id;
DROP INDEX IF EXISTS idx_inv_movements_station_id;
DROP INDEX IF EXISTS idx_inv_movements_tank_id;
DROP INDEX IF EXISTS idx_inv_movements_fuel_type_id;
DROP INDEX IF EXISTS idx_inv_movements_type;
DROP INDEX IF EXISTS idx_inv_movements_ref;
DROP INDEX IF EXISTS idx_inv_movements_created_at;
DROP INDEX IF EXISTS idx_cashboxes_station_id;
DROP INDEX IF EXISTS idx_cash_tx_cashbox_id;
DROP INDEX IF EXISTS idx_cash_tx_operational_day_id;
DROP INDEX IF EXISTS idx_cash_tx_type;
DROP INDEX IF EXISTS idx_cash_tx_ref;
DROP INDEX IF EXISTS idx_cash_tx_created_at;
DROP INDEX IF EXISTS idx_fin_accounts_station_id;
DROP INDEX IF EXISTS idx_fin_tx_account_id;
DROP INDEX IF EXISTS idx_fin_tx_operational_day_id;
DROP INDEX IF EXISTS idx_fin_tx_type;
DROP INDEX IF EXISTS idx_fin_tx_ref;
DROP INDEX IF EXISTS idx_fin_tx_created_at;
DROP INDEX IF EXISTS idx_customers_station_id;
DROP INDEX IF EXISTS idx_customer_tx_customer_id;
DROP INDEX IF EXISTS idx_customer_tx_operational_day_id;
DROP INDEX IF EXISTS idx_customer_tx_type;
DROP INDEX IF EXISTS idx_customer_tx_ref;
DROP INDEX IF EXISTS idx_customer_tx_created_at;
DROP INDEX IF EXISTS idx_suppliers_station_id;
DROP INDEX IF EXISTS idx_supplier_tx_supplier_id;
DROP INDEX IF EXISTS idx_supplier_tx_operational_day_id;
DROP INDEX IF EXISTS idx_supplier_tx_type;
DROP INDEX IF EXISTS idx_supplier_tx_ref;
DROP INDEX IF EXISTS idx_supplier_tx_created_at;
DROP INDEX IF EXISTS idx_employees_station_id;
DROP INDEX IF EXISTS idx_employees_status;
DROP INDEX IF EXISTS idx_expenses_station_id;
DROP INDEX IF EXISTS idx_expenses_operational_day_id;
DROP INDEX IF EXISTS idx_expenses_category_id;
DROP INDEX IF EXISTS idx_expenses_date;
DROP INDEX IF EXISTS idx_expenses_created_at;
DROP INDEX IF EXISTS idx_variances_station_id;
DROP INDEX IF EXISTS idx_variances_operational_day_id;
DROP INDEX IF EXISTS idx_variances_type;
DROP INDEX IF EXISTS idx_variances_status;
DROP INDEX IF EXISTS idx_variances_ref;
DROP INDEX IF EXISTS idx_variances_created_at;
DROP INDEX IF EXISTS idx_attachments_station_id;
DROP INDEX IF EXISTS idx_attachments_entity;
DROP INDEX IF EXISTS idx_audit_logs_user_id;
DROP INDEX IF EXISTS idx_audit_logs_station_id;
DROP INDEX IF EXISTS idx_audit_logs_entity;
DROP INDEX IF EXISTS idx_audit_logs_action;
DROP INDEX IF EXISTS idx_audit_logs_created_at;
DROP INDEX IF EXISTS idx_settings_station_id;
DROP INDEX IF EXISTS idx_settings_key;
