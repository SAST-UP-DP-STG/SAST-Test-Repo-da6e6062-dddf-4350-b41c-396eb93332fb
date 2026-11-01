package com.fielddispatch.web;

import com.fielddispatch.domain.Customer;
import com.fielddispatch.domain.Part;
import com.fielddispatch.domain.Technician;
import com.fielddispatch.domain.WorkOrder;
import com.fielddispatch.repo.CustomerRepository;
import com.fielddispatch.repo.PartRepository;
import com.fielddispatch.repo.TechnicianRepository;
import com.fielddispatch.repo.WorkOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final WorkOrderRepository workOrderRepository;
    private final TechnicianRepository technicianRepository;
    private final PartRepository partRepository;
    private final CustomerRepository customerRepository;

    public ApiController(WorkOrderRepository workOrderRepository,
                         TechnicianRepository technicianRepository,
                         PartRepository partRepository,
                         CustomerRepository customerRepository) {
        this.workOrderRepository = workOrderRepository;
        this.technicianRepository = technicianRepository;
        this.partRepository = partRepository;
        this.customerRepository = customerRepository;
    }

    @GetMapping("/dashboard/summary")
    public Map<String, Object> dashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("openWorkOrders", workOrderRepository.findAll().stream().filter(w -> "OPEN".equals(w.getStatus())).count());
        summary.put("totalWorkOrders", workOrderRepository.count());
        summary.put("availableTechnicians", technicianRepository.findAll().stream().filter(Technician::isAvailable).count());
        summary.put("partsBelowReorder", partRepository.findAll().stream().filter(p -> p.getQuantityOnHand() <= p.getReorderLevel()).count());
        summary.put("customers", customerRepository.count());
        return summary;
    }

    @GetMapping("/work-orders")
    public List<WorkOrder> workOrders() {
        return workOrderRepository.findAll();
    }

    @GetMapping("/work-orders/{id}")
    public ResponseEntity<WorkOrder> workOrder(@PathVariable Long id) {
        return workOrderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/technicians")
    public List<Technician> technicians() {
        return technicianRepository.findAll();
    }

    @GetMapping("/parts")
    public List<Part> parts() {
        return partRepository.findAll();
    }

    @GetMapping("/customers")
    public List<Map<String, Object>> customers() {
        return customerRepository.findAll().stream().map(this::toSummary).toList();
    }

    @PostMapping("/work-orders/{id}/close")
    public ResponseEntity<?> closeWorkOrder(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Optional<WorkOrder> found = workOrderRepository.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "work_order_not_found"));
        }
        String resolution = body != null && body.get("resolution") != null ? body.get("resolution").toString() : "Completed";
        WorkOrder workOrder = found.get();
        workOrder.setStatus("CLOSED");
        workOrder.setDescription(workOrder.getDescription() + " | Closed: " + resolution);
        workOrder.setUpdatedAt(Instant.now());
        workOrderRepository.save(workOrder);
        return ResponseEntity.ok(Map.of(
                "workOrderId", workOrder.getId(),
                "reference", workOrder.getReference(),
                "status", workOrder.getStatus()
        ));
    }

    @PostMapping("/work-orders/{id}/dispatch")
    public ResponseEntity<?> dispatchWorkOrder(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Optional<WorkOrder> found = workOrderRepository.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "work_order_not_found"));
        }
        Long technicianId = Long.valueOf(body.get("technicianId").toString());
        WorkOrder workOrder = found.get();
        workOrder.setAssignedTechnicianId(technicianId);
        workOrder.setStatus("DISPATCHED");
        workOrder.setUpdatedAt(Instant.now());
        workOrderRepository.save(workOrder);
        return ResponseEntity.ok(Map.of(
                "workOrderId", workOrder.getId(),
                "reference", workOrder.getReference(),
                "assignedTechnicianId", technicianId,
                "status", workOrder.getStatus()
        ));
    }

    private Map<String, Object> toSummary(Customer customer) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", customer.getId());
        view.put("name", customer.getName());
        view.put("contactEmail", customer.getContactEmail());
        view.put("contractReference", customer.getContractReference());
        return view;
    }
}
