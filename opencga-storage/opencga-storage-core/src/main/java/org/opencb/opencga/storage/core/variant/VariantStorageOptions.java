package org.opencb.opencga.storage.core.variant;

import org.opencb.biodata.models.variant.metadata.Aggregation;
import org.opencb.opencga.storage.core.config.ConfigurationOption;

public enum VariantStorageOptions implements ConfigurationOption {

    STUDY("study"),
    SPECIES("species"),
    ASSEMBLY("assembly"),
    GVCF("gvcf", false),

    RESUME("resume", false), // Resume step.
    FORCE("force", false), // Force execute step.

    STDIN("stdin", false),
    STDOUT("stdout", false),

    TRANSFORM_BATCH_SIZE("transform.batch.size", 200),
    TRANSFORM_THREADS("transform.threads", 4),
    TRANSFORM_FORMAT("transform.format", "avro"),
    TRANSFORM_FAIL_ON_MALFORMED_VARIANT("transform.fail.on.malformed", false),
    TRANSFORM_COMPRESSION("transform.compression", "gzip"),
    TRANSFORM_ISOLATE("transform.isolate", false), // Do not store file in metadata

    LOAD_BATCH_SIZE("load.batch.size", 100),
    LOAD_THREADS("load.threads", 6),
    LOAD_SPLIT_DATA("load.split-data", false),
    POST_LOAD_CHECK_SKIP("postLoad.check.skip", false),

    STATS_DEFAULT_GENOTYPE("stats.default-genotype", "0/0"), // Default genotype to be used for calculating stats.
    STATS_MULTI_ALLELIC("stats.multiallelic", false),  // Include secondary alternates in the variant stats calculation
    STATS_CALCULATE("stats.calculate", false),          //Calculate stats on the postLoad step
    STATS_OVERWRITE("stats.overwrite", false),          //Overwrite stats already present
    STATS_UPDATE("stats.update", false),                //Calculate missing stats
    STATS_AGGREGATION("stats.aggregation.type", Aggregation.NONE),
    STATS_AGGREGATION_MAPPING_FILE("stats.aggregation.mapping-file"),

    ANNOTATE("annotate", false), // Do annotate after step.
    ANNOTATION_BATCH_SIZE("annotation.batch.size", 100),
    ANNOTATION_FILE_FORMAT("annotation.file.format", "json"),
    ANNOTATION_NUM_THREADS("annotation.threads", 8),
    ANNOTATION_OVERWEITE("annotation.overwrite"),

    ANNOTATOR("annotator"),
    ANNOTATOR_CLASS("annotator.class"),
    ANNOTATOR_CELLBASE_USE_CACHE("annotator.cellbase.use_cache"),
    ANNOTATOR_CELLBASE_INCLUDE("annotator.cellbase.include"),
    ANNOTATOR_CELLBASE_EXCLUDE("annotator.cellbase.exclude"),
    ANNOTATOR_CELLBASE_IMPRECISE_VARIANTS("annotator.cellbase.imprecise_variants"),

    INDEX_SEARCH("indexSearch", false), // Build secondary indexes using search engine.

    QUERY_DEFAULT_TIMEOUT("query.default_timeout", 10000), // Default timeout for DBAdaptor operations. Only used if none is provided.
    QUERY_MAX_TIMEOUT("query.max_timeout", 30000),         // Max allowed timeout for DBAdaptor operations
    QUERY_LIMIT_DEFAULT("query.limit.default", 1000),
    QUERY_LIMIT_MAX("query.limit.max", 5000),
    QUERY_SAMPLE_LIMIT_DEFAULT("query.sample.limit.default", 100),
    QUERY_SAMPLE_LIMIT_MAX("query.sample.limit.max", 1000),

    // Search intersect options
    INTERSECT_ACTIVE("search.intersect.active", true),                       // Allow intersect queries with the SearchEngine (Solr)
    INTERSECT_ALWAYS("search.intersect.always", false),                      // Force intersect queries
    INTERSECT_PARAMS_THRESHOLD("search.intersect.params.threshold", 3),      // Minimum number of QueryParams in the query to intersect

    APPROXIMATE_COUNT_SAMPLING_SIZE("approximateCountSamplingSize", 1000),
    APPROXIMATE_COUNT("approximateCount", false),


    /////////////
    // These params are stored in {@link org.opencb.opencga.storage.core.metadata.models.StudyMetadata#getAttributes}.
    // Need migration when renaming them
    /////////////

    LOADED_GENOTYPES("loadedGenotypes", null), // List of loaded genotypes.
    EXTRA_FORMAT_FIELDS("include.extra-fields", ""),  //Include other sample information (like DP, GQ, ...)
    EXTRA_FORMAT_FIELDS_TYPE("include.extra-fields-format", ""),  //Other sample information format (String, Integer, Float)
    EXCLUDE_GENOTYPES("exclude.genotypes", false),              //Do not store genotypes from samples

    RELEASE("release", 1),

    MERGE_MODE("merge.mode", VariantStorageEngine.MergeMode.ADVANCED),
    SEARCH_INDEX_LAST_TIMESTAMP("search.index.last.timestamp", 0);

    private final String key;
    private final Object value;

    VariantStorageOptions(String key) {
        this.key = key;
        this.value = null;
    }

    VariantStorageOptions(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String key() {
        return key;
    }

    @SuppressWarnings("unchecked")
    public <T> T defaultValue() {
        return (T) value;
    }

}
