package com.fielddispatch.repo;

import com.fielddispatch.domain.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
}
