-- ==============================================================================
-- atomic_procedures.sql: Atomic Stored Procedures & Business Transactions
-- ==============================================================================

-- 1. RECORD READING + SALE + INVENTORY + PAYMENT (ATOMIC)
CREATE OR REPLACE FUNCTION sp_record_reading_and_sale(
    p_operational_day_id UUID,
    p_nozzle_id UUID,
    p_current_reading DECIMAL,
    p_payment_method_code VARCHAR,
    p_customer_id UUID DEFAULT NULL,
    p_financial_account_id UUID DEFAULT NULL,
    p_recorded_by UUID DEFAULT NULL,
    p_notes TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_day_status VARCHAR;
    v_station_id UUID;
    v_fuel_type_id UUID;
    v_tank_id UUID;
    v_previous_reading DECIMAL;
    v_unit_price DECIMAL;
    v_sold_quantity DECIMAL;
    v_sales_amount DECIMAL;
    v_reading_id UUID;
    v_sale_id UUID;
    v_payment_method_id UUID;
    v_tank_qty_before DECIMAL;
    v_tank_qty_after DECIMAL;
    v_sale_num VARCHAR;
    v_cashbox_id UUID;
    v_cash_balance_before DECIMAL;
    v_cash_balance_after DECIMAL;
    v_fin_balance_before DECIMAL;
    v_fin_balance_after DECIMAL;
    v_cust_balance_before DECIMAL;
    v_cust_balance_after DECIMAL;
BEGIN
    -- 1. Verify Operational Day is Open
    SELECT status, station_id INTO v_day_status, v_station_id
    FROM operational_days
    WHERE id = p_operational_day_id;

    IF v_day_status IS NULL THEN
        RAISE EXCEPTION 'Operational Day not found: %', p_operational_day_id;
    END IF;
    IF v_day_status <> 'open' THEN
        RAISE EXCEPTION 'Cannot record reading on closed operational day: %', p_operational_day_id;
    END IF;

    -- 2. Fetch Nozzle, Fuel Type & Connected Tank
    SELECT n.fuel_type_id, t.id, t.current_quantity
    INTO v_fuel_type_id, v_tank_id, v_tank_qty_before
    FROM nozzles n
    JOIN pumps p ON p.id = n.pump_id
    JOIN tanks t ON t.station_id = p.station_id AND t.fuel_type_id = n.fuel_type_id
    WHERE n.id = p_nozzle_id
    LIMIT 1;

    IF v_fuel_type_id IS NULL OR v_tank_id IS NULL THEN
        RAISE EXCEPTION 'Nozzle % has no valid fuel type or linked tank', p_nozzle_id;
    END IF;

    -- 3. Determine Previous Reading
    SELECT current_reading INTO v_previous_reading
    FROM readings
    WHERE nozzle_id = p_nozzle_id
    ORDER BY recorded_at DESC, created_at DESC
    LIMIT 1;

    IF v_previous_reading IS NULL THEN
        v_previous_reading := 0.0;
    END IF;

    -- 4. Constraint Check: current_reading >= previous_reading
    IF p_current_reading < v_previous_reading THEN
        RAISE EXCEPTION 'Current reading (%) cannot be less than previous reading (%)', p_current_reading, v_previous_reading;
    END IF;

    v_sold_quantity := p_current_reading - v_previous_reading;

    -- 5. Determine Active Fuel Price
    SELECT price INTO v_unit_price
    FROM fuel_prices
    WHERE fuel_type_id = v_fuel_type_id
      AND CURRENT_TIMESTAMP >= effective_from
      AND (effective_to IS NULL OR CURRENT_TIMESTAMP <= effective_to)
    ORDER BY effective_from DESC
    LIMIT 1;

    IF v_unit_price IS NULL OR v_unit_price <= 0 THEN
        RAISE EXCEPTION 'No active price configured for fuel type: %', v_fuel_type_id;
    END IF;

    v_sales_amount := ROUND((v_sold_quantity * v_unit_price)::numeric, 2);

    -- 6. Insert Reading
    INSERT INTO readings (
        operational_day_id, nozzle_id, previous_reading, current_reading,
        sold_quantity, fuel_price, sales_amount, recorded_by, notes
    ) VALUES (
        p_operational_day_id, p_nozzle_id, v_previous_reading, p_current_reading,
        v_sold_quantity, v_unit_price, v_sales_amount, p_recorded_by, p_notes
    ) RETURNING id INTO v_reading_id;

    -- 7. Update Tank & Record Inventory Movement
    v_tank_qty_after := GREATEST(0.0, v_tank_qty_before - v_sold_quantity);

    UPDATE tanks
    SET current_quantity = v_tank_qty_after,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = v_tank_id;

    INSERT INTO inventory_movements (
        station_id, tank_id, fuel_type_id, movement_type, quantity,
        reference_type, reference_id, quantity_before, quantity_after,
        created_by, notes
    ) VALUES (
        v_station_id, v_tank_id, v_fuel_type_id, 'sale', v_sold_quantity,
        'readings', v_reading_id, v_tank_qty_before, v_tank_qty_after,
        p_recorded_by, 'مبيعات عداد مسدس'
    );

    -- 8. Fetch Payment Method
    SELECT id INTO v_payment_method_id
    FROM payment_methods
    WHERE code = p_payment_method_code;

    IF v_payment_method_id IS NULL THEN
        RAISE EXCEPTION 'Invalid payment method code: %', p_payment_method_code;
    END IF;

    -- 9. Create Sale Record & Sale Item
    v_sale_num := 'SL-' || TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD-HH24MISS') || '-' || SUBSTRING(v_reading_id::text, 1, 4);

    INSERT INTO sales (
        operational_day_id, station_id, sale_number, sale_date,
        total_amount, total_quantity, payment_method_id, customer_id,
        status, created_by
    ) VALUES (
        p_operational_day_id, v_station_id, v_sale_num, CURRENT_TIMESTAMP,
        v_sales_amount, v_sold_quantity, v_payment_method_id, p_customer_id,
        'completed', p_recorded_by
    ) RETURNING id INTO v_sale_id;

    INSERT INTO sale_items (
        sale_id, fuel_type_id, nozzle_id, quantity, unit_price, total_amount
    ) VALUES (
        v_sale_id, v_fuel_type_id, p_nozzle_id, v_sold_quantity, v_unit_price, v_sales_amount
    );

    -- 10. Update Cashbox / Bank / Customer depending on payment method
    IF p_payment_method_code = 'cash' THEN
        SELECT id, current_balance INTO v_cashbox_id, v_cash_balance_before
        FROM cashboxes
        WHERE station_id = v_station_id AND is_active = true
        LIMIT 1;

        IF v_cashbox_id IS NOT NULL THEN
            v_cash_balance_after := v_cash_balance_before + v_sales_amount;
            UPDATE cashboxes SET current_balance = v_cash_balance_after WHERE id = v_cashbox_id;

            INSERT INTO cash_transactions (
                cashbox_id, operational_day_id, transaction_type, amount,
                balance_before, balance_after, reference_type, reference_id,
                description, created_by
            ) VALUES (
                v_cashbox_id, p_operational_day_id, 'sale', v_sales_amount,
                v_cash_balance_before, v_cash_balance_after, 'sales', v_sale_id,
                'مبيعات نقدية من العداد', p_recorded_by
            );
        END IF;

    ELSIF p_payment_method_code = 'credit' THEN
        IF p_customer_id IS NULL THEN
            RAISE EXCEPTION 'Customer ID is required for credit sales';
        END IF;

        -- Get customer last balance
        SELECT COALESCE(balance_after, 0.0) INTO v_cust_balance_before
        FROM customer_transactions
        WHERE customer_id = p_customer_id
        ORDER BY created_at DESC LIMIT 1;

        IF v_cust_balance_before IS NULL THEN v_cust_balance_before := 0.0; END IF;
        v_cust_balance_after := v_cust_balance_before + v_sales_amount;

        INSERT INTO customer_transactions (
            customer_id, operational_day_id, transaction_type, amount,
            balance_before, balance_after, reference_type, reference_id,
            description, created_by
        ) VALUES (
            p_customer_id, p_operational_day_id, 'credit_sale', v_sales_amount,
            v_cust_balance_before, v_cust_balance_after, 'sales', v_sale_id,
            'مبيعات وقود آجلة', p_recorded_by
        );

    ELSIF p_payment_method_code = 'financial_account' THEN
        IF p_financial_account_id IS NULL THEN
            RAISE EXCEPTION 'Financial Account ID is required for bank/wallet payments';
        END IF;

        SELECT current_balance INTO v_fin_balance_before
        FROM financial_accounts
        WHERE id = p_financial_account_id;

        v_fin_balance_after := v_fin_balance_before + v_sales_amount;
        UPDATE financial_accounts SET current_balance = v_fin_balance_after WHERE id = p_financial_account_id;

        INSERT INTO financial_transactions (
            financial_account_id, operational_day_id, transaction_type, amount,
            balance_before, balance_after, reference_type, reference_id,
            description, created_by
        ) VALUES (
            p_financial_account_id, p_operational_day_id, 'sale', v_sales_amount,
            v_fin_balance_before, v_fin_balance_after, 'sales', v_sale_id,
            'إيداع مبيعات وقود بنكية', p_recorded_by
        );
    END IF;

    RETURN jsonb_build_object(
        'success', true,
        'reading_id', v_reading_id,
        'sale_id', v_sale_id,
        'sold_quantity', v_sold_quantity,
        'sales_amount', v_sales_amount,
        'tank_id', v_tank_id,
        'tank_quantity_after', v_tank_qty_after
    );
END;
$$ LANGUAGE plpgsql;

-- 2. RECEIVE FUEL PURCHASE + UPDATE INVENTORY + UPDATE SUPPLIER LIABILITY (ATOMIC)
CREATE OR REPLACE FUNCTION sp_receive_fuel_purchase(
    p_operational_day_id UUID,
    p_supplier_id UUID,
    p_tank_id UUID,
    p_quantity DECIMAL,
    p_unit_price DECIMAL,
    p_invoice_number VARCHAR,
    p_created_by UUID DEFAULT NULL,
    p_notes TEXT DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_station_id UUID;
    v_fuel_type_id UUID;
    v_tank_capacity DECIMAL;
    v_tank_qty_before DECIMAL;
    v_tank_qty_after DECIMAL;
    v_total_amount DECIMAL;
    v_purchase_id UUID;
    v_sup_balance_before DECIMAL;
    v_sup_balance_after DECIMAL;
BEGIN
    IF p_quantity <= 0 THEN
        RAISE EXCEPTION 'Purchase quantity must be greater than zero: %', p_quantity;
    END IF;

    -- Fetch tank & fuel type
    SELECT station_id, fuel_type_id, capacity, current_quantity
    INTO v_station_id, v_fuel_type_id, v_tank_capacity, v_tank_qty_before
    FROM tanks
    WHERE id = p_tank_id;

    IF v_station_id IS NULL THEN
        RAISE EXCEPTION 'Tank not found: %', p_tank_id;
    END IF;

    v_total_amount := ROUND((p_quantity * p_unit_price)::numeric, 2);
    v_tank_qty_after := LEAST(v_tank_capacity, v_tank_qty_before + p_quantity);

    -- 1. Create Purchase & Item
    INSERT INTO purchases (
        supplier_id, station_id, operational_day_id, invoice_number,
        purchase_date, total_amount, status, notes, created_by
    ) VALUES (
        p_supplier_id, v_station_id, p_operational_day_id, p_invoice_number,
        CURRENT_TIMESTAMP, v_total_amount, 'received', p_notes, p_created_by
    ) RETURNING id INTO v_purchase_id;

    INSERT INTO purchase_items (
        purchase_id, fuel_type_id, tank_id, quantity, unit_price, total_amount
    ) VALUES (
        v_purchase_id, v_fuel_type_id, p_tank_id, p_quantity, p_unit_price, v_total_amount
    );

    -- 2. Update Tank Current Stock
    UPDATE tanks
    SET current_quantity = v_tank_qty_after,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_tank_id;

    -- 3. Record Inventory Movement
    INSERT INTO inventory_movements (
        station_id, tank_id, fuel_type_id, movement_type, quantity,
        reference_type, reference_id, quantity_before, quantity_after,
        created_by, notes
    ) VALUES (
        v_station_id, p_tank_id, v_fuel_type_id, 'purchase', p_quantity,
        'purchases', v_purchase_id, v_tank_qty_before, v_tank_qty_after,
        p_created_by, 'توريد شحنة وقود - فاتورة ' || p_invoice_number
    );

    -- 4. Update Supplier Ledger
    SELECT COALESCE(balance_after, 0.0) INTO v_sup_balance_before
    FROM supplier_transactions
    WHERE supplier_id = p_supplier_id
    ORDER BY created_at DESC LIMIT 1;

    IF v_sup_balance_before IS NULL THEN v_sup_balance_before := 0.0; END IF;
    v_sup_balance_after := v_sup_balance_before + v_total_amount;

    INSERT INTO supplier_transactions (
        supplier_id, operational_day_id, transaction_type, amount,
        balance_before, balance_after, reference_type, reference_id,
        description, created_by
    ) VALUES (
        p_supplier_id, p_operational_day_id, 'purchase', v_total_amount,
        v_sup_balance_before, v_sup_balance_after, 'purchases', v_purchase_id,
        'فاتورة توريد وقود رقم ' || p_invoice_number, p_created_by
    );

    RETURN jsonb_build_object(
        'success', true,
        'purchase_id', v_purchase_id,
        'total_amount', v_total_amount,
        'tank_id', p_tank_id,
        'tank_quantity_after', v_tank_qty_after,
        'supplier_balance_after', v_sup_balance_after
    );
END;
$$ LANGUAGE plpgsql;

-- 3. RECORD EXPENSE + DEDUCT CASH / ACCOUNT (ATOMIC)
CREATE OR REPLACE FUNCTION sp_record_expense(
    p_operational_day_id UUID,
    p_category_id UUID,
    p_amount DECIMAL,
    p_payment_source_type VARCHAR, -- 'cash' or 'financial_account'
    p_source_id UUID,
    p_beneficiary VARCHAR,
    p_voucher_number VARCHAR,
    p_description TEXT,
    p_created_by UUID DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_station_id UUID;
    v_expense_id UUID;
    v_bal_before DECIMAL;
    v_bal_after DECIMAL;
BEGIN
    IF p_amount <= 0 THEN
        RAISE EXCEPTION 'Expense amount must be greater than zero: %', p_amount;
    END IF;

    SELECT station_id INTO v_station_id
    FROM operational_days WHERE id = p_operational_day_id;

    IF p_payment_source_type = 'cash' THEN
        SELECT current_balance INTO v_bal_before FROM cashboxes WHERE id = p_source_id;
        IF v_bal_before IS NULL THEN RAISE EXCEPTION 'Cashbox not found: %', p_source_id; END IF;
        IF v_bal_before < p_amount THEN RAISE EXCEPTION 'Insufficient cashbox balance: has %, needs %', v_bal_before, p_amount; END IF;

        v_bal_after := v_bal_before - p_amount;
        UPDATE cashboxes SET current_balance = v_bal_after WHERE id = p_source_id;

        INSERT INTO expenses (
            station_id, operational_day_id, category_id, amount,
            payment_source_type, cashbox_id, beneficiary, voucher_number,
            description, created_by
        ) VALUES (
            v_station_id, p_operational_day_id, p_category_id, p_amount,
            'cash', p_source_id, p_beneficiary, p_voucher_number,
            p_description, p_created_by
        ) RETURNING id INTO v_expense_id;

        INSERT INTO cash_transactions (
            cashbox_id, operational_day_id, transaction_type, amount,
            balance_before, balance_after, reference_type, reference_id,
            description, created_by
        ) VALUES (
            p_source_id, p_operational_day_id, 'expense', p_amount,
            v_bal_before, v_bal_after, 'expenses', v_expense_id,
            'سند صرف نقدي - ' || COALESCE(p_description, ''), p_created_by
        );

    ELSIF p_payment_source_type = 'financial_account' THEN
        SELECT current_balance INTO v_bal_before FROM financial_accounts WHERE id = p_source_id;
        IF v_bal_before IS NULL THEN RAISE EXCEPTION 'Financial Account not found: %', p_source_id; END IF;
        IF v_bal_before < p_amount THEN RAISE EXCEPTION 'Insufficient account balance: has %, needs %', v_bal_before, p_amount; END IF;

        v_bal_after := v_bal_before - p_amount;
        UPDATE financial_accounts SET current_balance = v_bal_after WHERE id = p_source_id;

        INSERT INTO expenses (
            station_id, operational_day_id, category_id, amount,
            payment_source_type, financial_account_id, beneficiary, voucher_number,
            description, created_by
        ) VALUES (
            v_station_id, p_operational_day_id, p_category_id, p_amount,
            'financial_account', p_source_id, p_beneficiary, p_voucher_number,
            p_description, p_created_by
        ) RETURNING id INTO v_expense_id;

        INSERT INTO financial_transactions (
            financial_account_id, operational_day_id, transaction_type, amount,
            balance_before, balance_after, reference_type, reference_id,
            description, created_by
        ) VALUES (
            p_source_id, p_operational_day_id, 'expense', p_amount,
            v_bal_before, v_bal_after, 'expenses', v_expense_id,
            'سند صرف بنكي - ' || COALESCE(p_description, ''), p_created_by
        );
    ELSE
        RAISE EXCEPTION 'Invalid payment source type: %', p_payment_source_type;
    END IF;

    RETURN jsonb_build_object(
        'success', true,
        'expense_id', v_expense_id,
        'amount', p_amount,
        'balance_after', v_bal_after
    );
END;
$$ LANGUAGE plpgsql;

-- 4. TRANSFER BETWEEN FINANCIAL ACCOUNTS (ATOMIC)
CREATE OR REPLACE FUNCTION sp_transfer_financial_accounts(
    p_operational_day_id UUID,
    p_from_account_id UUID,
    p_to_account_id UUID,
    p_amount DECIMAL,
    p_description TEXT,
    p_created_by UUID DEFAULT NULL
)
RETURNS JSONB AS $$
DECLARE
    v_from_bal_before DECIMAL;
    v_from_bal_after DECIMAL;
    v_to_bal_before DECIMAL;
    v_to_bal_after DECIMAL;
BEGIN
    IF p_amount <= 0 THEN RAISE EXCEPTION 'Transfer amount must be positive'; END IF;
    IF p_from_account_id = p_to_account_id THEN RAISE EXCEPTION 'Source and destination accounts must be different'; END IF;

    SELECT current_balance INTO v_from_bal_before FROM financial_accounts WHERE id = p_from_account_id FOR UPDATE;
    SELECT current_balance INTO v_to_bal_before FROM financial_accounts WHERE id = p_to_account_id FOR UPDATE;

    IF v_from_bal_before < p_amount THEN
        RAISE EXCEPTION 'Insufficient balance in source account: has %, needs %', v_from_bal_before, p_amount;
    END IF;

    v_from_bal_after := v_from_bal_before - p_amount;
    v_to_bal_after := v_to_bal_before + p_amount;

    UPDATE financial_accounts SET current_balance = v_from_bal_after WHERE id = p_from_account_id;
    UPDATE financial_accounts SET current_balance = v_to_bal_after WHERE id = p_to_account_id;

    INSERT INTO financial_transactions (
        financial_account_id, operational_day_id, transaction_type, amount,
        balance_before, balance_after, description, created_by
    ) VALUES (
        p_from_account_id, p_operational_day_id, 'transfer_out', p_amount,
        v_from_bal_before, v_from_bal_after, 'تحويل صادر: ' || COALESCE(p_description, ''), p_created_by
    );

    INSERT INTO financial_transactions (
        financial_account_id, operational_day_id, transaction_type, amount,
        balance_before, balance_after, description, created_by
    ) VALUES (
        p_to_account_id, p_operational_day_id, 'transfer_in', p_amount,
        v_to_bal_before, v_to_bal_after, 'تحويل وارد: ' || COALESCE(p_description, ''), p_created_by
    );

    RETURN jsonb_build_object(
        'success', true,
        'from_balance_after', v_from_bal_after,
        'to_balance_after', v_to_bal_after
    );
END;
$$ LANGUAGE plpgsql;
