package com.song.my_pim.repository.job;

import com.song.my_pim.entity.job.JobHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobHistoryRepository
        extends JpaRepository<JobHistoryEntity, Long> {

    List<JobHistoryEntity> findTop20ByJob_IdOrderByCreationTimeDesc(Long jobId);

    Optional<JobHistoryEntity> findByRunUid(UUID runUid);
}
