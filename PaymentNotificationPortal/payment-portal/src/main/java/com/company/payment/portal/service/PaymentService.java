package com.company.payment.portal.service;

import com.company.payment.portal.dto.PaymentCreateRequest;
import com.company.payment.portal.model.Payment;
import com.company.payment.portal.repository.PaymentRepository;
import java.io.OutputStream;
import java.math.RoundingMode;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;
    private final JavaMailSender javaMailSender;
    private final String processingUrl;
    private final String auditHost;
    private final int auditPort;

    public PaymentService(PaymentRepository paymentRepository,
                          RestTemplate restTemplate,
                          JavaMailSender javaMailSender,
                          @Value("${processing.url:http://localhost:9090/process}") String processingUrl,
                          @Value("${audit.host:localhost}") String auditHost,
                          @Value("${audit.port:9100}") int auditPort) {
        this.paymentRepository = paymentRepository;
        this.restTemplate = restTemplate;
        this.javaMailSender = javaMailSender;
        this.processingUrl = processingUrl;
        this.auditHost = auditHost;
        this.auditPort = auditPort;
    }

    public Payment createPayment(PaymentCreateRequest request) {
        Payment payment = new Payment();
        payment.setFullName(request.getFullName());
        payment.setEmail(request.getEmail());
        payment.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        payment.setReference(request.getReference());
        payment.setStatus("REGISTERED");
        payment.setCreatedAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);

        Map<String, Object> payload = new HashMap<>();
        payload.put("reference", saved.getReference());
        payload.put("fullName", saved.getFullName());
        payload.put("email", saved.getEmail());
        payload.put("amount", saved.getAmount());
        payload.put("createdAt", saved.getCreatedAt().toString());
        restTemplate.postForObject(processingUrl, payload, Map.class);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@company.internal");
        message.setTo(saved.getEmail());
        message.setSubject("Payment Confirmation " + saved.getReference());
        message.setText("Your payment has been registered with amount " + saved.getAmount());
        javaMailSender.send(message);

        String auditPayload =
                "reference=" + saved.getReference() +
                ",fullName=" + saved.getFullName() +
                ",email=" + saved.getEmail() +
                ",amount=" + saved.getAmount() +
                ",createdAt=" + saved.getCreatedAt();
        try (Socket socket = new Socket(auditHost, auditPort);
             OutputStream outputStream = socket.getOutputStream()) {
            outputStream.write(auditPayload.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (Exception e) {
            throw new IllegalStateException("Audit transmission failed", e);
        }

        return saved;
    }

    public List<Payment> listPayments() {
        return paymentRepository.findAll();
    }
}
