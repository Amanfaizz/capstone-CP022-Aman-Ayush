package com.pharmatrack;

import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

// @SpringBootApplication = "start Spring here": it scans this package for our classes
// (controller, service, repository) and auto-configures Tomcat, JPA and the database.
@SpringBootApplication
public class PharmaTrackApplication {

    public static void main(String[] args) {
        // Boots Spring, starts the web server on port 8080.
        SpringApplication.run(PharmaTrackApplication.class, args);
    }

    // A @Bean method that returns a CommandLineRunner runs once, right after startup.
    // We use it to insert demo data so every feature can be shown immediately.
    // Spring passes in the BatchRepository automatically (dependency injection).
    @Bean
    CommandLineRunner seedDemoData(BatchRepository repo) {
        return args -> {
            // Only seed an empty database, so restarts do not duplicate the data.
            if (repo.count() > 0) {
                return;
            }
            LocalDate today = LocalDate.now();

            // Paracetamol: one EXPIRED batch, one EXPIRING SOON batch, one healthy batch.
            repo.save(new Batch("PARA500", "Paracetamol 500mg", "PB-001", today.minusDays(10), 40, 50));
            repo.save(new Batch("PARA500", "Paracetamol 500mg", "PB-002", today.plusDays(20), 30, 50));
            repo.save(new Batch("PARA500", "Paracetamol 500mg", "PB-003", today.plusDays(200), 100, 50));

            // Amoxicillin: only 15 units but reorder level is 30 -> shows up in LOW STOCK.
            repo.save(new Batch("AMOX250", "Amoxicillin 250mg", "AM-101", today.plusDays(25), 15, 30));

            // Ibuprofen: healthy stock, nothing to report.
            repo.save(new Batch("IBU400", "Ibuprofen 400mg", "IB-201", today.plusDays(300), 200, 50));
        };
    }
}
