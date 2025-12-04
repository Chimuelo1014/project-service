package com.sentinel.project_service.repository;

import com.sentinel.project_service.entity.TenantLimitsCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TenantLimitsCacheRepository extends JpaRepository<TenantLimitsCacheEntity, UUID> {
}
