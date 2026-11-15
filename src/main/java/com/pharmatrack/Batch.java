package com.pharmatrack;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// @Entity = "this class maps to a database table". One Batch object = one row.
// It holds exactly the 6 fields from the Project Plan Card (plus a technical id).
@Entity
@Table(name = "batches")
public class Batch {

    // Primary key. IDENTITY = the database auto-increments it (1, 2, 3 ...).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String medicineId;       // e.g. "PARA500" (same for every batch of one medicine)

    @Column(nullable = false)
    private String medicineName;     // e.g. "Paracetamol 500mg"

    // unique = true: the DATABASE refuses two rows with the same batch number.
    // This is a safety net behind the check we also do in BatchService.
    @Column(nullable = false, unique = true)
    private String batchNumber;      // e.g. "PB-002"

    // LocalDate = a date without time. Java does the date maths for us (no string comparing).
    @Column(nullable = false)
    private LocalDate expiryDate;

    private int quantity;            // units currently in stock for this batch
    private int reorderLevel;        // alert when total stock of the medicine drops below this

    // Hibernate needs an empty constructor to build objects when reading rows.
    // Jackson (JSON library) also uses it when turning a JSON request into a Batch.
    public Batch() {
    }

    // Convenience constructor (without id, the database creates the id).
    public Batch(String medicineId, String medicineName, String batchNumber,
                 LocalDate expiryDate, int quantity, int reorderLevel) {
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
        this.reorderLevel = reorderLevel;
    }

    // ---------- Computed values (NOT stored in the database, calculated every time) ----------
    // Because the field is annotated with @Id above, Hibernate looks at FIELDS only,
    // so these getters are ignored by the database but Jackson still sends them to the browser.

    // Days from today until expiry. Negative = already expired.
    public long getDaysLeft() {
        return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    // Expired means the expiry date is before today. A batch expiring today is still usable today.
    public boolean isExpired() {
        return getDaysLeft() < 0;
    }

    // The label shown in the Inventory table.
    public String getStatus() {
        long days = getDaysLeft();
        if (days < 0) {
            return "EXPIRED";
        }
        if (days <= 30) {
            return "EXPIRING SOON";
        }
        return "OK";
    }

    // ---------- Plain getters and setters (needed by Hibernate and Jackson) ----------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMedicineId() { return medicineId; }
    public void setMedicineId(String medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
}
