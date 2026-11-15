from datetime import date, datetime

WARNING_DAYS = 30

batches = []


def days_left(batch):
    """Days from today until expiry. Negative means already expired."""
    return (batch["expiry"] - date.today()).days


def is_expired(batch):
    """Expired = expiry date is before today (expiring today can still be sold today)."""
    return days_left(batch) < 0


def status(batch):
    """The ONE place that decides a batch's status, so it is never classified two ways."""
    days = days_left(batch)
    if days < 0:
        return "EXPIRED"
    if days <= WARNING_DAYS:
        return "EXPIRING SOON"
    return "OK"


def find_medicine(med_id):
    """Return the first batch of this medicine, or None if the medicine doesn't exist."""
    for b in batches:
        if b["medicine_id"] == med_id:
            return b
    return None


def find_batch(batch_no):
    """Return the batch with this batch number, or None."""
    for b in batches:
        if b["batch_no"] == batch_no:
            return b
    return None


def read_int(prompt):
    """Return the whole number typed, or None if it isn't one (like 'abc' or '3.5')."""
    try:
        return int(input(prompt).strip())
    except ValueError:
        return None


def error(message):
    print("Error:", message)



def add_stock():
    med_id = input("Medicine ID: ").strip().upper()
    if med_id == "":
        error("Medicine ID cannot be empty.")
        return


    existing = find_medicine(med_id)
    if existing:
        name = existing["name"]
        reorder = existing["reorder"]
        print(f"Existing medicine: {name} (reorder level {reorder}). Using these details.")
    else:
        name = input("Medicine name: ").strip()
        if name == "":
            error("Medicine name cannot be empty.")
            return
        reorder = read_int("Reorder level: ")
        if reorder is None or reorder < 0:
            error("Reorder level must be a whole number, 0 or more.")
            return

    batch_no = input("Batch number: ").strip().upper()
    if batch_no == "":
        error("Batch number cannot be empty.")
        return
    if find_batch(batch_no):                       
        error(f"Batch number {batch_no} already exists.")
        return

    try:                                         
        expiry = datetime.strptime(input("Expiry date (YYYY-MM-DD): ").strip(), "%Y-%m-%d").date()
    except ValueError:                            
        error("Invalid expiry date. Use a real date like 2027-03-31.")
        return

    qty = read_int("Quantity: ")
    if qty is None or qty <= 0:                    
        error("Quantity must be a whole number greater than 0.")
        return

    batch = {"medicine_id": med_id, "name": name, "batch_no": batch_no,
             "expiry": expiry, "qty": qty, "reorder": reorder}
    batches.append(batch)
    print(f"Added batch {batch_no} of {name} ({status(batch)}).")
    if is_expired(batch):
        print("Warning: this batch is already expired and can never be sold.")




def sell_medicine():
    med_id = input("Medicine ID: ").strip().upper()
    if find_medicine(med_id) is None:
        error(f"Medicine {med_id} not found.")
        return

    qty = read_int("Quantity to sell: ")
    if qty is None or qty <= 0:                   
        error("Quantity must be a whole number greater than 0.")
        return

   
    sellable = [b for b in batches
                if b["medicine_id"] == med_id and not is_expired(b) and b["qty"] > 0]
    available = sum(b["qty"] for b in sellable)

    if available == 0:                          
        error(f"No sellable stock for {med_id}: all its batches are expired or empty. Sale blocked.")
        return
    if qty > available:                           
        error(f"Only {available} units available (expired batches not counted), but {qty} requested.")
        return

   
    sellable.sort(key=lambda b: b["expiry"])

    remaining = qty
    for b in sellable:
        if remaining == 0:
            break
        take = min(b["qty"], remaining)           
        b["qty"] -= take
        remaining -= take
        print(f"Sold {take} from batch {b['batch_no']} (expires {b['expiry']})")
    print(f"Sale complete: {qty} units of {med_id}.")




def view_expiring_soon():
    print(f"Batches expiring within {WARNING_DAYS} days:")
    found = False
    for b in batches:
        days = days_left(b)
      
        if b["qty"] > 0 and 0 <= days <= WARNING_DAYS:
            print(f"  {b['medicine_id']} | {b['name']} | batch {b['batch_no']} | "
                  f"expires {b['expiry']} ({days} days left) | qty {b['qty']}")
            found = True
    if not found:
        print("  None.")




def show_low_stock():
  
    totals = {}                                   
    for b in batches:
        usable = 0 if is_expired(b) else b["qty"]
        totals[b["medicine_id"]] = totals.get(b["medicine_id"], 0) + usable

    print("Low-stock medicines:")
    found = False
    for med_id, total in totals.items():
        first = find_medicine(med_id)             
        if total < first["reorder"]:               
            print(f"  {med_id} | {first['name']} | usable stock {total} | reorder level {first['reorder']}")
            found = True
    if not found:
        print("  None.")




def view_inventory():
    if not batches:
        print("No stock added yet.")
        return
    print(f"{'ID':<10} {'Name':<20} {'Batch':<10} {'Expiry':<12} {'Qty':>5} {'Reorder':>8}  Status")
    for b in batches:
        print(f"{b['medicine_id']:<10} {b['name']:<20} {b['batch_no']:<10} {str(b['expiry']):<12} "
              f"{b['qty']:>5} {b['reorder']:>8}  {status(b)}")


# ---------- Menu ----------

def main():
    while True:                                   
        print("\n===== PharmaTrack =====")
        print("1. Add Medicine Stock by Batch")
        print("2. Sell Medicine")
        print("3. View Batches Expiring Within 30 Days")
        print("4. Show Low-Stock Medicines")
        print("5. View Medicine Inventory")
        print("6. Exit")
        choice = input("Choose an option: ").strip()

        if choice == "1":
            add_stock()
        elif choice == "2":
            sell_medicine()
        elif choice == "3":
            view_expiring_soon()
        elif choice == "4":
            show_low_stock()
        elif choice == "5":
            view_inventory()
        elif choice == "6":
            print("Goodbye!")
            break
        else:                                     
            print("Invalid option. Enter a number from 1 to 6.")


main()
