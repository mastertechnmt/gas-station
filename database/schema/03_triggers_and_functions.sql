-- ==============================================================================
-- 03_triggers_and_functions.sql: Automated Triggers and Validation Functions
-- ==============================================================================

-- 1. Generic function to update updated_at timestamp
CREATE OR REPLACE FUNCTION fn_update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply updated_at triggers across entities
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

-- 2. Validation Trigger for Fuel Prices Overlap Prevention
CREATE OR REPLACE FUNCTION fn_check_fuel_price_overlap()
RETURNS TRIGGER AS $$
DECLARE
    overlap_count INT;
BEGIN
    -- Check if another price entry for the same fuel_type_id overlaps with the new interval
    SELECT COUNT(*)
    INTO overlap_count
    FROM fuel_prices
    WHERE fuel_type_id = NEW.fuel_type_id
      AND id <> COALESCE(NEW.id, '00000000-0000-0000-0000-000000000000'::uuid)
      AND (
          (effective_to IS NULL AND (NEW.effective_to IS NULL OR NEW.effective_to > effective_from))
          OR
          (NEW.effective_to IS NULL AND NEW.effective_from < COALESCE(effective_to, 'infinity'::timestamptz))
          OR
          (NEW.effective_from, COALESCE(NEW.effective_to, 'infinity'::timestamptz)) OVERLAPS (effective_from, COALESCE(effective_to, 'infinity'::timestamptz))
      );

    IF overlap_count > 0 THEN
        RAISE EXCEPTION 'Fuel price period overlaps with an existing price interval for fuel_type_id %', NEW.fuel_type_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_check_fuel_price_overlap ON fuel_prices;
CREATE TRIGGER trg_check_fuel_price_overlap
BEFORE INSERT OR UPDATE ON fuel_prices
FOR EACH ROW
EXECUTE FUNCTION fn_check_fuel_price_overlap();
