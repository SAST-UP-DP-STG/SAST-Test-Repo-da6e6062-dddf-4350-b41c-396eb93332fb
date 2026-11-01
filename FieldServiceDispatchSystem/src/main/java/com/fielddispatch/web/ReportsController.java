package com.fielddispatch.web;

import com.fielddispatch.domain.Customer;
import com.fielddispatch.repo.CustomerRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportsController {
    private final CustomerRepository customerRepository;
    private final Path exportDir = Paths.get("data", "exports");

    public ReportsController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping("/customers/export")
    public ResponseEntity<String> exportCustomers() throws IOException {
        List<Customer> customers = customerRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("id,name,contactEmail,contractReference,accountNumber,contractValue,billingAddress\n");
        for (Customer customer : customers) {
            csv.append(customer.getId()).append(',')
                    .append(escape(customer.getName())).append(',')
                    .append(escape(customer.getContactEmail())).append(',')
                    .append(escape(customer.getContractReference())).append(',')
                    .append(escape(customer.getAccountNumber())).append(',')
                    .append(customer.getContractValue()).append(',')
                    .append(escape(customer.getBillingAddress())).append('\n');
        }

        Files.createDirectories(exportDir);
        Path target = exportDir.resolve("customers-" + System.currentTimeMillis() + ".csv");
        Files.writeString(target, csv.toString());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"customers-export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
