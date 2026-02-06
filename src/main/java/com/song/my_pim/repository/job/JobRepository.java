package com.song.my_pim.repository.job;

import com.song.my_pim.entity.job.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface JobRepository extends JpaRepository<JobEntity, Long> {

    @Query("""
           select j
           from JobEntity j
           where j.enabled = true
             and j.nextRunAt <= :now
           order by j.nextRunAt asc
           """)
    List<JobEntity> findDueJobs(@Param("now") OffsetDateTime now);
}