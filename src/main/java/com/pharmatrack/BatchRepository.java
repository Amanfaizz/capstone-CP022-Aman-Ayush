package com.pharmatrack;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// We only write an INTERFACE. Spring creates the class behind it at startup.
// JpaRepository<Batch, Long> already gives us save(), findAll(), count(), saveAll() ...
// (Batch = the entity, Long = the type of its id).
public interface BatchRepository extends JpaRepository<Batch, Long> {

    // Spring READS THE METHOD NAME and writes the SQL: "exists a row where batch_number = ?".
    boolean existsByBatchNumber(String batchNumber);

    // "find first row where medicine_id = ?" -> used to check a medicine exists
    // and to reuse its name and reorder level for new batches.
    Optional<Batch> findFirstByMedicineId(String medicineId);

    // "expiry_date BETWEEN from AND to, sorted by expiry_date ascending" (both ends inclusive).
    List<Batch> findByExpiryDateBetweenOrderByExpiryDateAsc(LocalDate from, LocalDate to);

    // For this one the name would be too long, so we write the query ourselves (JPQL).
    // JPQL uses Java class/field names (Batch, b.expiryDate), not table/column names.
    // It returns only batches of ONE medicine that are NOT expired and still have stock,
    // earliest expiry first. This single query is the heart of "first expiry first out".
    @Query("SELECT b FROM Batch b "
         + "WHERE b.medicineId = :medicineId AND b.expiryDate >= :today AND b.quantity > 0 "
         + "ORDER BY b.expiryDate ASC")
    List<Batch> findSellableBatches(@Param("medicineId") String medicineId,
                                    @Param("today") LocalDate today);
}
