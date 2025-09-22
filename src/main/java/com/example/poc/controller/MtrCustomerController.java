package com.example.poc.controller;

import com.example.poc.model.MtrCustomer;
import com.example.poc.service.MtrCustomerService;
import io.micrometer.observation.annotation.Observed;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal REST controller to demo Micrometer tracing with @Observed.
 * Observation name is stable (low-cardinality) and works with the OTel bridge.
 */
@RestController
@RequestMapping("/api/v1/mtr")
public class MtrCustomerController {

    private final MtrCustomerService service;

    public MtrCustomerController(MtrCustomerService service) {
        this.service = service;
    }

    // Annotate the handler so Micrometer emits an observation/span via the OTel bridge
    @Observed(name = "mtr.customer.controller", lowCardinalityKeyValues = {"endpoint", "/customers/{id}"})
    @GetMapping("/customers/{id}")
    public ResponseEntity<MtrCustomer> getCustomer(@PathVariable String id) {
        MtrCustomer customer = service.getCustomer(id);
        return ResponseEntity.ok(customer);
    }
}
