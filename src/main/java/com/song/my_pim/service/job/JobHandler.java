package com.song.my_pim.service.job;

import com.song.my_pim.entity.job.JobEntity;
import com.song.my_pim.entity.job.JobType;

public interface JobHandler {

    JobType supports();

    /**
     * Execute one job definition.
     * Return a JSON string to store in job_run.result_json
     */
    DispatchResult execute(JobEntity job) throws Exception;
}
