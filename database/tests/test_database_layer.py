#!/usr/bin/env python3
"""
test_database_layer.py: Unit tests for PostgreSQL schema, constraints, migrations and procedures.
"""

import unittest
import os
import re

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SCHEMA_DIR = os.path.join(BASE_DIR, 'schema')
MIGRATIONS_DIR = os.path.join(BASE_DIR, 'migrations')
SEED_DIR = os.path.join(BASE_DIR, 'seed')
TRANSACTIONS_DIR = os.path.join(BASE_DIR, 'transactions')

class TestFuelStationDatabaseLayer(unittest.TestCase):

    def setUp(self):
        with open(os.path.join(SCHEMA_DIR, '01_create_tables.sql'), 'r', encoding='utf-8') as f:
            self.schema_sql = f.read()
        with open(os.path.join(TRANSACTIONS_DIR, 'atomic_procedures.sql'), 'r', encoding='utf-8') as f:
            self.proc_sql = f.read()
        with open(os.path.join(SEED_DIR, 'seed_demo_data.sql'), 'r', encoding='utf-8') as f:
            self.seed_sql = f.read()

    def test_all_30_tables_exist(self):
        expected_tables = [
            'users', 'stations', 'operational_days', 'fuel_types', 'fuel_prices',
            'pumps', 'nozzles', 'tanks', 'readings', 'sales',
            'sale_items', 'payment_methods', 'purchases', 'purchase_items',
            'inventory_movements', 'cashboxes', 'cash_transactions', 'financial_accounts',
            'financial_transactions', 'customers', 'customer_transactions', 'suppliers',
            'supplier_transactions', 'employees', 'expenses', 'expense_categories',
            'variances', 'attachments', 'audit_logs', 'settings'
        ]
        found_tables = re.findall(r'CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?([a-zA-Z0-9_]+)', self.schema_sql, re.IGNORECASE)
        found_set = set(t.lower() for t in found_tables)
        for t in expected_tables:
            self.assertIn(t, found_set, f"Table {t} must exist in 01_create_tables.sql")

    def test_reading_check_constraint(self):
        # Database Constraint: current_reading >= previous_reading
        self.assertIn('chk_reading_sequence', self.schema_sql)
        self.assertIn('current_reading >= previous_reading', self.schema_sql)

    def test_operational_day_unique_open_constraint(self):
        with open(os.path.join(SCHEMA_DIR, '02_indexes.sql'), 'r', encoding='utf-8') as f:
            indexes_sql = f.read()
        self.assertIn('idx_uq_open_day_per_station', indexes_sql)
        self.assertIn("WHERE status = 'open'", indexes_sql)

    def test_inventory_movement_types(self):
        # opening, purchase, sale, adjustment
        self.assertIn('chk_inventory_movement_type', self.schema_sql)
        for m_type in ['opening', 'purchase', 'sale', 'adjustment']:
            self.assertIn(m_type, self.schema_sql)

    def test_seed_demo_data_completeness(self):
        # Check seed has fuel types: G91, G95, DSL
        self.assertIn('بنزين 91', self.seed_sql)
        self.assertIn('بنزين 95', self.seed_sql)
        self.assertIn('ديزل', self.seed_sql)
        # Check pumps
        self.assertIn('طرمبة 1', self.seed_sql)
        self.assertIn('طرمبة 2', self.seed_sql)
        # Check payment methods
        self.assertIn('cash', self.seed_sql)
        self.assertIn('credit', self.seed_sql)
        self.assertIn('financial_account', self.seed_sql)
        # Check expense categories
        for cat in ['صيانة', 'رواتب', 'كهرباء', 'ماء', 'نقل', 'مشتريات', 'أخرى']:
            self.assertIn(cat, self.seed_sql)

    def test_atomic_stored_procedures(self):
        self.assertIn('sp_record_reading_and_sale', self.proc_sql)
        self.assertIn('sp_receive_fuel_purchase', self.proc_sql)
        self.assertIn('sp_record_expense', self.proc_sql)
        self.assertIn('sp_transfer_financial_accounts', self.proc_sql)

    def test_migration_up_down_symmetry(self):
        with open(os.path.join(MIGRATIONS_DIR, '001_initial_schema.up.sql'), 'r', encoding='utf-8') as f:
            up_001 = f.read()
        with open(os.path.join(MIGRATIONS_DIR, '001_initial_schema.down.sql'), 'r', encoding='utf-8') as f:
            down_001 = f.read()
        found_tables = re.findall(r'CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?([a-zA-Z0-9_]+)', up_001, re.IGNORECASE)
        for t in found_tables:
            self.assertIn(f"DROP TABLE IF EXISTS {t}", down_001)

if __name__ == '__main__':
    unittest.main()
