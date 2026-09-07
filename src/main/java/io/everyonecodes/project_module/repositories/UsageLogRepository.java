package io.everyonecodes.project_module.repositories;

import io.everyonecodes.project_module.models.UsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsageLogRepository extends JpaRepository<UsageLog, Long> {
    List<UsageLog> findByProductIdOrderByUseDateDesc(Long productId);
    int countByProductId(Long productId);
    int countByProductIdAndProjectId(Long productId, Long projectId);
}
