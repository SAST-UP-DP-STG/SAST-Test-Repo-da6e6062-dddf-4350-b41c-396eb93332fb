package com.fielddispatch.service;

import com.fielddispatch.domain.Customer;
import com.fielddispatch.repo.CustomerRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class CustomerSyncService {
    private final CustomerRepository customerRepository;

    public CustomerSyncService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public int runSync() {
        List<Customer> customers = customerRepository.findAll();
        Instant now = Instant.now();
        for (Customer customer : customers) {
            customer.setLastSyncedAt(now);
            customerRepository.save(customer);
        }
        return customers.size();
    }
}
