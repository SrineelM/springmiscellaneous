package com.example.poc.service;

import com.example.poc.model.MtrCustomer;
import com.example.poc.repository.MtrCustomerRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Micrometer demo service. Wraps repository and exposes a single read path
 * annotated with {@code @Observed} so ObservedAspect emits an observation/span.
 */
@Service
public class MtrCustomerService {
    private final MtrCustomerRepository repository;

    public MtrCustomerService(MtrCustomerRepository repository) {
        this.repository = repository;
    }

    /** Returns a mock customer; observation adds low-cardinality tag customer.id. */
    @Observed(name = "mtr.customer.service", lowCardinalityKeyValues = {"customer.id", "{id}"})
    public MtrCustomer getCustomer(String id) {
        return repository.findById(id);
    }
}
