# دليل قاعدة البيانات والطبقة الخلفية لنظام إدارة محطة الوقود
## Fuel Station Database & Backend Layer Documentation

قاعدة بيانات علائقية كاملة وعالية الأداء (Fully Normalized Relational Database) مبنية ومجهزة على محرك **PostgreSQL 16** لنظام **إدارة محطة الوقود**، مع نظام هجرات (Migrations)، بيانات أولية (Seeds)، قيود تكامل، فهارس تسريع، وإجراءات ذرية (Atomic Stored Procedures).

---

## 1. بنية الملفات (Directory Structure)

```
.
├── docker-compose.yml                     # إعداد PostgreSQL والحاوية وحفظ البيانات بالفوليوم
├── .env.example                           # متغيرات البيئة وبيانات الاتصال النموذجية
├── .gitignore                             # استبعاد ملفات البيئة والمجلدات الحساسة
└── database/
    ├── README.md                          # التوثيق الشامل لقاعدة البيانات
    ├── schema/
    │   ├── 00_init_extensions.sql         # تفعيل إضافات PostgreSQL (UUID, pgcrypto, btree_gist)
    │   ├── 01_create_tables.sql           # إنشاء الـ 30 جدولاً مع القيود والمفاتيح
    │   ├── 02_indexes.sql                 # الفهارس الشاملة للفلاتر والتواريخ والبحث
    │   └── 03_triggers_and_functions.sql  # المشغلات التلقائية لمنع التداخل وتحديث updated_at
    ├── migrations/
    │   ├── 001_initial_schema.up.sql      # هجرة الإنشاء (الجداول الأساسية والقيود)
    │   ├── 001_initial_schema.down.sql    # هجرة التراجع (حذف الجداول بترتيب عكسي)
    │   ├── 002_add_constraints_indexes.up.sql    # هجرة إضافة الفهارس والمشغلات
    │   ├── 002_add_constraints_indexes.down.sql  # هجرة التراجع عن الفهارس والمشغلات
    │   ├── 003_seed_master_data.up.sql    # هجرة البيانات الأولية الأساسية
    │   └── 003_seed_master_data.down.sql  # هجرة التراجع عن البيانات الأولية
    ├── seed/
    │   └── seed_demo_data.sql             # سكريبت حقن البيانات التجريبية الشاملة
    ├── transactions/
    │   └── atomic_procedures.sql          # الإجراءات المخزنة للعمليات الذرية (Atomic Transactions)
    └── scripts/
        ├── migrate.js                     # مشغل الهجرات (up / down / status)
        ├── seed.js                        # مشغل حقن البيانات
        └── validate_schema.py             # أداة التحقق الشامل من تكامل وهيكلية المخطط
```

---

## 2. الجداول الـ 30 الأساسية (Normalized Entities)

| # | اسم الجدول | الوصف والوظيفة |
|---|------------|----------------|
| 1 | `users` | المستخدمين ومدراء النظام وكلمات المرور المشفرة |
| 2 | `stations` | بيانات المحطة والعملة (YER) والمنطقة الزمنية |
| 3 | `operational_days` | الأيام التشغيلية وحالة اليوم مع منع فتح أكثر من يوم في نفس الوقت |
| 4 | `fuel_types` | أنواع الوقود (بنزين 91، بنزين 95، ديزل) |
| 5 | `fuel_prices` | الأسعار التاريخية مع منع تداخل فترات الصلاحية |
| 6 | `pumps` | الطرمبات والمضخات داخل المحطة |
| 7 | `nozzles` | مسدسات التعبئة وربطها بالمضخات وأنواع الوقود |
| 8 | `tanks` | خزانات الوقود وسعاتها والحدود الحرجة للتنبيه |
| 9 | `readings` | قراءات العدادات المحسوبة آلياً (`current >= previous`) |
| 10 | `payment_methods` | طرق الدفع (نقد، حساب مالي / محفظة، آجل) |
| 11 | `customers` | حسابات العملاء المعتمدين للبيع الآجل |
| 12 | `sales` | سجل الفواتير والمبيعات اليومية |
| 13 | `sale_items` | بنود الفاتورة بالكمية وسعر اللتر الفعلي وقت البيع |
| 14 | `suppliers` | موردي المشتقات النفطية والشحنات |
| 15 | `purchases` | فواتير استلام شحنات وتوريدات الوقود |
| 16 | `purchase_items` | تفاصيل كميات التوريد لكل خزان وسعر الشراء |
| 17 | `inventory_movements` | **دفتر أستاذ المخزون (Source of Truth)** لحركات الخزانات |
| 18 | `cashboxes` | الخزينة النقدية الرئيسية للمحطة |
| 19 | `cash_transactions` | حركات القبض والصرف والإيداع وسندات الصندوق |
| 20 | `financial_accounts` | الحسابات البنكية والمحافظ الإلكترونية (الكريمي/جوالي) |
| 21 | `financial_transactions` | حركات السحب والإيداع والتحويل بين الحسابات |
| 22 | `customer_transactions` | دفتر أستاذ مديونيات ودفعات العملاء |
| 23 | `supplier_transactions` | دفتر أستاذ مستحقات وسداد الموردين |
| 24 | `employees` | موظفو وعمال المحطة وبياناتهم التشغيلية |
| 25 | `expense_categories` | تصنيفات المصروفات (صيانة، رواتب، كهرباء، ماء...) |
| 26 | `expenses` | سندات الصرف للمستفيدين والخصم المالي المرتبط |
| 27 | `variances` | سجل الفروقات وعجز/فائض الجرد للمطابقة والتدقيق |
| 28 | `attachments` | المرفقات ومسارات الملفات والأحجام (دون حفظ Binary) |
| 29 | `audit_logs` | السجل الأمني للرقابة وتتبع التعديلات بحقول JSONB |
| 30 | `settings` | إعدادات ومحددات النظام والمحطة الديناميكية |

---

## 3. طريقة تشغيل PostgreSQL محلياً عبر Docker

### أ. إنشاء ملف المتغيرات:
قم بنسخ ملف الإعدادات:
```bash
cp .env.example .env
```

### ب. تشغيل الحاوية:
```bash
docker compose up -d
```

### ج. فحص الحالة والجاهزية (Healthcheck):
```bash
docker compose ps
```
سيظهر أن الحاوية `fuel_station_postgres` تعمل في المنفذ `5432` وحالتها `healthy`.

### د. إيقاف وتشغيل الحاوية دون فقد البيانات:
البيانات محفوظة داخل Docker Volume مخصص باسم `fuel_station_postgres_data`.
```bash
# إيقاف الحاوية
docker compose stop

# إعادة التشغيل
docker compose start

# إزالة الحاوية مع بقاء البيانات في Volume
docker compose down
```

---

## 4. تنفيذ الهجرات (Migrations)

### لتطبيق الهجرات (UP):
```bash
node database/scripts/migrate.js up
```
أو عبر `psql` مباشرة:
```bash
psql -h localhost -p 5432 -U fuel_station -d fuel_station_db -f database/migrations/001_initial_schema.up.sql
psql -h localhost -p 5432 -U fuel_station -d fuel_station_db -f database/migrations/002_add_constraints_indexes.up.sql
psql -h localhost -p 5432 -U fuel_station -d fuel_station_db -f database/migrations/003_seed_master_data.up.sql
```

### للتراجع عن الهجرات (DOWN):
```bash
node database/scripts/migrate.js down
```

---

## 5. حقن البيانات الأولية (Seed Data)

لحقن بيانات المحطة، أنواع الوقود، الخزانات، المضخات، المسدسات، وحساب المدير:
```bash
node database/scripts/seed.js
```
أو عبر `psql`:
```bash
psql -h localhost -p 5432 -U fuel_station -d fuel_station_db -f database/seed/seed_demo_data.sql
```

---

## 6. العمليات الذرية (Atomic Stored Procedures)

تتضمن قاعدة البيانات دوال مخزنة جاهزة تطبق مبدأ **ACID Transactions**:
1. `sp_record_reading_and_sale`:
   - تسجيل قراءة العداد.
   - التحقق من `current_reading >= previous_reading`.
   - احتساب اللترات والمبلغ آلياً.
   - خصم كمية الوقود من الخزان.
   - إضافة قيد في `inventory_movements`.
   - إنشاء الفاتورة وسجل بنود البيع.
   - زيادة رصيد الخزينة (نقد) أو تسجيل مديونية في حساب العميل (آجل) أو إيداع بنكي.
   - في حال فشل أي خطوة يتم التراجع الفوري الكامل (Rollback).
2. `sp_receive_fuel_purchase`:
   - تسجيل فاتورة التوريد.
   - زيادة رصيد الخزان الفعلي.
   - تسجيل حركة المخزون `purchase`.
   - إثبات الالتزام المالي للمورد في `supplier_transactions`.
3. `sp_record_expense`:
   - التحقق من توفر رصيد كافٍ في الخزينة أو الحساب البنكي.
   - خصم المبلغ وتسجيل سند الصرف.
4. `sp_transfer_financial_accounts`:
   - تحويل ذري ومضمون بين حسابين بنكيين أو محفظتين.
