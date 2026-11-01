package com.fielddispatch.web;

import com.fielddispatch.domain.WorkOrder;
import com.fielddispatch.repo.WorkOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/integration")
public class IntegrationController {
    private final WorkOrderRepository workOrderRepository;

    public IntegrationController(WorkOrderRepository workOrderRepository) {
        this.workOrderRepository = workOrderRepository;
    }

    @PostMapping("/work-orders/close")
    public ResponseEntity<?> closeWorkOrder(@RequestBody Map<String, Object> body) {
        Long workOrderId = Long.valueOf(body.get("workOrderId").toString());
        String resolution = body.getOrDefault("resolution", "Completed").toString();

        Optional<WorkOrder> found = workOrderRepository.findById(workOrderId);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "work_order_not_found"));
        }

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

    @PostMapping("/dispatch/release")
    public ResponseEntity<?> releaseDispatch(@RequestBody Map<String, Object> body) {
        Long workOrderId = Long.valueOf(body.get("workOrderId").toString());
        Long technicianId = Long.valueOf(body.get("technicianId").toString());

        Optional<WorkOrder> found = workOrderRepository.findById(workOrderId);
        if (found.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "work_order_not_found"));
        }

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
}
