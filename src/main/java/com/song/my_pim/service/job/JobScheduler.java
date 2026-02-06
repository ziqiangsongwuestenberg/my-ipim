package com.song.my_pim.service.job;


import com.song.my_pim.common.constants.ExportConstants;
import com.song.my_pim.entity.job.JobEntity;
import com.song.my_pim.entity.job.JobHistoryEntity;
import com.song.my_pim.entity.job.JobStatus;
import com.song.my_pim.repository.job.JobRepository;
import com.song.my_pim.repository.job.JobHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
public class JobScheduler {

    private final JobRepository jobRepository;
    private final JobHistoryRepository jobHistoryRepository;
    private final JobDispatcher jobDispatcher;
    private final JdbcTemplate jdbcTemplate;

    private final ZoneId zoneId = ZoneId.systemDefault();

    public JobScheduler(JobRepository jobRepository,
                        JobHistoryRepository jobHistoryRepository,
                        JobDispatcher jobDispatcher,
                        JdbcTemplate jdbcTemplate) {
        this.jobRepository = jobRepository;
        this.jobHistoryRepository = jobHistoryRepository;
        this.jobDispatcher = jobDispatcher;
        this.jdbcTemplate = jdbcTemplate;
    }

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
                "select pg_try_advisory_lock(?)",
                Boolean.class,
                lockKey
        );

        if (locked == null || !locked) {
            // another instance is running it
            return;
        }

        try {
            doRunJobTransactional(job.getId(), schedulerNow);
        } catch (Exception ex) {
            log.error("Job execution failed. jobId={}", job.getId(), ex);
        } finally {
            // always unlock
            jdbcTemplate.queryForObject("select pg_advisory_unlock(?)", Boolean.class, lockKey);
        }
    }

    @Transactional
    protected void doRunJobTransactional(Long jobId, OffsetDateTime schedulerNow) {
        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));

        // record one run
        JobHistoryEntity run = new JobHistoryEntity();
        run.setJob(job);
        run.setScheduledTime(job.getNextRunAt());
        run.setStartedAt(OffsetDateTime.now(zoneId));
        run.setStatus(JobStatus.RUNNING);

        // BaseEntity audit fields
        run.setCreationUser(ExportConstants.SYSTEM);
        run.setUpdateUser(ExportConstants.SYSTEM);
        job.setUpdateUser(ExportConstants.SYSTEM);

        jobHistoryRepository.save(run);

        String resultJson = "{}";
        try {
            resultJson = jobDispatcher.dispatch(job);

            run.setStatus(JobStatus.SUCCESS);
            run.setResultJson(resultJson);
        } catch (Exception ex) {
            run.setStatus(JobStatus.FAILED);
            run.setErrorMessage(shortMessage(ex));
            // keep result_json as {}
        } finally {
            run.setFinishedAt(OffsetDateTime.now(zoneId));
            run.setUpdateUser(ExportConstants.SYSTEM);
            jobHistoryRepository.save(run);

            // Always advance next_run_at so job doesn't get stuck re-running every tick
            job.setNextRunAt(computeNextRun(job.getCron(), schedulerNow));
            jobRepository.save(job);
        }
    }

    private OffsetDateTime computeNextRun(String cron, OffsetDateTime from) {
        CronExpression expr = CronExpression.parse(cron);
        var next = expr.next(from.toInstant().atZone(zoneId));
        if (next == null) {
            // fallback: next minute
            return from.plusMinutes(1);
        }
        return next.toOffsetDateTime();
    }

    private String shortMessage(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null) msg = ex.getClass().getSimpleName();
        // avoid huge stack traces in DB
        if (msg.length() > 2000) msg = msg.substring(0, 2000);
        return msg;
    }
}