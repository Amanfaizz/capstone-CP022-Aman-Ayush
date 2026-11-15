# PharmaTrack (CP-022)

Batch-level medicine inventory and expiry control system. A menu-driven console application written in Python.

**Team:** Aman Faizan (AT24CS016), Ayush (1AT24CS032)

## Problem

Pharmacies lose money to expired medicines and risk selling them by mistake. PharmaTrack tracks stock batch by batch and always sells the batch that expires first.

## How to run

Requires Python 3.8 or higher. No external libraries are needed.

```
python pharmatrack.py
```

## Menu

1. Add Medicine Stock by Batch
2. Sell Medicine
3. View Batches Expiring Within 30 Days
4. Show Low-Stock Medicines
5. View Medicine Inventory
6. Exit

## Data stored for each batch

Medicine ID, medicine name, batch number, expiry date, quantity available, reorder level.

## Core features

1. **Add stock by batch** with expiry date and quantity.
2. **Sell** a requested quantity, taking stock from the batch that expires first.
3. **Expiry alert** for batches expiring within 30 days.
4. **Low-stock report** for medicines whose total stock is below the reorder level.
5. **Expired batch block**: an expired batch can never be sold.

## What can go wrong, and how it is handled

| Feature | Case | Handling |
|---|---|---|
| Add stock | Duplicate batch number | Rejected with an error message |
| Add stock | Negative, zero or non-numeric quantity | Rejected |
| Add stock | Invalid expiry date (e.g. `2027-02-30`, `abc`) | Rejected; format must be `YYYY-MM-DD` |
| Sell | Quantity more than available stock | Rejected; shows how many units are available |
| Sell | Wrong batch selected | Batches are sorted by expiry date, earliest first |
| Sell | Invalid quantity (0, negative, text) | Rejected |
| Sell | Expired batch sold | Expired batches are never included in a sale |
| Expiry alert | Batch within 30 days not shown | Shows every batch from today up to 30 days ahead |
| Expiry alert | Expired or wrongly dated batch classified incorrectly | One function decides the status: OK, EXPIRING SOON or EXPIRED |
| Low stock | Wrong total across batches | Adds up all non-expired batches of the medicine |
| Low stock | Medicine above reorder level flagged | Flagged only if total is strictly below the reorder level |

## Design decisions

- Expired stock is not counted as available stock, in sales or in the low-stock report.
- A batch that expires today can still be sold today; it is marked EXPIRING SOON.
- Already-expired batches can be added (with a warning) so that the expired-batch block can be demonstrated.
- When a new batch is added for an existing medicine, its name and reorder level are reused, so one medicine never has two reorder levels.
- Data is kept in memory and is cleared when the program exits.
