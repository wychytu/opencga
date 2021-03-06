package org.opencb.opencga.core.api.variant;

import org.opencb.biodata.models.variant.metadata.Aggregation;
import org.opencb.opencga.core.api.ParamConstants;
import org.opencb.opencga.core.tools.ToolParams;

public class VariantIndexParams extends ToolParams {
    public static final String DESCRIPTION = "Variant index params";

    public VariantIndexParams() {
    }

    public VariantIndexParams(String file,
                              boolean resume, String outdir, boolean transform, boolean gvcf,
                              boolean load, boolean loadSplitData, boolean skipPostLoadCheck,
                              boolean excludeGenotype, String includeExtraFields, String merge,
                              boolean calculateStats, Aggregation aggregated, String aggregationMappingFile, boolean annotate,
                              String annotator, boolean overwriteAnnotations, boolean indexSearch) {
        this.file = file;
        this.resume = resume;
        this.outdir = outdir;
        this.transform = transform;
        this.gvcf = gvcf;
        this.load = load;
        this.loadSplitData = loadSplitData;
        this.skipPostLoadCheck = skipPostLoadCheck;
        this.excludeGenotype = excludeGenotype;
        this.includeExtraFields = includeExtraFields;
        this.merge = merge;
        this.calculateStats = calculateStats;
        this.aggregated = aggregated;
        this.aggregationMappingFile = aggregationMappingFile;
        this.annotate = annotate;
        this.annotator = annotator;
        this.overwriteAnnotations = overwriteAnnotations;
        this.indexSearch = indexSearch;
    }

    private String file;
    private boolean resume;
    private String outdir;

    private boolean transform;
    private boolean gvcf;

    private boolean load;
    private boolean loadSplitData;
    private boolean skipPostLoadCheck;
    private boolean excludeGenotype;
    private String includeExtraFields = ParamConstants.ALL;
    private String merge;

    private boolean calculateStats;
    private Aggregation aggregated = Aggregation.NONE;
    private String aggregationMappingFile;

    private boolean annotate;
    private String annotator;
    private boolean overwriteAnnotations;

    private boolean indexSearch;

    public String getFile() {
        return file;
    }

    public VariantIndexParams setFile(String file) {
        this.file = file;
        return this;
    }

    public boolean isResume() {
        return resume;
    }

    public VariantIndexParams setResume(boolean resume) {
        this.resume = resume;
        return this;
    }

    public String getOutdir() {
        return outdir;
    }

    public VariantIndexParams setOutdir(String outdir) {
        this.outdir = outdir;
        return this;
    }

    public boolean isTransform() {
        return transform;
    }

    public VariantIndexParams setTransform(boolean transform) {
        this.transform = transform;
        return this;
    }

    public boolean isGvcf() {
        return gvcf;
    }

    public VariantIndexParams setGvcf(boolean gvcf) {
        this.gvcf = gvcf;
        return this;
    }

    public boolean isLoad() {
        return load;
    }

    public VariantIndexParams setLoad(boolean load) {
        this.load = load;
        return this;
    }

    public boolean isLoadSplitData() {
        return loadSplitData;
    }

    public VariantIndexParams setLoadSplitData(boolean loadSplitData) {
        this.loadSplitData = loadSplitData;
        return this;
    }

    public boolean isSkipPostLoadCheck() {
        return skipPostLoadCheck;
    }

    public VariantIndexParams setSkipPostLoadCheck(boolean skipPostLoadCheck) {
        this.skipPostLoadCheck = skipPostLoadCheck;
        return this;
    }

    public boolean isExcludeGenotype() {
        return excludeGenotype;
    }

    public VariantIndexParams setExcludeGenotype(boolean excludeGenotype) {
        this.excludeGenotype = excludeGenotype;
        return this;
    }

    public String getIncludeExtraFields() {
        return includeExtraFields;
    }

    public VariantIndexParams setIncludeExtraFields(String includeExtraFields) {
        this.includeExtraFields = includeExtraFields;
        return this;
    }

    public String getMerge() {
        return merge;
    }

    public VariantIndexParams setMerge(String merge) {
        this.merge = merge;
        return this;
    }

    public boolean isCalculateStats() {
        return calculateStats;
    }

    public VariantIndexParams setCalculateStats(boolean calculateStats) {
        this.calculateStats = calculateStats;
        return this;
    }

    public Aggregation getAggregated() {
        return aggregated;
    }

    public VariantIndexParams setAggregated(Aggregation aggregated) {
        this.aggregated = aggregated;
        return this;
    }

    public String getAggregationMappingFile() {
        return aggregationMappingFile;
    }

    public VariantIndexParams setAggregationMappingFile(String aggregationMappingFile) {
        this.aggregationMappingFile = aggregationMappingFile;
        return this;
    }

    public boolean isAnnotate() {
        return annotate;
    }

    public VariantIndexParams setAnnotate(boolean annotate) {
        this.annotate = annotate;
        return this;
    }

    public String getAnnotator() {
        return annotator;
    }

    public VariantIndexParams setAnnotator(String annotator) {
        this.annotator = annotator;
        return this;
    }

    public boolean isOverwriteAnnotations() {
        return overwriteAnnotations;
    }

    public VariantIndexParams setOverwriteAnnotations(boolean overwriteAnnotations) {
        this.overwriteAnnotations = overwriteAnnotations;
        return this;
    }

    public boolean isIndexSearch() {
        return indexSearch;
    }

    public VariantIndexParams setIndexSearch(boolean indexSearch) {
        this.indexSearch = indexSearch;
        return this;
    }
}
