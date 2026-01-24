package com.song.my_pim.service.exportjob.process;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportJobResult {

    private int exported;
    private int skipped;
    private int failed;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    public ExportJobResult merge(ExportJobResult other) {
        if (other == null) return this;
        this.exported += other.exported;
        this.skipped += other.skipped;
        this.failed += other.failed;
        if (other.errors != null) this.errors.addAll(other.errors);
        return this;
    }

    public static ExportJobResult empty() {
        return ExportJobResult.builder().exported(0).skipped(0).failed(0).build();
    }
}
