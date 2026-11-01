package com.fielddispatch.net;

import com.fielddispatch.domain.WorkOrder;
import com.fielddispatch.repo.WorkOrderRepository;
import com.fielddispatch.service.CustomerSyncService;
import com.fielddispatch.service.PartsReconciliationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

@Component
public class DispatchCommandServer {
    private final WorkOrderRepository workOrderRepository;
    private final PartsReconciliationService partsReconciliationService;
    private final CustomerSyncService customerSyncService;
    private final int port;
    private final String address;

    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private Thread listenerThread;

    public DispatchCommandServer(WorkOrderRepository workOrderRepository,
                                 PartsReconciliationService partsReconciliationService,
                                 CustomerSyncService customerSyncService,
                                 @Value("${app.command-server.port}") int port,
                                 @Value("${app.command-server.address}") String address) {
        this.workOrderRepository = workOrderRepository;
        this.partsReconciliationService = partsReconciliationService;
        this.customerSyncService = customerSyncService;
        this.port = port;
        this.address = address;
    }

    @PostConstruct
    public void start() throws IOException {
        serverSocket = new ServerSocket(port, 50, InetAddress.getByName(address));
        running = true;
        listenerThread = new Thread(this::acceptLoop, "dispatch-command-server");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                handleConnection(socket);
            } catch (IOException e) {
                if (running) {
                    continue;
                }
                return;
            }
        }
    }

    private void handleConnection(Socket socket) {
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            if (line == null || line.isBlank()) {
                writer.println("ERROR empty_command");
                return;
            }
            writer.println(execute(line.trim()));
        } catch (IOException e) {
            // connection closed
        }
    }

    private String execute(String command) {
        String[] parts = command.split("\\s+");
        String action = parts[0].toUpperCase();

        switch (action) {
            case "CANCEL_JOB":
                return cancelJob(parts);
            case "DISPATCH_TECH":
                return dispatchTech(parts);
            case "RECONCILE_PARTS":
                return "OK reconciled=" + partsReconciliationService.runFullReconciliation();
            case "SYNC_CUSTOMERS":
                return "OK synced=" + customerSyncService.runSync();
            default:
                return "ERROR unknown_command";
        }
    }

    private String cancelJob(String[] parts) {
        if (parts.length < 2) {
            return "ERROR usage: CANCEL_JOB <workOrderId>";
        }
        Long workOrderId = Long.valueOf(parts[1]);
        Optional<WorkOrder> found = workOrderRepository.findById(workOrderId);
        if (found.isEmpty()) {
            return "ERROR work_order_not_found";
        }
        WorkOrder workOrder = found.get();
        workOrder.setStatus("CANCELLED");
        workOrder.setUpdatedAt(Instant.now());
        workOrderRepository.save(workOrder);
        return "OK cancelled=" + workOrder.getReference();
    }

    private String dispatchTech(String[] parts) {
        if (parts.length < 3) {
            return "ERROR usage: DISPATCH_TECH <workOrderId> <technicianId>";
        }
        Long workOrderId = Long.valueOf(parts[1]);
        Long technicianId = Long.valueOf(parts[2]);
        Optional<WorkOrder> found = workOrderRepository.findById(workOrderId);
        if (found.isEmpty()) {
            return "ERROR work_order_not_found";
        }
        WorkOrder workOrder = found.get();
        workOrder.setAssignedTechnicianId(technicianId);
        workOrder.setStatus("DISPATCHED");
        workOrder.setUpdatedAt(Instant.now());
        workOrderRepository.save(workOrder);
        return "OK dispatched=" + workOrder.getReference() + " tech=" + technicianId;
    }

    @PreDestroy
    public void stop() throws IOException {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }
}
