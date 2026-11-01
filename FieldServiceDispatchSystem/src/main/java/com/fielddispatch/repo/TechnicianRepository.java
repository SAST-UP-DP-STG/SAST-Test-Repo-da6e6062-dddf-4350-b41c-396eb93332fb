package com.fielddispatch.repo;

import com.fielddispatch.domain.Technician;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {
}
