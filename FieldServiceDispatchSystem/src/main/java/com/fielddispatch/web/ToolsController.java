package com.fielddispatch.web;

import com.fielddispatch.domain.Part;
import com.fielddispatch.repo.PartRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/tools")
public class ToolsController {
    private final PartRepository partRepository;

    public ToolsController(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    @PostMapping("/parts/recount")
    public ResponseEntity<?> recountParts(@RequestParam("file") MultipartFile file) throws IOException {
        List<Part> updated = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                if (first && line.toLowerCase().startsWith("sku")) {
                    first = false;
                    continue;
                }
                first = false;
                String[] columns = line.split(",");
                if (columns.length < 2) {
                    skipped.add(line);
                    continue;
                }
                String sku = columns[0].trim();
                int quantity;
                try {
                    quantity = Integer.parseInt(columns[1].trim());
                } catch (NumberFormatException e) {
                    skipped.add(line);
                    continue;
                }
                Optional<Part> found = partRepository.findBySku(sku);
                if (found.isEmpty()) {
                    skipped.add(sku);
                    continue;
                }
                Part part = found.get();
                part.setQuantityOnHand(quantity);
                updated.add(part);
            }
        }

        partRepository.saveAll(updated);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", updated.stream().map(Part::getSku).toList());
        result.put("skipped", skipped);
        return ResponseEntity.ok(result);
    }
}
