package com.fielddispatch.repo;

import com.fielddispatch.domain.Part;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartRepository extends JpaRepository<Part, Long> {
    Optional<Part> findBySku(String sku);
}
