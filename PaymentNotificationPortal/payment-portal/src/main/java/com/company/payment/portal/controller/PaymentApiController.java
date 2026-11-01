package com.company.payment.portal.controller;

import com.company.payment.portal.dto.PaymentCreateRequest;
import com.company.payment.portal.model.Payment;
import com.company.payment.portal.service.PaymentService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentApiController {
    private final PaymentService paymentService;

    public PaymentApiController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@Valid @RequestBody PaymentCreateRequest request, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        Payment payment = paymentService.createPayment(request);
        Map<String, Object> response = new HashMap<>();
        response.put("id", payment.getId());
        response.put("reference", payment.getReference());
        response.put("status", payment.getStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    public ResponseEntity<?> list(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }
        List<Payment> payments = paymentService.listPayments();
        return ResponseEntity.ok(payments);
    }
}
