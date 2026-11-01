package com.fielddispatch.service;

import com.fielddispatch.domain.Customer;
import com.fielddispatch.domain.Part;
import com.fielddispatch.domain.SystemSetting;
import com.fielddispatch.domain.Technician;
import com.fielddispatch.domain.UserAccount;
import com.fielddispatch.domain.WorkOrder;
import com.fielddispatch.repo.CustomerRepository;
import com.fielddispatch.repo.PartRepository;
import com.fielddispatch.repo.SystemSettingRepository;
import com.fielddispatch.repo.TechnicianRepository;
import com.fielddispatch.repo.UserAccountRepository;
import com.fielddispatch.repo.WorkOrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SeedDataRunner implements CommandLineRunner {
    private final UserAccountRepository userAccountRepository;
    private final WorkOrderRepository workOrderRepository;
    private final TechnicianRepository technicianRepository;
    private final PartRepository partRepository;
    private final CustomerRepository customerRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedDataRunner(UserAccountRepository userAccountRepository,
                          WorkOrderRepository workOrderRepository,
                          TechnicianRepository technicianRepository,
                          PartRepository partRepository,
                          CustomerRepository customerRepository,
                          SystemSettingRepository systemSettingRepository,
                          PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.workOrderRepository = workOrderRepository;
        this.technicianRepository = technicianRepository;
        this.partRepository = partRepository;
        this.customerRepository = customerRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userAccountRepository.count() == 0) {
            userAccountRepository.save(buildUser("amaral", "Sofia Amaral", "ADMIN", "Admin#2024"));
            userAccountRepository.save(buildUser("dcosta", "Diogo Costa", "DISPATCHER", "Dispatch#2024"));
        }

        if (technicianRepository.count() == 0) {
            technicianRepository.save(buildTechnician("Rui Tavares", "HVAC", "North", true));
            technicianRepository.save(buildTechnician("Marta Lima", "Electrical", "Center", true));
            technicianRepository.save(buildTechnician("Pedro Nunes", "Plumbing", "South", false));
        }

        if (customerRepository.count() == 0) {
            customerRepository.save(buildCustomer("Northbridge Facilities", "ops@northbridge.example", "CTR-4501", "AC-880142", 48500.0, "12 Harbor Road, Porto"));
            customerRepository.save(buildCustomer("Lumen Retail Group", "facilities@lumenretail.example", "CTR-4502", "AC-880143", 73250.0, "8 Market Street, Lisbon"));
            customerRepository.save(buildCustomer("Aurora Health Campus", "estates@aurorahealth.example", "CTR-4503", "AC-880144", 121000.0, "5 Cedar Avenue, Braga"));
        }

        if (partRepository.count() == 0) {
            partRepository.save(buildPart("PRT-1001", "Compressor Relay", 42, "Bay A1", 15));
            partRepository.save(buildPart("PRT-1002", "Thermostat Module", 18, "Bay A2", 10));
            partRepository.save(buildPart("PRT-1003", "Circuit Breaker 20A", 67, "Bay B1", 20));
            partRepository.save(buildPart("PRT-1004", "Coolant Line Kit", 9, "Bay B2", 12));
        }

        if (workOrderRepository.count() == 0) {
            workOrderRepository.save(buildWorkOrder("WO-7001", "Northbridge Facilities", "Rooftop HVAC unit not cooling", "HIGH", "OPEN", null));
            workOrderRepository.save(buildWorkOrder("WO-7002", "Lumen Retail Group", "Lighting circuit intermittent on floor 2", "MEDIUM", "SCHEDULED", 2L));
            workOrderRepository.save(buildWorkOrder("WO-7003", "Aurora Health Campus", "Water leak in basement plant room", "HIGH", "OPEN", null));
            workOrderRepository.save(buildWorkOrder("WO-7004", "Northbridge Facilities", "Quarterly preventive maintenance", "LOW", "SCHEDULED", 1L));
        }

        if (systemSettingRepository.count() == 0) {
            systemSettingRepository.save(buildSetting("dispatch.autoRelease", "false"));
            systemSettingRepository.save(buildSetting("dispatch.maxDailyJobsPerTech", "6"));
            systemSettingRepository.save(buildSetting("parts.reorderEmail", "procurement@fielddispatch.example"));
            systemSettingRepository.save(buildSetting("integration.dispatchListenerPort", "9090"));
        }
    }

    private UserAccount buildUser(String username, String fullName, String role, String rawPassword) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return user;
    }

    private Technician buildTechnician(String name, String skill, String region, boolean available) {
        Technician technician = new Technician();
        technician.setName(name);
        technician.setSkill(skill);
        technician.setRegion(region);
        technician.setAvailable(available);
        return technician;
    }

    private Customer buildCustomer(String name, String email, String contractReference, String accountNumber, double contractValue, String billingAddress) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setContactEmail(email);
        customer.setContractReference(contractReference);
        customer.setAccountNumber(accountNumber);
        customer.setContractValue(contractValue);
        customer.setBillingAddress(billingAddress);
        return customer;
    }

    private Part buildPart(String sku, String name, int quantityOnHand, String location, int reorderLevel) {
        Part part = new Part();
        part.setSku(sku);
        part.setName(name);
        part.setQuantityOnHand(quantityOnHand);
        part.setLocation(location);
        part.setReorderLevel(reorderLevel);
        return part;
    }

    private WorkOrder buildWorkOrder(String reference, String customerName, String description, String priority, String status, Long technicianId) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setReference(reference);
        workOrder.setCustomerName(customerName);
        workOrder.setDescription(description);
        workOrder.setPriority(priority);
        workOrder.setStatus(status);
        workOrder.setAssignedTechnicianId(technicianId);
        return workOrder;
    }

    private SystemSetting buildSetting(String key, String value) {
        SystemSetting setting = new SystemSetting();
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        return setting;
    }
}
