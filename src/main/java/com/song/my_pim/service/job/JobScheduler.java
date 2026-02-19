package com.song.my_pim.service.job;

import com.song.my_pim.entity.job.JobEntity;
import com.song.my_pim.repository.job.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobScheduler {

    private final JobRepository jobRepository;
    private final JobExecutionService jobExecutionService;
    private final JdbcTemplate jdbcTemplate;
    private static final int LOCK_NS_JOB = 1001;
    private final ZoneId zoneId = ZoneId.systemDefault();

    /**
     * Every minute poll due jobs.
     */
    @Scheduled(fixedDelayString = "60000")
    public void tick() {
        OffsetDateTime now = OffsetDateTime.now(zoneId);
        List<JobEntity> due = jobRepository.findDueJobs(now);

        if (due.isEmpty()) return;

        for (JobEntity job : due) {
            runOne(job, now);
        }
    }

    private void runOne(JobEntity job, OffsetDateTime schedulerNow) {
        // advisory lock per job id (prevents duplicates across instances)
        long lockKey = job.getId();

        Boolean locked = jdbcTemplate.queryForObject(
                "select pg_try_advisory_lock(?,?)",
                Boolean.class,
                LOCK_NS_JOB,
                Math.toIntExact(lockKey)
        );

        if (locked == null || !locked) {
            // another instance is running it
            return;
        }

        try {

            jobExecutionService.runJobTransactional(job.getId(), schedulerNow);

        } catch (Exception ex) {
            log.error("Job execution failed. jobId={}", job.getId(), ex);
        } finally {
            // always unlock
            jdbcTemplate.queryForObject("select pg_advisory_unlock(?,?)",
                    Boolean.class,
                    LOCK_NS_JOB,
                    Math.toIntExact(lockKey));
        }
    }
}
