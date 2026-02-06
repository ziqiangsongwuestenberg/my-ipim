package com.song.my_pim.repository.job;

import com.song.my_pim.entity.job.JobHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobHistoryRepository
        extends JpaRepository<JobHistoryEntity, Long> {

    List<JobHistoryEntity> findTop20ByJob_IdOrderByCreationTimeDesc(Long jobId);
}
