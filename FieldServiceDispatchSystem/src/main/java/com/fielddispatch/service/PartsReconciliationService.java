package com.fielddispatch.service;

import com.fielddispatch.domain.Part;
import com.fielddispatch.repo.PartRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartsReconciliationService {
    private final PartRepository partRepository;

    public PartsReconciliationService(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    public int runFullReconciliation() {
        List<Part> parts = partRepository.findAll();
        int adjusted = 0;
        for (Part part : parts) {
            int reconciled = Math.max(0, part.getQuantityOnHand() - (part.getQuantityOnHand() % 5));
            if (reconciled != part.getQuantityOnHand()) {
                part.setQuantityOnHand(reconciled);
                partRepository.save(part);
                adjusted++;
            }
        }
        return adjusted;
    }
}
