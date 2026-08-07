# Business Calculation Specification
## Swari Sadhan Bike Dealership ERP - Analytics Module

**Version:** 1.0  
**Last Updated:** 2026-07-23  
**Purpose:** Single source of truth for all KPI calculations used in Analytics Dashboard, Reports, Ledgers, GST, and Accounting modules

---

# TABLE OF CONTENTS

1. [Business Overview KPIs](#business-overview-kpis)
2. [Sales & Inventory KPIs](#sales--inventory-kpis)
3. [Financial Overview KPIs](#financial-overview-kpis)
4. [Additional Recommended KPIs](#additional-recommended-kpis)
5. [Date Filter Behavior](#date-filter-behavior)
6. [Vehicle Status Definitions](#vehicle-status-definitions)
7. [Expense Category Rules](#expense-category-rules)
8. [Edge Cases & Business Rules](#edge-cases--business-rules)

---

# BUSINESS OVERVIEW KPIS

## KPI: Vehicles Purchased

**Definition:** Count of vehicles acquired by the dealership in the selected time period

**Formula:**
```
Vehicles Purchased = COUNT(*)
WHERE shop_id = {shopId}
AND bought_date BETWEEN {startDate} AND {endDate}
```

**Data Source:** `vehicles` table  
**Database Fields:** `shop_id`, `bought_date`

**Affected by Date Filter:** YES

**Included Records:**
- All vehicles with `bought_date` in the selected date range
- All vehicle statuses (ACTIVE, SOLD, PENDING_SALE, etc.)

**Excluded Records:**
- Vehicles bought outside the date range
- Deleted/inactive vehicles

**Edge Cases:**
- If no vehicles purchased in period: Returns 0
- If `bought_date` is NULL: Excluded from count

**Business Example:**
```
Period: July 2026
Vehicles Bought:
- Honda Activa on July 5: COUNT = 1
- Royal Enfield on July 15: COUNT = 1
- Honda Activa on June 28: COUNT = 0 (outside period)

Total Vehicles Purchased: 2
```

---

## KPI: Vehicles Sold

**Definition:** Count of vehicles sold to customers in the selected time period

**Formula:**
```
Vehicles Sold = COUNT(*)
WHERE shop_id = {shopId}
AND status = 'SOLD'
AND sold_at BETWEEN {startDate} AND {endDate}
```

**Data Source:** `vehicles` table  
**Database Fields:** `shop_id`, `status`, `sold_at`

**Affected by Date Filter:** YES

**Included Records:**
- Vehicles with status = 'SOLD'
- `sold_at` within selected date range

**Excluded Records:**
- Vehicles with other statuses (ACTIVE, PENDING_SALE, etc.)
- Vehicles sold outside date range
- Cancelled sales (should not have SOLD status)
- Refunded sales (should be tracked separately, not in SOLD status)

**Edge Cases:**
- If no vehicles sold in period: Returns 0
- If `sold_at` is NULL: Excluded from count
- Cancelled sales: Should revert status from SOLD to ACTIVE or appropriate status

**Business Example:**
```
Period: July 2026
Vehicles Sold:
- Honda Activa sold on July 10: COUNT = 1
- Royal Enfield sold on July 20: COUNT = 1
- Honda Activa reserved on July 25: COUNT = 0 (status = PENDING_SALE)
- Honda Activa sold on June 30: COUNT = 0 (outside period)

Total Vehicles Sold: 2
```

---

## KPI: Current Stock

**Definition:** Total vehicles currently in dealership inventory (point-in-time, NOT period-based)

**Formula:**
```
Current Stock = COUNT(*)
WHERE shop_id = {shopId}
AND status IN ('ACTIVE', 'PENDING_SALE')
```

**Data Source:** `vehicles` table  
**Database Fields:** `shop_id`, `status`

**Affected by Date Filter:** NO (Always shows current state)

**Included Records:**
- Vehicles with status = 'ACTIVE' (available for sale)
- Vehicles with status = 'PENDING_SALE' (reserved but not yet sold)

**Excluded Records:**
- Vehicles with status = 'SOLD'
- Vehicles with status = 'INACTIVE'
- Vehicles with other statuses (UNDER_REPAIR, DELIVERED, etc.)

**Edge Cases:**
- If no vehicles in stock: Returns 0
- Always shows current state regardless of date filter

**Business Example:**
```
Current State (as of NOW):
- 12 ACTIVE vehicles: COUNT = 12
- 8 PENDING_SALE vehicles: COUNT = 8
- 5 SOLD vehicles: COUNT = 0 (excluded)
- 3 UNDER_REPAIR vehicles: COUNT = 0 (excluded)

Total Current Stock: 20

Note: This value does NOT change when user selects "Last Month" filter.
It always shows the current inventory state.
```

---

## KPI: Sales Value

**Definition:** Total revenue from vehicle sales in the selected time period

**Formula:**
```
Sales Value = SUM(price)
WHERE shop_id = {shopId}
AND status = 'SOLD'
AND sold_at BETWEEN {startDate} AND {endDate}
```

**Data Source:** `vehicles` table  
**Database Fields:** `shop_id`, `status`, `sold_at`, `price`

**Affected by Date Filter:** YES

**Included Records:**
- Vehicles with status = 'SOLD'
- `sold_at` within selected date range
- Uses `price` field (selling price to customer)

**Excluded Records:**
- Vehicles with other statuses
- Vehicles sold outside date range
- Cancelled sales
- Refunded sales (should be tracked separately)

**Edge Cases:**
- If no sales in period: Returns 0
- If `price` is NULL: Treated as 0
- Cancelled sales: Should not be counted (revert status)

**Business Example:**
```
Period: July 2026
Vehicles Sold:
- Honda Activa sold for ₹75,000 on July 10
- Royal Enfield sold for ₹180,000 on July 20
- Honda Activa reserved for ₹78,000 on July 25 (not sold yet)

Total Sales Value: ₹255,000
```

---

## KPI: Inventory Purchased (Purchase Value)

**Definition:** Total cost of vehicles acquired in the selected time period (cash outflow for inventory)

**Formula:**
```
Inventory Purchased = SUM(purchase_price)
WHERE shop_id = {shopId}
AND bought_date BETWEEN {startDate} AND {endDate}
```

**Data Source:** `vehicles` table  
**Database Fields:** `shop_id`, `bought_date`, `purchase_price`

**Affected by Date Filter:** YES

**Included Records:**
- All vehicles with `bought_date` in selected date range
- All vehicle statuses

**Excluded Records:**
- Vehicles bought outside date range
- Vehicles with NULL `purchase_price`

**Edge Cases:**
- If no purchases in period: Returns 0
- If `purchase_price` is NULL: Treated as 0

**Business Example:**
```
Period: July 2026
Vehicles Purchased:
- Honda Activa bought for ₹55,000 on July 5
- Royal Enfield bought for ₹140,000 on July 15
- Honda Activa bought for ₹52,000 on June 28 (outside period)

Total Inventory Purchased: ₹195,000

Note: This is a CASH FLOW metric, not a P&L metric.
It represents cash outflow for inventory acquisition.
It does NOT reduce profit.
```

---

## KPI: Gross Profit

**Definition:** Profit from vehicle sales before operating expenses (margin earned on sales)

**Formula:**
```
Gross Profit = SUM(
  price - (purchase_price + repair_cost + additional_expenses)
)
WHERE shop_id = {shopId}
AND status = 'SOLD'
AND sold_at BETWEEN {startDate} AND {endDate}
```

**Data Source:** `vehicles` table  
**Database Fields:** `shop_id`, `status`, `sold_at`, `price`, `purchase_price`, `repair_cost`, `additional_expenses`

**Affected by Date Filter:** YES

**Included Records:**
- Vehicles with status = 'SOLD'
- `sold_at` within selected date range

**Excluded Records:**
- Vehicles with other statuses
- Vehicles sold outside date range

**Cost Components Included:**
- `purchase_price`: Acquisition cost
- `repair_cost`: Repair and refurbishment costs
- `additional_expenses`: Accessories, painting, transport, etc.

**Edge Cases:**
- If no sales in period: Returns 0
- If any cost field is NULL: Treated as 0
- Negative profit possible (loss on sale)

**Business Example:**
```
Period: July 2026
Vehicle 1: Honda Activa
- Purchase Price: ₹55,000
- Repair Cost: ₹4,000
- Additional Expenses: ₹3,000
- Total Cost: ₹62,000
- Selling Price: ₹75,000
- Gross Profit: ₹13,000

Vehicle 2: Royal Enfield
- Purchase Price: ₹140,000
- Repair Cost: ₹8,000
- Additional Expenses: ₹5,000
- Total Cost: ₹153,000
- Selling Price: ₹180,000
- Gross Profit: ₹27,000

Total Gross Profit: ₹40,000
```

---

# SALES & INVENTORY KPIS

## KPI: Total Sales

**Definition:** Total revenue from vehicle sales in selected time period (same as Sales Value)

**Formula:**
```
Total Sales = SUM(price)
WHERE shop_id = {shopId}
AND status = 'SOLD'
AND sold_at BETWEEN {startDate} AND {endDate}
```

**Data Source:** `vehicles` table  
**Database Fields:** `shop_id`, `status`, `sold_at`, `price`

**Affected by Date Filter:** YES

**Note:** Identical to Sales Value in Business Overview

---

## KPI: Current Stock

**Definition:** Total vehicles currently in dealership inventory (point-in-time)

**Formula:**
```
Current Stock = COUNT(*)
WHERE shop_id = {shopId}
AND status IN ('ACTIVE', 'PENDING_SALE')
```

**Affected by Date Filter:** NO

**Note:** Identical to Current Stock in Business Overview

---

## KPI: Available Stock

**Definition:** Vehicles available for immediate sale (not reserved)

**Formula:**
```
Available Stock = COUNT(*)
WHERE shop_id = {shopId}
AND status = 'ACTIVE'
```

**Data Source:** `vehicles` table  
**Database Fields:** `shop_id`, `status`

**Affected by Date Filter:** NO (Always shows current state)

**Included Records:**
- Vehicles with status = 'ACTIVE' only

**Excluded Records:**
- Vehicles with status = 'PENDING_SALE' (reserved)
- Vehicles with status = 'SOLD'
- Vehicles with other statuses

**Edge Cases:**
- If no available vehicles: Returns 0
- Always shows current state

**Business Example:**
```
Current State:
- 12 ACTIVE vehicles: Available Stock = 12
- 8 PENDING_SALE vehicles: Excluded
- 5 SOLD vehicles: Excluded

Total Available Stock: 12
```

---

## KPI: Reserved Stock

**Definition:** Vehicles reserved by customers but not yet sold

**Formula:**
```
Reserved Stock = COUNT(*)
WHERE shop_id = {shopId}
AND status = 'PENDING_SALE'
```

**Data Source:** `vehicles` table  
**Database Fields:** `shop_id`, `status`

**Affected by Date Filter:** NO (Always shows current state)

**Included Records:**
- Vehicles with status = 'PENDING_SALE' only

**Excluded Records:**
- Vehicles with status = 'ACTIVE'
- Vehicles with status = 'SOLD'
- Vehicles with other statuses

**Edge Cases:**
- If no reserved vehicles: Returns 0
- Always shows current state

**Business Example:**
```
Current State:
- 12 ACTIVE vehicles: Excluded
- 8 PENDING_SALE vehicles: Reserved Stock = 8
- 5 SOLD vehicles: Excluded

Total Reserved Stock: 8
```

---

## KPI: Sold Stock

**Definition:** Count of vehicles sold in selected time period

**Formula:**
```
Sold Stock = COUNT(*)
WHERE shop_id = {shopId}
AND status = 'SOLD'
AND sold_at BETWEEN {startDate} AND {endDate}
```

**Data Source:** `vehicles` table  
**Database Fields:** `shop_id`, `status`, `sold_at`

**Affected by Date Filter:** YES

**Note:** Identical to Vehicles Sold in Business Overview

---

## KPI: Inventory Value

**Definition:** Total cost value of current inventory at acquisition cost (Balance Sheet asset)

**Formula:**
```
Inventory Value = SUM(purchase_price)
WHERE shop_id = {shopId}
AND status IN ('ACTIVE', 'PENDING_SALE')
```

**Data Source:** `vehicles` table  
**Database Fields:** `shop_id`, `status`, `purchase_price`

**Affected by Date Filter:** NO (Always shows current state)

**Included Records:**
- Vehicles with status = 'ACTIVE'
- Vehicles with status = 'PENDING_SALE'
- Uses `purchase_price` (acquisition cost)

**Excluded Records:**
- Vehicles with status = 'SOLD' (no longer inventory)
- Vehicles with other statuses

**Edge Cases:**
- If no inventory: Returns 0
- If `purchase_price` is NULL: Treated as 0
- Always shows current state

**Business Example:**
```
Current State:
- 12 ACTIVE vehicles: Average ₹55,000 = ₹660,000
- 8 PENDING_SALE vehicles: Average ₹52,000 = ₹416,000
- 5 SOLD vehicles: Excluded

Total Inventory Value: ₹1,076,000

Note: This is a Balance Sheet asset value.
It does NOT change when user selects "Last Month" filter.
```

---

# FINANCIAL OVERVIEW KPIS

## KPI: Operating Expenses

**Definition:** Total business operating expenses in selected time period (P&L expense)

**Formula:**
```
Operating Expenses = SUM(amount)
WHERE shop_id = {shopId}
AND is_active = true
AND expense_date BETWEEN {startDate} AND {endDate}
```

**Data Source:** `expenses` table  
**Database Fields:** `shop_id`, `is_active`, `expense_date`, `amount`

**Affected by Date Filter:** YES

**Included Records:**
- All active expenses in date range
- Categories: Rent, Salary, Electricity, Internet, Marketing, Office Supplies, etc.

**Excluded Records:**
- Vehicle purchases (these are asset transactions, not expenses)
- Inactive expenses (`is_active = false`)
- Expenses outside date range

**Edge Cases:**
- If no expenses in period: Returns 0
- If `amount` is NULL: Treated as 0

**Business Example:**
```
Period: July 2026
Operating Expenses:
- Rent: ₹25,000
- Salaries: ₹80,000
- Electricity: ₹8,000
- Internet: ₹2,000
- Marketing: ₹15,000
- Office Supplies: ₹5,000

Total Operating Expenses: ₹135,000

Note: Vehicle purchases (₹195,000) are NOT included here.
They are asset transactions, not operating expenses.
```

---

## KPI: Inventory Purchased (Financial Overview)

**Definition:** Total cost of vehicles acquired in selected time period

**Formula:**
```
Inventory Purchased = SUM(purchase_price)
WHERE shop_id = {shopId}
AND bought_date BETWEEN {startDate} AND {endDate}
```

**Affected by Date Filter:** YES

**Note:** This KPI is included in Financial Overview for cash flow analysis context, but it is NOT a P&L expense. It represents cash outflow for inventory investment.

**Important:** This should ideally be in a separate "Cash Flow" section rather than P&L to avoid confusion.

---

## KPI: Gross Profit

**Definition:** Profit from vehicle sales before operating expenses

**Formula:**
```
Gross Profit = SUM(
  price - (purchase_price + repair_cost + additional_expenses)
)
WHERE shop_id = {shopId}
AND status = 'SOLD'
AND sold_at BETWEEN {startDate} AND {endDate}
```

**Affected by Date Filter:** YES

**Note:** Identical to Gross Profit in Business Overview

---

## KPI: Net Profit

**Definition:** Final profit after deducting all operating expenses from gross profit

**Formula:**
```
Net Profit = Gross Profit - Operating Expenses
```

**Data Source:** Calculated from other KPIs

**Affected by Date Filter:** YES

**Accounting Treatment:**
- Gross Profit: Revenue - COGS
- Operating Expenses: Period costs
- Net Profit: Bottom-line profit

**Edge Cases:**
- If Operating Expenses > Gross Profit: Net Profit is negative (loss)
- If no sales and no expenses: Net Profit = 0

**Business Example:**
```
Period: July 2026
Gross Profit: ₹40,000
Operating Expenses: ₹135,000

Net Profit: ₹40,000 - ₹135,000 = -₹95,000 (Loss)

Note: Inventory Purchased (₹195,000) does NOT affect Net Profit.
It is an asset transaction, not an expense.
```

---

# ADDITIONAL RECOMMENDED KPIS

## KPI: Average Purchase Price

**Definition:** Average acquisition cost per vehicle purchased in period

**Formula:**
```
Average Purchase Price = Inventory Purchased / Vehicles Purchased
```

**Affected by Date Filter:** YES

**Business Value:** Monitor cost trends and supplier pricing

**Edge Cases:**
- If Vehicles Purchased = 0: Returns 0 or NULL

---

## KPI: Average Selling Price

**Definition:** Average revenue per vehicle sold in period

**Formula:**
```
Average Selling Price = Sales Value / Vehicles Sold
```

**Affected by Date Filter:** YES

**Business Value:** Monitor pricing strategy and market trends

**Edge Cases:**
- If Vehicles Sold = 0: Returns 0 or NULL

---

## KPI: Average Profit Per Vehicle

**Definition:** Average gross profit earned per vehicle sold

**Formula:**
```
Average Profit Per Vehicle = Gross Profit / Vehicles Sold
```

**Affected by Date Filter:** YES

**Business Value:** Identify high-margin vehicles and models

**Edge Cases:**
- If Vehicles Sold = 0: Returns 0 or NULL

---

## KPI: Profit Margin %

**Definition:** Gross profit as percentage of revenue

**Formula:**
```
Profit Margin % = (Gross Profit / Sales Value) * 100
```

**Affected by Date Filter:** YES

**Business Value:** Monitor profitability trends

**Edge Cases:**
- If Sales Value = 0: Returns 0 or NULL
- Can be negative (loss margin)

---

## KPI: Inventory Turnover

**Definition:** How many times inventory is sold and replaced in a period

**Formula:**
```
Inventory Turnover = COGS / Average Inventory Value
```

**Where:**
- COGS = Sum of purchase prices of sold vehicles in period
- Average Inventory Value = (Beginning Inventory + Ending Inventory) / 2

**Affected by Date Filter:** YES

**Business Value:** Identify slow-moving stock

**Edge Cases:**
- If Average Inventory Value = 0: Returns 0 or NULL

---

## KPI: Days in Inventory

**Definition:** Average number of days vehicles stay in stock before being sold

**Formula:**
```
Days in Inventory = 365 / Inventory Turnover
```

**Affected by Date Filter:** YES

**Business Value:** Identify aging inventory

---

## KPI: Stock Age Analysis

**Definition:** Breakdown of current inventory by age

**Formula:**
```
0-30 days: COUNT(*) WHERE bought_date >= (today - 30 days) AND status IN (ACTIVE, PENDING_SALE)
31-60 days: COUNT(*) WHERE bought_date BETWEEN (today - 60 days) AND (today - 31 days) AND status IN (ACTIVE, PENDING_SALE)
61-90 days: COUNT(*) WHERE bought_date BETWEEN (today - 90 days) AND (today - 61 days) AND status IN (ACTIVE, PENDING_SALE)
90+ days: COUNT(*) WHERE bought_date < (today - 90 days) AND status IN (ACTIVE, PENDING_SALE)
```

**Affected by Date Filter:** NO (Always shows current state)

**Business Value:** Identify dead stock and aging inventory

---

## KPI: Dead Stock Value

**Definition:** Value of unsold inventory after 90 days

**Formula:**
```
Dead Stock Value = SUM(purchase_price)
WHERE bought_date < (today - 90 days)
AND status IN (ACTIVE, PENDING_SALE)
```

**Affected by Date Filter:** NO (Always shows current state)

**Business Value:** Identify obsolete inventory requiring discount/disposal

---

# DATE FILTER BEHAVIOR

## Filter Types

### Today
- **From:** Today 00:00:00
- **To:** Current time
- **Behavior:** Shows data from start of today to now

### Yesterday
- **From:** Yesterday 00:00:00
- **To:** Yesterday 23:59:59
- **Behavior:** Shows full day yesterday

### This Week
- **From:** Monday 00:00:00
- **To:** Current time
- **Behavior:** Shows data from Monday to now

### This Month
- **From:** 1st of month 00:00:00
- **To:** Current time
- **Behavior:** Shows data from 1st to now

### Last Month
- **From:** 1st of previous month 00:00:00
- **To:** Last day of previous month 23:59:59
- **Behavior:** Shows full previous month

### This Year
- **From:** January 1 00:00:00
- **To:** Current time
- **Behavior:** Shows data from Jan 1 to now

## KPI Date Filter Compliance

### KPIs That MUST Respect Date Filter
- Vehicles Purchased ✅
- Vehicles Sold ✅
- Sales Value ✅
- Inventory Purchased ✅
- Gross Profit ✅
- Total Sales ✅
- Sold Stock ✅
- Operating Expenses ✅
- Net Profit ✅

### KPIs That MUST NOT Respect Date Filter (Point-in-Time)
- Current Stock ❌
- Available Stock ❌
- Reserved Stock ❌
- Inventory Value ❌
- Stock Age Analysis ❌
- Dead Stock Value ❌

---

# VEHICLE STATUS DEFINITIONS

## Current Statuses

### ACTIVE
- **Definition:** Vehicle is available for immediate sale
- **Included in:** Current Stock, Available Stock, Inventory Value
- **Not included in:** Sold Stock
- **Date Filter:** Point-in-time (no filter)

### PENDING_SALE
- **Definition:** Vehicle is reserved by customer but not yet sold
- **Included in:** Current Stock, Reserved Stock, Inventory Value
- **Not included in:** Available Stock, Sold Stock
- **Date Filter:** Point-in-time (no filter)

### SOLD
- **Definition:** Vehicle has been sold to customer
- **Included in:** Sold Stock, Sales calculations
- **Not included in:** Current Stock, Inventory Value
- **Date Filter:** Respects date filter (based on `sold_at`)

### INACTIVE
- **Definition:** Vehicle is not active (archived, damaged, etc.)
- **Included in:** None (excluded from all KPIs)
- **Date Filter:** N/A

## Future Statuses (To Be Defined)

### UNDER_REPAIR
- **Definition:** Vehicle is being repaired/refurbished
- **Included in:** Current Stock (if still owned)
- **Not included in:** Available Stock
- **Date Filter:** Point-in-time

### RESERVED
- **Definition:** Alternative to PENDING_SALE
- **Behavior:** Same as PENDING_SALE

### BOOKED
- **Definition:** Customer has booked but not yet paid
- **Behavior:** Similar to PENDING_SALE

### DELIVERED
- **Definition:** Vehicle has been delivered to customer
- **Behavior:** Similar to SOLD

### RETURNED
- **Definition:** Vehicle returned by customer
- **Behavior:** Should revert to ACTIVE or appropriate status

---

# EXPENSE CATEGORY RULES

## Current Categories

### Fuel
- **Type:** Operating Expense
- **Included in:** Operating Expenses
- **Example:** Petrol, diesel for shop vehicles

### Rent
- **Type:** Operating Expense
- **Included in:** Operating Expenses
- **Example:** Shop rent, warehouse rent

### Maintenance
- **Type:** Operating Expense
- **Included in:** Operating Expenses
- **Example:** Shop maintenance, equipment repair

## Future Categories (Dynamic)

### GST
- **Type:** Tax Expense
- **Included in:** Operating Expenses
- **Example:** GST paid on purchases

### Bank Charges
- **Type:** Financial Expense
- **Included in:** Operating Expenses
- **Example:** Bank fees, transaction charges

### Loan Interest
- **Type:** Financial Expense
- **Included in:** Operating Expenses
- **Example:** Interest on business loans

### Insurance
- **Type:** Operating Expense
- **Included in:** Operating Expenses
- **Example:** Shop insurance, vehicle insurance

### Legal Fees
- **Type:** Professional Expense
- **Included in:** Operating Expenses
- **Example:** Legal consultation, registration fees

### Marketing
- **Type:** Operating Expense
- **Included in:** Operating Expenses
- **Example:** Advertising, promotions

### Salaries
- **Type:** Operating Expense
- **Included in:** Operating Expenses
- **Example:** Staff salaries

### Utilities
- **Type:** Operating Expense
- **Included in:** Operating Expenses
- **Example:** Electricity, water, internet

## Important: Vehicle Purchase NOT an Expense

**Vehicle purchases are ASSET transactions, NOT expenses.**

- **Type:** Asset Transaction
- **Included in:** Inventory Purchased (cash flow)
- **NOT Included in:** Operating Expenses
- **Accounting Treatment:** Converts cash to inventory asset
- **P&L Impact:** None (until vehicle is sold)

---

# EDGE CASES & BUSINESS RULES

## Cancelled Sales

**Rule:** Cancelled sales should not be counted in Sales Value or Vehicles Sold

**Implementation:**
- Revert vehicle status from SOLD to ACTIVE
- Update `sold_at` to NULL
- This ensures cancelled sales are not included in KPIs

**Example:**
```
Vehicle sold on July 10: Status = SOLD, sold_at = July 10
Sale cancelled on July 15: Status = ACTIVE, sold_at = NULL
Result: Not counted in July Sales Value
```

## Refunded Sales

**Rule:** Refunded sales should be tracked separately, not in standard KPIs

**Implementation:**
- Create separate refund tracking mechanism
- Do not revert SOLD status
- Track refunds in separate table
- Calculate Net Sales = Sales Value - Refunds

**Example:**
```
Vehicle sold for ₹75,000 on July 10
Refunded ₹75,000 on July 20
Sales Value: ₹75,000 (still counted)
Refunds: ₹75,000 (tracked separately)
Net Sales: ₹0
```

## Exchanged Vehicles

**Rule:** Vehicle exchanges should be treated as two separate transactions

**Implementation:**
- Record sale of old vehicle
- Record purchase of new vehicle
- Calculate net effect separately

**Example:**
```
Customer exchanges old bike for new bike
1. Sell old bike: +₹30,000 Sales Value
2. Buy new bike: +₹50,000 Inventory Purchased
Net cash flow: -₹20,000
```

## NULL Value Handling

**Rule:** All NULL values should be treated as 0 in calculations

**Implementation:**
- Use COALESCE in SQL queries
- Use null checks in application code
- Never return NULL for KPIs

**Example:**
```
purchase_price = NULL → Treated as 0
repair_cost = NULL → Treated as 0
additional_expenses = NULL → Treated as 0
```

## Negative Values

**Rule:** Negative values are allowed for losses

**Implementation:**
- Gross Profit can be negative (loss on sale)
- Net Profit can be negative (operating loss)
- Display as negative numbers in UI

**Example:**
```
Purchase: ₹60,000
Selling Price: ₹55,000
Gross Profit: -₹5,000 (loss)
```

## Zero Division

**Rule:** Division by zero should return 0 or NULL

**Implementation:**
- Check denominator before division
- Return 0 if denominator is 0
- Return NULL if calculation is invalid

**Example:**
```
Average Selling Price = Sales Value / Vehicles Sold
If Vehicles Sold = 0: Return 0 or NULL
```

## Missing Months in Charts

**Rule:** Charts should show 0 for months with no data

**Implementation:**
- Generate all periods for date range
- Fill missing periods with 0
- Display complete timeline

**Example:**
```
Period: January to March 2026
January: ₹100,000
February: ₹0 (no sales, but displayed)
March: ₹150,000
```

---

# FUTURE IMPROVEMENTS

## Vehicle Expense Table (Future Enhancement)

**Current Design:** `repair_cost` and `additional_expenses` fields in Vehicle table

**Proposed Design:** Separate `vehicle_expenses` table for detailed tracking

**Advantages:**
- Track individual expense types (Registration, Insurance, Broker Commission, Transport, RTO, Accessories, Painting, Fuel)
- Track expense dates
- Track expense vendors
- Track expense receipts
- Multiple repair cycles
- Better audit trail

**Disadvantages:**
- More complex queries
- Performance overhead
- More data entry

**Recommendation:** Implement when business needs detailed expense tracking

---

# BUSINESS CALCULATION ENGINE

## Architecture

The Analytics module now includes a **Business Calculation Engine** that serves as the single source of truth for all business calculations across the entire ERP.

**Package Structure:**
```
swari.sewa.module.analytics.engine/
├── BusinessCalculationEngine.java (Interface)
└── impl/
    └── BusinessCalculationEngineImpl.java (Implementation)
```

**Architecture Layer:**
```
Analytics Controller
        ↓
Analytics Service
        ↓
Business Calculation Engine ← Single Source of Truth
        ↓
Repositories
        ↓
Database
```

## Purpose

- Ensure consistent calculations across all modules (Analytics, Reports, Accounting, GST, Ledgers, Cash Flow, Mobile, Desktop)
- Eliminate duplicate business logic
- Provide reusable KPI methods
- Centralize formula definitions

## Reusable KPI Methods

The BusinessCalculationEngine provides the following reusable methods:

**Stock KPIs (Point-in-Time):**
- `getCurrentStock(shopId)` - Current inventory count
- `getAvailableStock(shopId)` - Available for sale
- `getReservedStock(shopId)` - Reserved but not sold
- `getInventoryValue(shopId)` - Current inventory value at cost

**Period KPIs (Date Filtered):**
- `getVehiclesPurchased(shopId, startDate, endDate)` - Vehicles acquired in period
- `getVehiclesSold(shopId, startDate, endDate)` - Vehicles sold in period
- `getSalesValue(shopId, startDate, endDate)` - Total revenue from sales
- `getInventoryPurchased(shopId, startDate, endDate)` - Cash outflow for inventory
- `getOperatingExpenses(shopId, startDate, endDate)` - Operating expenses

**Profitability KPIs:**
- `getCOGS(shopId, startDate, endDate)` - Cost of Goods Sold
- `getGrossProfit(shopId, startDate, endDate)` - Sales - COGS
- `getNetProfit(shopId, startDate, endDate)` - Gross Profit - Operating Expenses

**Average KPIs:**
- `getAveragePurchasePrice(shopId, startDate, endDate)` - Average acquisition cost
- `getAverageSellingPrice(shopId, startDate, endDate)` - Average revenue per sale
- `getAverageProfitPerVehicle(shopId, startDate, endDate)` - Average profit per vehicle
- `getProfitMargin(shopId, startDate, endDate)` - Profit as percentage of revenue

**Inventory Analysis KPIs:**
- `getInventoryTurnover(shopId, startDate, endDate)` - How fast inventory sells
- `getDaysInInventory(shopId, startDate, endDate)` - Average days in stock
- `getStockAgeAnalysis(shopId)` - Inventory breakdown by age
- `getDeadStockValue(shopId)` - Value of unsold inventory after 90 days

## Usage Example

**Before (Duplicated Logic):**
```java
// In AnalyticsServiceImpl
BigDecimal salesValue = vehicleRepository.sumPriceByShopIdAndStatusAndSoldAtBetween(...);

// In Reports Module (would duplicate)
BigDecimal salesValue = vehicleRepository.sumPriceByShopIdAndStatusAndSoldAtBetween(...);
```

**After (Single Source of Truth):**
```java
// In AnalyticsServiceImpl
BigDecimal salesValue = businessCalculationEngine.getSalesValue(shopId, startDate, endDate);

// In Reports Module (reuses same logic)
BigDecimal salesValue = businessCalculationEngine.getSalesValue(shopId, startDate, endDate);

// In Accounting Module (reuses same logic)
BigDecimal salesValue = businessCalculationEngine.getSalesValue(shopId, startDate, endDate);
```

---

# COGS (COST OF GOODS SOLD)

## Definition

COGS represents the total cost of acquiring and preparing a vehicle for sale.

## Formula

```
COGS = Purchase Price + Repair Cost + Additional Expenses
```

## Components

- **Purchase Price:** Cost to acquire the vehicle
- **Repair Cost:** Repair and refurbishment costs
- **Additional Expenses:** Accessories, painting, transport, registration, insurance, etc.

## Implementation

**Repository Query:**
```java
@Query("SELECT COALESCE(SUM(COALESCE(v.purchasePrice, 0) + COALESCE(v.repairCost, 0) + COALESCE(v.additionalExpenses, 0)), 0) FROM Vehicle v WHERE v.shop.id = :shopId AND v.status = :status AND v.soldAt BETWEEN :startDate AND :endDate")
BigDecimal sumCOGSByShopIdAndStatusAndSoldAtBetween(...);
```

**BusinessCalculationEngine Method:**
```java
BigDecimal getCOGS(Long shopId, LocalDateTime startDate, LocalDateTime endDate);
```

## Usage

**Gross Profit Calculation:**
```
Gross Profit = Sales Value - COGS
```

**Net Profit Calculation:**
```
Net Profit = Gross Profit - Operating Expenses
```

## Future Enhancement

When migrating to `vehicle_expenses` table, COGS will be calculated by summing all vehicle expenses for sold vehicles.

---

# VEHICLE COST ABSTRACTION

## Purpose

Prepare the architecture for future migration to a `vehicle_expenses` table while maintaining current implementation.

## Interface

**VehicleCostCalculator Interface:**
```java
public interface VehicleCostCalculator {
    BigDecimal calculateTotalCost(Vehicle vehicle);
    BigDecimal calculateCOGS(Vehicle vehicle);
    BigDecimal getPurchasePrice(Vehicle vehicle);
    BigDecimal getRepairCost(Vehicle vehicle);
    BigDecimal getAdditionalExpenses(Vehicle vehicle);
    BigDecimal calculateGrossProfit(Vehicle vehicle);
}
```

## Current Implementation

**VehicleCostCalculatorImpl:**
- Uses Vehicle entity fields (purchase_price, repair_cost, additional_expenses)
- Handles NULL values safely
- Returns BigDecimal.ZERO for NULL values

## Future Migration

When `vehicle_expenses` table is implemented:
1. Only `VehicleCostCalculatorImpl` needs to change
2. Query from `vehicle_expenses` table instead of Vehicle entity
3. All other code using the interface remains unchanged
4. Minimal code changes required

## Benefits

- Future-proof architecture
- Minimal migration effort
- Clean separation of concerns
- Easy to test and maintain

---

# CHART DATA UTILITIES

## ChartDataUtil

Utility class for chart data processing to ensure complete timelines in charts.

## Zero-Filling Logic

Charts must always return complete timelines. If a month has no data, it should show 0 instead of being skipped.

**Methods:**
- `fillMissingPeriods()` - Fill missing periods for single value charts
- `fillMissingPeriodsTwoValues()` - Fill missing periods for dual value charts

**Example:**
```
Before: January (₹100,000), March (₹150,000)
After: January (₹100,000), February (₹0), March (₹150,000)
```

## Implementation

**Applied to:**
- Sales & Purchase Trend (Business Overview)
- Sales Trend (Sales Inventory)
- Expense Trend (Financial Overview)

---

# BUSINESS CONSTANTS

## DateFilter

Centralized constants for date filter types.

**Constants:**
- `TODAY` = "today"
- `YESTERDAY` = "yesterday"
- `THIS_WEEK` = "thisweek"
- `THIS_MONTH` = "thismonth"
- `LAST_MONTH` = "lastmonth"
- `THIS_YEAR` = "thisyear"

**Validation:**
- `isValid(String filter)` - Check if filter value is valid

**Usage:**
```java
if (DateFilter.isValid(filter)) {
    // Process filter
}
```

---

# UNIT TESTS

## Test Coverage

Unit tests have been added for BusinessCalculationEngine to ensure calculation accuracy.

**Test Cases:**
- Stock KPIs (current stock, available stock, reserved stock, inventory value)
- Period KPIs (vehicles purchased, vehicles sold, sales value)
- Profitability KPIs (gross profit, net profit, COGS)
- Average KPIs (average purchase price, average selling price, profit margin)
- Edge cases (NULL values, zero values, negative profit, no sales, no purchases)

**Test File:**
`src/test/java/swari/sewa/module/analytics/engine/BusinessCalculationEngineTest.java`

---

# DOCUMENT VERSION HISTORY

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0 | 2026-07-23 | Initial version | Analytics Team |
| 1.1 | 2026-07-23 | Added Business Calculation Engine, COGS, Vehicle Cost Abstraction, Chart Data Utilities, Business Constants, Unit Tests | Analytics Team |

---

# APPROVAL

**Reviewed By:** [Name]  
**Approved By:** [Name]  
**Approval Date:** [Date]
