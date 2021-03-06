package org.opencb.opencga.core.api.operations.variant;

import org.opencb.opencga.core.tools.ToolParams;

import java.util.List;

public class VariantSecondaryIndexParams extends ToolParams {
    public static final String DESCRIPTION = "Variant secondary index params.";

    public VariantSecondaryIndexParams() {
    }
    public VariantSecondaryIndexParams(String region, List<String> sample, boolean overwrite) {
        this.region = region;
        this.sample = sample;
        this.overwrite = overwrite;
    }
    private String region;
    private List<String> sample;
    private boolean overwrite;

    public String getRegion() {
        return region;
    }

    public VariantSecondaryIndexParams setRegion(String region) {
        this.region = region;
        return this;
    }

    public List<String> getSample() {
        return sample;
    }

    public VariantSecondaryIndexParams setSample(List<String> sample) {
        this.sample = sample;
        return this;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public VariantSecondaryIndexParams setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
        return this;
    }
}
