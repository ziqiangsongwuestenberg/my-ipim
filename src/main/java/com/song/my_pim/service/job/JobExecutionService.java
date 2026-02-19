package com.song.my_pim.service.job;

import com.song.my_pim.common.constants.ExportConstants;
import com.song.my_pim.entity.job.JobEntity;
import com.song.my_pim.entity.job.JobHistoryEntity;
import com.song.my_pim.entity.job.JobStatus;
import com.song.my_pim.repository.job.JobHistoryRepository;
import com.song.my_pim.repository.job.JobRepository;
import com.song.my_pim.service.job.model.ArtifactInfo;
import com.song.my_pim.service.job.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class JobExecutionService {

    private final JobRepository jobRepository;
    private final JobHistoryRepository jobHistoryRepository;
    private final JobDispatcher jobDispatcher;
    private final OutboxService outboxService;

    private final ZoneId zoneId = ZoneId.systemDefault();

    @Transactional
    public void runJobTransactional(Long jobId, OffsetDateTime schedulerNow) {
        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));

        // record one job run history
        JobHistoryEntity jobHistory = new JobHistoryEntity();
        jobHistory.setJob(job);
        jobHistory.setScheduledTime(job.getNextRunAt());
        jobHistory.setStartedAt(OffsetDateTime.now(zoneId));
        jobHistory.setStatus(JobStatus.RUNNING);

        // BaseEntity audit fields
        jobHistory.setCreationUser(ExportConstants.SYSTEM);
        jobHistory.setUpdateUser(ExportConstants.SYSTEM);
        job.setUpdateUser(ExportConstants.SYSTEM);

        jobHistoryRepository.save(jobHistory);

        try {
            DispatchResult dispatchResult = jobDispatcher.dispatch(job); // JobHandler !!!

            jobHistory.setStatus(JobStatus.SUCCESS);
            jobHistory.setResultJson(dispatchResult.resultJson());

            if (dispatchResult.artifactInfo() != null) {
                ArtifactInfo artifactInfo = dispatchResult.artifactInfo();
                jobHistory.setArtifactUri(artifactInfo.uri());
                jobHistory.setSizeBytes(artifactInfo.sizeBytes());
                jobHistory.setChecksumSha256(artifactInfo.checksumSha256());
                jobHistory.setOutputFormat(artifactInfo.format());
                jobHistory.setSchemaVersion(artifactInfo.schemaVersion());
            }

            outboxService.enqueueExportCompleted(job, jobHistory, dispatchResult); // add to outboxEvent
        } catch (Exception ex) {
            jobHistory.setStatus(JobStatus.FAILED);
            jobHistory.setErrorMessage(shortMessage(ex));
        } finally {
            jobHistory.setFinishedAt(OffsetDateTime.now(zoneId));
            jobHistory.setUpdateUser(ExportConstants.SYSTEM);
            jobHistoryRepository.save(jobHistory);

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
