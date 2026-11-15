package com.pharmatrack;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// @RestController = every method returns data (converted to JSON), not an HTML page.
// @RequestMapping("/api") = every URL below starts with /api.
// The controller contains NO rules: it just receives the request and calls the service.
@RestController
@RequestMapping("/api")
public class BatchController {

    // Shape of the JSON body for selling: { "medicineId": "PARA500", "quantity": 30 }
    public record SellRequest(String medicineId, int quantity) { }

    private final BatchService service;

    public BatchController(BatchService service) {
        this.service = service;
    }

    // Menu 1: POST /api/batches   (@RequestBody turns the JSON body into a Batch object)
    @PostMapping("/batches")
    public Batch addBatch(@RequestBody Batch batch) {
        return service.addBatch(batch);
    }

    // Menu 5: GET /api/batches
    @GetMapping("/batches")
    public List<Batch> inventory() {
        return service.inventory();
    }

    // Menu 3: GET /api/batches/expiring
    @GetMapping("/batches/expiring")
    public List<Batch> expiring() {
        return service.expiringSoon();
    }

    // Menu 4: GET /api/medicines/low-stock
    @GetMapping("/medicines/low-stock")
    public List<BatchService.LowStockItem> lowStock() {
        return service.lowStock();
    }

    // Delete one batch: DELETE /api/batches/7
    // {id} in the URL is handed to the method parameter by @PathVariable.
    @DeleteMapping("/batches/{id}")
    public Map<String, String> deleteBatch(@PathVariable Long id) {
        Batch deleted = service.deleteBatch(id);
        return Map.of("deleted", deleted.getBatchNumber());
    }

    // Menu 2: POST /api/sell
    @PostMapping("/sell")
    public BatchService.SaleResult sell(@RequestBody SellRequest request) {
        return service.sell(request.medicineId(), request.quantity());
    }
}
