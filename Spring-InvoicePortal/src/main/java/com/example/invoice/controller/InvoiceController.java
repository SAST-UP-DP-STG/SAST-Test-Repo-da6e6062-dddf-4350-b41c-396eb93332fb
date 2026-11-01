package com.example.invoice.controller;

import com.example.invoice.facade.InvoiceFacade;
import com.example.invoice.model.Invoice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceFacade invoiceFacade;

    public InvoiceController(InvoiceFacade invoiceFacade) {
        this.invoiceFacade = invoiceFacade;
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<?> getInvoice(
            @PathVariable Long invoiceId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Authenticated: userDetails identifies the caller.
            // VULNERABLE: invoiceId is passed directly to the facade layer and userDetails
            // is discarded — any authenticated user can read any invoice by guessing IDs.
            Invoice invoice = invoiceFacade.fetchInvoice(invoiceId);
            return ResponseEntity.ok(Map.of(
                    "id", invoice.getId(),
                    "description", invoice.getDescription(),
                    "amount", invoice.getAmount(),
                    "dueDate", invoice.getDueDate()
            ));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{invoiceId}")
    public ResponseEntity<?> deleteInvoice(
            @PathVariable Long invoiceId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // VULNERABLE: userDetails discarded — any authenticated user can delete
            // any invoice by supplying its ID in the path.
            invoiceFacade.removeInvoice(invoiceId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
