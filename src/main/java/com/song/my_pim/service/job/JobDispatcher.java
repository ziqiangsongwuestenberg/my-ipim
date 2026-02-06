package com.song.my_pim.service.job;

import com.song.my_pim.entity.job.JobEntity;
import com.song.my_pim.entity.job.JobType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class JobDispatcher {
    private final Map<JobType, JobHandler> handlers = new EnumMap<>(JobType.class);

    public JobDispatcher(List<JobHandler> handlerList) {
        for (JobHandler h : handlerList) {
            handlers.put(h.supports(), h);
        }
    }

    public String dispatch(JobEntity job) throws Exception {
        JobHandler handler = handlers.get(job.getJobType());
        if (handler == null) {
            throw new IllegalStateException("No handler for jobType=" + job.getJobType());
        }
        return handler.execute(job);
    }
}
