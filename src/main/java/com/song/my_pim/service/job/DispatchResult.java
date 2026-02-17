package com.song.my_pim.service.job;

import com.song.my_pim.service.job.model.ArtifactInfo;

public record DispatchResult(
        String resultJson,
        ArtifactInfo artifactInfo
) {
    public static DispatchResult ofResult(String resultJson) {
        return new DispatchResult(resultJson, null);
    }

    public static DispatchResult of(String resultJson, ArtifactInfo artifactInfo) {
        return new DispatchResult(resultJson, artifactInfo);
    }
}