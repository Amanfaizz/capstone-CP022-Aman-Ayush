package com.pharmatrack;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// The SERVICE holds all business rules. The controller only forwards requests here,
// so every "What can go wrong" row from the plan card is handled in this one file.
// Every rule failure throws IllegalArgumentException; ApiExceptionHandler turns it into a 400 JSON error.
@Service
public class BatchService {

    private static final int EXPIRY_WARNING_DAYS = 30;

    // Small result objects (Java "records") returned as JSON to the browser.
    public record SaleResult(String medicineId, int totalSold, List<String> details) { }
    public record LowStockItem(String medicineId, String medicineName, int totalStock, int reorderLevel) { }

    private final BatchRepository repo;

    // Constructor injection: Spring sees we need a BatchRepository and passes it in.
    public BatchService(BatchRepository repo) {
        this.repo = repo;
    }

    // ======================= 1. ADD MEDICINE STOCK BY BATCH =======================
    public Batch addBatch(Batch in) {
        // Normalise text: trim spaces; IDs and batch numbers are stored in UPPERCASE so "pb-1" and "PB-1" are the same.
        String medicineId = clean(in.getMedicineId()).toUpperCase();
        String medicineName = clean(in.getMedicineName());
        String batchNumber = clean(in.getBatchNumber()).toUpperCase();

        // Each check below is one "what can go wrong" case from the plan card.
        if (medicineId.isEmpty() || medicineName.isEmpty() || batchNumber.isEmpty()) {
            throw new IllegalArgumentException("Medicine ID, medicine name and batch number are required.");
        }
        if (in.getExpiryDate() == null) {
            throw new IllegalArgumentException("Expiry date is required (format YYYY-MM-DD).");
        }
        if (!in.getExpiryDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Expiry date must be in the future. Expired stock cannot be added.");
        }
        if (in.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }
        if (in.getReorderLevel() < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative.");
        }
        if (repo.existsByBatchNumber(batchNumber)) {
            throw new IllegalArgumentException("Batch number " + batchNumber + " already exists.");
        }

        // We build a NEW Batch instead of saving the incoming one. This way a client can never
        // sneak in an "id" and overwrite an existing row.
        Batch batch = new Batch(medicineId, medicineName, batchNumber,
                in.getExpiryDate(), in.getQuantity(), in.getReorderLevel());

        // If this medicine already exists, reuse its name and reorder level,
        // so one medicine can never have two different reorder levels.
        Optional<Batch> existing = repo.findFirstByMedicineId(medicineId);
        if (existing.isPresent()) {
            batch.setMedicineName(existing.get().getMedicineName());
            batch.setReorderLevel(existing.get().getReorderLevel());
        }
        return repo.save(batch);
    }

    // ======================= 2. SELL MEDICINE (first expiry, first out) =======================
    // @Transactional: a sale may change several batches. Either ALL changes are saved or NONE
    // (if an error happens halfway, the database rolls back).
    @Transactional
    public SaleResult sell(String rawMedicineId, int quantity) {
        String medicineId = clean(rawMedicineId).toUpperCase();
        if (medicineId.isEmpty()) {
            throw new IllegalArgumentException("Medicine ID is required.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }
        if (repo.findFirstByMedicineId(medicineId).isEmpty()) {
            throw new IllegalArgumentException("Medicine " + medicineId + " not found.");
        }

        // Only non-expired batches with stock, earliest expiry first.
        // Expired batches are never in this list, so an expired batch can NEVER be sold.
        List<Batch> sellable = repo.findSellableBatches(medicineId, LocalDate.now());

        int available = 0;
        for (Batch b : sellable) {
            available += b.getQuantity();
        }
        if (available == 0) {
            throw new IllegalArgumentException("No sellable stock for " + medicineId
                    + ": all batches are expired or empty.");
        }
        if (quantity > available) {
            throw new IllegalArgumentException("Only " + available + " units available (expired batches excluded), "
                    + "but " + quantity + " requested.");
        }

        // Take stock batch by batch until the requested quantity is covered.
        int remaining = quantity;
        List<String> details = new ArrayList<>();
        for (Batch b : sellable) {
            if (remaining == 0) {
                break;
            }
            int take = Math.min(b.getQuantity(), remaining);  // can't take more than this batch has
            b.setQuantity(b.getQuantity() - take);
            remaining -= take;
            details.add(b.getBatchNumber() + ": " + take + " units");
        }
        repo.saveAll(sellable);
        return new SaleResult(medicineId, quantity, details);
    }

    // ======================= 3. BATCHES EXPIRING WITHIN 30 DAYS =======================
    public List<Batch> expiringSoon() {
        LocalDate today = LocalDate.now();
        List<Batch> result = new ArrayList<>();
        // Between today and today+30, both included. Already-expired batches are NOT in this range.
        for (Batch b : repo.findByExpiryDateBetweenOrderByExpiryDateAsc(today, today.plusDays(EXPIRY_WARNING_DAYS))) {
            if (b.getQuantity() > 0) {   // an empty batch is nothing to worry about
                result.add(b);
            }
        }
        return result;
    }

    // ======================= 4. LOW-STOCK MEDICINES =======================
    public List<LowStockItem> lowStock() {
        Map<String, Integer> totals = new LinkedHashMap<>();   // medicineId -> total usable stock
        Map<String, Batch> firstBatch = new HashMap<>();       // medicineId -> one batch (for name + reorder level)

        for (Batch b : repo.findAll(Sort.by("medicineId"))) {
            firstBatch.putIfAbsent(b.getMedicineId(), b);
            // Expired stock counts as 0 because it cannot be sold.
            int usable = b.isExpired() ? 0 : b.getQuantity();
            // merge: if the key is new store 'usable', otherwise add it to the existing total.
            totals.merge(b.getMedicineId(), usable, Integer::sum);
        }

        List<LowStockItem> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : totals.entrySet()) {
            Batch sample = firstBatch.get(entry.getKey());
            if (entry.getValue() < sample.getReorderLevel()) {   // strictly BELOW the reorder level
                result.add(new LowStockItem(entry.getKey(), sample.getMedicineName(),
                        entry.getValue(), sample.getReorderLevel()));
            }
        }
        return result;
    }

    // ======================= 5. VIEW MEDICINE INVENTORY =======================
    public List<Batch> inventory() {
        // Sorted by medicine, then by expiry. Status (OK / EXPIRING SOON / EXPIRED) is computed inside Batch.
        return repo.findAll(Sort.by("medicineId", "expiryDate"));
    }

    // ======================= 6. DELETE A BATCH (used by the Delete button in Inventory) =======================
    @Transactional
    public Batch deleteBatch(Long id) {
        // findById returns an Optional; if no row has this id we report a clear error instead of crashing.
        Batch batch = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found. It may already be deleted."));
        repo.delete(batch);      // runs: DELETE FROM batches WHERE id = ?
        return batch;            // returned so the controller can tell the user which batch was removed
    }

    // null-safe trim helper
    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }
}
