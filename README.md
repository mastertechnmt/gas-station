# نظام إدارة محطة الوقود (Fuel Station Management System)

نظام متكامل واحترافي لإدارة وتشغيل محطة الوقود اليومية، مبني وفق أحدث المعايير البرمجية (Clean Architecture & Jetpack Compose & Room Database with Atomic Transactions).

---

## 1. المميزات الرئيسية للنظام

1. **إدارة اليوم التشغيلي (Operational Day)**:
   - مفهوم اليوم المفتوح والمغلق لمنع التلاعب في العمليات الحساسة.
   - جرد النقد الفعلي ومطابقته آلياً مع الرصيد الدفتري المتوقع.
   - رصد الفروقات (العجز أو الفائض) وتوثيقها.

2. **قراءات العدادات والمبيعات الآلية (Readings & Sales)**:
   - تسجيل قراءة العداد لكل مسدس (Nozzle).
   - منع إدخال قراءة حالية أقل من القراءة السابقة.
   - احتساب كمية اللترات المباعة آلياً: `Sold Liters = Current - Previous`.
   - احتساب قيمة المبيعات آلياً: `Sales Amount = Liters × Price`.
   - دعم طرق الدفع المتعددة: نقداً (الخزينة)، حساب بنكي / محفظة إلكترونية، أو بيع آجل (ذمم عملاء).

3. **سجل حركة المخزون والخزانات (Inventory Ledger & Tanks)**:
   - مراقبة سعة ومخزون الخزانات مع مؤشرات حالة حية (مستقر، منخفض، حرج).
   - توثيق كل حركة مخزنية (توريدات، مبيعات، تسويات) بسجل `inventory_movements`.

4. **التوريدات والشراء (Purchases & Deliveries)**:
   - تسجيل فواتير استلام شحنات الوقود من الموردين.
   - زيادة رصيد الخزانات وتحديث حساب والتزام المورد تلقائياً.

5. **الخزينة والحسابات المالية (Cashbox & Accounts)**:
   - إدارة الخزينة النقدية الرئيسية.
   - إدارة الحسابات البنكية والمحافظ الإلكترونية (الكريمي، جوالي، وغيرها).
   - التحويل المالي الذري (Atomic Transfer) بين الحسابات.

6. **المصروفات التشغيلية (Expenses)**:
   - تصنيفات المصروفات: صيانة، رواتب، كهرباء، ماء، نقل، مشتريات، وغيرها.
   - إسناد سندات الصرف للمستفيدين وخصمها من الخزينة أو البنك.

7. **حسابات العملاء والموردين (Ledgers)**:
   - متابعة الديون الآجلة للعملاء وسندات التحصيل.
   - متابعة مستحقات الموردين وسندات السداد.

8. **سجل التدقيق والرقابة (Audit Log)**:
   - توثيق كافة العمليات (تسجيل الدخول، الإضافة، التعديل، إغلاق اليوم، الحركات المالية).

---

## 2. الهيكلية المعمارية (Architecture)

```
app/src/main/java/com/example/fuelstation/
├── data/
│   ├── local/
│   │   ├── entities/        # 25 Normalized Relational Entities
│   │   ├── dao/             # Room DAO with reactive Flows & queries
│   │   └── FuelStationDatabase.kt
│   ├── repository/          # FuelStationRepository with ACID transactions
│   └── seed/                # DatabaseSeeder (Demo initialization)
├── ui/
│   ├── components/          # CommonComponents, OperationDialogs, MasterDataDialogs
│   ├── screens/             # Login, Dashboard, Day, Readings, Inventory, Reports, More
│   ├── FuelStationViewModel.kt
│   └── FuelStationApp.kt
└── ui/theme/                # Colors, Typography, Shapes, FuelStationTheme
```

---

## 3. تفاصيل قاعدة البيانات (Database Schema)

- `users`: المستخدمين ومدراء النظام.
- `station`: بيانات المحطة والعملة (ريال يمني).
- `operational_days`: الأيام التشغيلية وحالة اليوم وأرصدة الجرد.
- `fuel_types`: المشتقات النفطية (بنزين 91، بنزين 95، ديزل).
- `fuel_prices`: السجل التاريخي لتغير أسعار الوقود.
- `pumps`: الطرمبات والمضخات.
- `nozzles`: مسدسات التعبئة وربطها بالخزان والعداد.
- `tanks`: خزانات الوقود وسعاتها والحدود الحرجة.
- `readings`: قراءات العدادات المحسوبة.
- `sales`: المبيعات المرتبطة بالقراءات وطرق الدفع.
- `inventory_movements`: حركات المخزون الداخلة والخارجة.
- `purchases`: فواتير توريد الوقود.
- `suppliers` & `supplier_transactions`: الموردون وحركات الحساب.
- `customers` & `customer_transactions`: العملاء والمبيعات الآجلة.
- `cashbox` & `cash_transactions`: الخزينة النقدية وسندات الصرف والقبض.
- `financial_accounts` & `financial_transactions`: البنوك والمحافظ.
- `expenses` & `expense_categories`: المصروفات التشغيلية.
- `employees`: موظفو وعمال المحطة.
- `variances`: سجل الفروقات والتسويات.
- `audit_logs`: سجل الرقابة والتدقيق الأمني.

---

## 4. بيانات تسجيل الدخول التجريبية (Demo Credentials)

- **اسم المستخدم**: `admin`
- **كلمة المرور**: `123456`
- **الدور**: مدير المحطة (بصلاحيات كاملة)
- **خيار العمل بدون إنترنت**: متاح عبر تفعيل "دخول بدون نت"
