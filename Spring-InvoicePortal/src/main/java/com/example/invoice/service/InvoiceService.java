package com.example.invoice.service;

import com.example.invoice.model.Invoice;
import com.example.invoice.repository.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public Invoice getInvoice(Long invoiceId) {
        // VULNERABLE: resolves any invoice by primary key with no ownership check.
        // The caller (InvoiceFacade → InvoiceController) only validates the JWT is
        // present — it does not verify the token subject matches invoice.ownerId.
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found: " + invoiceId));
    }

    public void deleteInvoice(Long invoiceId) {
        // VULNERABLE: deletes any invoice by primary key without checking caller ownership.
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found: " + invoiceId));
        invoiceRepository.delete(invoice);
    }
}
