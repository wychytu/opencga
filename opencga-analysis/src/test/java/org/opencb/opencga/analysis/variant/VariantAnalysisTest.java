package org.opencb.opencga.analysis.variant;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.hamcrest.CoreMatchers;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opencb.biodata.models.commons.Disorder;
import org.opencb.biodata.models.commons.Phenotype;
import org.opencb.biodata.models.pedigree.IndividualProperty;
import org.opencb.biodata.models.variant.StudyEntry;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.metadata.SampleVariantStats;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.analysis.tools.ToolRunner;
import org.opencb.opencga.analysis.variant.genes.knockout.GeneKnockoutAnalysis;
import org.opencb.opencga.analysis.variant.genes.knockout.GeneKnockoutAnalysisParams;
import org.opencb.opencga.analysis.variant.gwas.GwasAnalysis;
import org.opencb.opencga.analysis.variant.manager.VariantStorageManager;
import org.opencb.opencga.analysis.variant.stats.CohortVariantStatsAnalysis;
import org.opencb.opencga.analysis.variant.stats.SampleVariantStatsAnalysis;
import org.opencb.opencga.analysis.variant.stats.VariantStatsAnalysis;
import org.opencb.opencga.catalog.db.api.SampleDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.managers.CatalogManager;
import org.opencb.opencga.catalog.models.update.SampleUpdateParams;
import org.opencb.opencga.catalog.utils.AvroToAnnotationConverter;
import org.opencb.opencga.core.api.variant.VariantExportParams;
import org.opencb.opencga.core.common.JacksonUtils;
import org.opencb.opencga.core.models.*;
import org.opencb.opencga.core.tools.result.ExecutionResult;
import org.opencb.opencga.core.tools.result.ExecutionResultManager;
import org.opencb.opencga.storage.core.StorageEngineFactory;
import org.opencb.opencga.storage.core.config.StorageConfiguration;
import org.opencb.opencga.storage.core.metadata.models.VariantScoreMetadata;
import org.opencb.opencga.storage.core.variant.VariantStorageEngine;
import org.opencb.opencga.storage.core.variant.VariantStorageOptions;
import org.opencb.opencga.storage.core.variant.adaptors.VariantQueryParam;
import org.opencb.opencga.storage.hadoop.variant.HadoopVariantStorageEngine;
import org.opencb.opencga.storage.hadoop.variant.HadoopVariantStorageTest;
import org.opencb.opencga.storage.hadoop.variant.VariantHbaseTestUtils;
import org.opencb.opencga.storage.hadoop.variant.adaptors.VariantHadoopDBAdaptor;
import org.opencb.opencga.storage.mongodb.variant.MongoDBVariantStorageEngine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class VariantAnalysisTest {

    public static final String USER = "user";
    public static final String PASSWORD = "asdf";
    public static final String PROJECT = "project";
    public static final String STUDY = "study";
    public static final String PHENOTYPE_NAME = "myPhenotype";
    public static final Phenotype PHENOTYPE = new Phenotype(PHENOTYPE_NAME, PHENOTYPE_NAME, "mySource")
            .setStatus(Phenotype.Status.OBSERVED);
    public static final String DB_NAME = "opencga_test_" + USER + "_" + PROJECT;
    private ToolRunner toolRunner;

    @Parameterized.Parameters(name="{0}")
    public static Object[][] parameters() {
        return new Object[][]{
                {MongoDBVariantStorageEngine.STORAGE_ENGINE_ID},
                {HadoopVariantStorageEngine.STORAGE_ENGINE_ID}};
    }

    public VariantAnalysisTest(String storageEngine) {
        if (!storageEngine.equals(VariantAnalysisTest.storageEngine)) {
            indexed = false;
        }
        VariantAnalysisTest.storageEngine = storageEngine;
    }


    private CatalogManager catalogManager;
    private VariantStorageManager variantStorageManager;

    @ClassRule
    public static OpenCGATestExternalResource opencga = new OpenCGATestExternalResource();
    @ClassRule
    public static HadoopVariantStorageTest.HadoopExternalResource hadoopExternalResource = new HadoopVariantStorageTest.HadoopExternalResource();

    private static String storageEngine;
    private static boolean indexed = false;
    private static String token;
    private static File file;

    @Before
    public void setUp() throws Exception {
        catalogManager = opencga.getCatalogManager();
        variantStorageManager = new VariantStorageManager(catalogManager, opencga.getStorageEngineFactory());

        if (!indexed) {
            indexed = true;

            opencga.after();
            opencga.before();

            catalogManager = opencga.getCatalogManager();
            variantStorageManager = new VariantStorageManager(catalogManager, opencga.getStorageEngineFactory());

            opencga.clearStorageDB(DB_NAME);

            StorageConfiguration storageConfiguration = opencga.getStorageConfiguration();
            storageConfiguration.getVariant().setDefaultEngine(storageEngine);
            if (storageEngine.equals(HadoopVariantStorageEngine.STORAGE_ENGINE_ID)) {
                HadoopVariantStorageTest.updateStorageConfiguration(storageConfiguration, hadoopExternalResource.getConf());
                ObjectMap variantHadoopOptions = storageConfiguration.getVariantEngine(HadoopVariantStorageEngine.STORAGE_ENGINE_ID).getOptions();
                for (Map.Entry<String, String> entry : hadoopExternalResource.getConf()) {
                    variantHadoopOptions.put(entry.getKey(), entry.getValue());
                }
            }

            setUpCatalogManager();


            file = opencga.createFile(STUDY, "variant-test-file.vcf.gz", token);
            variantStorageManager.index(STUDY, file.getId(), opencga.createTmpOutdir("_index"), new ObjectMap(VariantStorageOptions.ANNOTATE.key(), true), token);

            for (int i = 0; i < file.getSamples().size(); i++) {
                if (i % 2 == 0) {
                    String id = file.getSamples().get(i).getId();
                    SampleUpdateParams updateParams = new SampleUpdateParams().setPhenotypes(Collections.singletonList(PHENOTYPE));
                    catalogManager.getSampleManager().update(STUDY, id, updateParams, null, token);
                }
            }

            catalogManager.getCohortManager().create(STUDY, "c1", null, null, file.getSamples().subList(0, 2), null, null, token);
            catalogManager.getCohortManager().create(STUDY, "c2", null, null, file.getSamples().subList(2, 4), null, null, token);

            Phenotype phenotype = new Phenotype("phenotype", "phenotype", "");
            Disorder disorder = new Disorder("disorder", "disorder", "", "", Collections.singletonList(phenotype), Collections.emptyMap());
            List<Individual> individuals = new ArrayList<>(4);
            List<String> sampleIds = file.getSamples().stream().map(Sample::getId).collect(Collectors.toList());

            String father = "NA19661";
            String mother = "NA19660";
            String son = "NA19685";
            String daughter = "NA19600";
            // Father
            individuals.add(catalogManager.getIndividualManager()
                    .create(STUDY, new Individual(father, father, IndividualProperty.Sex.MALE, null, null, 0, Collections.emptyList(), Collections.emptyMap()), Collections.singletonList(father), null, token).first());
            // Mother
            individuals.add(catalogManager.getIndividualManager()
                    .create(STUDY, new Individual(mother, mother, IndividualProperty.Sex.FEMALE, null, null, 0, Collections.emptyList(), Collections.emptyMap()), Collections.singletonList(mother), null, token).first());
            // Son
            individuals.add(catalogManager.getIndividualManager()
                    .create(STUDY, new Individual(son, son, IndividualProperty.Sex.MALE, null, null, 0, Collections.emptyList(), Collections.emptyMap()).setFather(individuals.get(0)).setMother(individuals.get(1)).setDisorders(Collections.singletonList(disorder)), Collections.singletonList(son), null, token).first());
            // Daughter
            individuals.add(catalogManager.getIndividualManager()
                    .create(STUDY, new Individual(daughter, daughter, IndividualProperty.Sex.FEMALE, null, null, 0, Collections.emptyList(), Collections.emptyMap()).setFather(individuals.get(0)).setMother(individuals.get(1)), Collections.singletonList(daughter), null, token).first());
            catalogManager.getFamilyManager().create(
                    STUDY,
                    new Family("f1", "f1", Collections.singletonList(phenotype), Collections.singletonList(disorder), null, null, 3, null, null),
                    individuals.stream().map(Individual::getId).collect(Collectors.toList()), new QueryOptions(),
                    token);


            if (storageEngine.equals(HadoopVariantStorageEngine.STORAGE_ENGINE_ID)) {
                VariantStorageEngine engine = opencga.getStorageEngineFactory().getVariantStorageEngine(HadoopVariantStorageEngine.STORAGE_ENGINE_ID, DB_NAME);
                VariantHbaseTestUtils.printVariants(((VariantHadoopDBAdaptor) engine.getDBAdaptor()), Paths.get(opencga.createTmpOutdir("_hbase_print_variants")).toUri());
            }
        }
        toolRunner = new ToolRunner(opencga.getOpencgaHome().toString(), catalogManager, StorageEngineFactory.get(variantStorageManager.getStorageConfiguration()));
    }

    public void setUpCatalogManager() throws IOException, CatalogException {
        catalogManager.getUserManager().create(USER, "User Name", "mail@ebi.ac.uk", PASSWORD, "", null, Account.Type.FULL, null);
        token = catalogManager.getUserManager().login("user", PASSWORD);

        String projectId = catalogManager.getProjectManager().create(PROJECT, "Project about some genomes", "", "ACME", "Homo sapiens",
                null, null, "GRCh38", new QueryOptions(), token).first().getId();
        catalogManager.getStudyManager().create(projectId, STUDY, null, "Phase 1", Study.Type.TRIO, null, "Done", null, null, null, null,
                null, null, null, null, token);

        // Create 10 samples not indexed
        for (int i = 0; i < 10; i++) {
            Sample sample = new Sample().setId("SAMPLE_" + i);
            if (i % 2 == 0) {
                sample.setPhenotypes(Collections.singletonList(PHENOTYPE));
            }
            catalogManager.getSampleManager().create(STUDY, sample, null, token);
        }

    }

    @Test
    public void testVariantStats() throws Exception {
        ObjectMap executorParams = new ObjectMap();
        Path outDir = Paths.get(opencga.createTmpOutdir("_variant_stats"));
        System.out.println("output = " + outDir.toAbsolutePath());
        List<String> samples = file.getSamples().stream().map(Sample::getId).collect(Collectors.toList());

        VariantStatsAnalysis variantStatsAnalysis = new VariantStatsAnalysis()
                .setStudy(STUDY)
                .setSamples(samples.subList(1, 3));
        variantStatsAnalysis.setUp(opencga.getOpencgaHome().toString(), catalogManager, variantStorageManager, executorParams, outDir, token);

        ExecutionResult ar = variantStatsAnalysis.start();
        checkExecutionResult(ar);

        MutableInt count = new MutableInt();
        java.io.File file = getOutputFile(outDir);
        FileUtils.lineIterator(file).forEachRemaining(line -> {
            if (!line.startsWith("#")) {
                count.increment();
            }
        });
        Assert.assertEquals(variantStorageManager.count(new Query(VariantQueryParam.STUDY.key(), STUDY), token).first().intValue(),
                count.intValue());
    }

    @Test
    public void testVariantStatsTwoCohorts() throws Exception {
        ObjectMap executorParams = new ObjectMap();
        Path outDir = Paths.get(opencga.createTmpOutdir("_variant_stats_multi_cohort"));
        System.out.println("output = " + outDir.toAbsolutePath());

        VariantStatsAnalysis variantStatsAnalysis = new VariantStatsAnalysis()
                .setStudy(STUDY)
                .setCohort(Arrays.asList("c1", "c2"));
        variantStatsAnalysis.setUp(opencga.getOpencgaHome().toString(), catalogManager, variantStorageManager, executorParams, outDir, token);

        ExecutionResult ar = variantStatsAnalysis.start();
        checkExecutionResult(ar);

        MutableInt count = new MutableInt();
        java.io.File file = getOutputFile(outDir);
        FileUtils.lineIterator(file).forEachRemaining(line->{
            if (!line.startsWith("#")) {
                count.increment();
            }
        });
        Assert.assertEquals(variantStorageManager.count(new Query(VariantQueryParam.STUDY.key(), STUDY), token).first().intValue(),
                count.intValue());
    }

    @Test
    public void testVariantStatsWithFilter() throws Exception {
        ObjectMap executorParams = new ObjectMap();
        Path outDir = Paths.get(opencga.createTmpOutdir("_variant_stats_chr22"));
        System.out.println("output = " + outDir.toAbsolutePath());
        List<String> samples = file.getSamples().stream().map(Sample::getId).collect(Collectors.toList());

        String region = "22";
        VariantStatsAnalysis variantStatsAnalysis = new VariantStatsAnalysis()
                .setStudy(STUDY)
                .setSamples(samples.subList(1, 3))
                .setRegion(region);
        variantStatsAnalysis.setUp(opencga.getOpencgaHome().toString(), catalogManager, variantStorageManager, executorParams, outDir, token);

        ExecutionResult ar = variantStatsAnalysis.start();
        checkExecutionResult(ar);

        MutableInt count = new MutableInt();
        java.io.File file = getOutputFile(outDir);
        FileUtils.lineIterator(file).forEachRemaining(line -> {
            if (!line.startsWith("#")) {
                count.increment();
            }
        });
        Query variantsQuery = new Query(VariantQueryParam.REGION.key(), region);
        System.out.println("variantsQuery = " + variantsQuery.toJson());
        Assert.assertEquals(variantStorageManager.count(new Query(variantsQuery).append(VariantQueryParam.STUDY.key(), STUDY), token).getNumMatches(),
                count.intValue());
    }

    private java.io.File getOutputFile(Path outDir) {
        return FileUtils.listFiles(outDir.toFile(), null, false)
                .stream()
                .filter(f -> !f.getName().endsWith(ExecutionResultManager.FILE_EXTENSION))
                .findFirst().orElse(null);
    }

    @Test
    public void testSampleStats() throws Exception {

        ObjectMap executorParams = new ObjectMap();
        SampleVariantStatsAnalysis analysis = new SampleVariantStatsAnalysis();
        Path outDir = Paths.get(opencga.createTmpOutdir("_sample_stats"));
        System.out.println("output = " + outDir.toAbsolutePath());
        analysis.setUp(opencga.getOpencgaHome().toString(), catalogManager, variantStorageManager, executorParams, outDir, token);
        List<String> samples = file.getSamples().stream().map(Sample::getId).collect(Collectors.toList());
        analysis.setSampleNames(samples)
                .setStudy(STUDY)
                .setIndexResults(true);
        checkExecutionResult(analysis.start(), storageEngine.equals(HadoopVariantStorageEngine.STORAGE_ENGINE_ID));

        for (String sample : samples) {
            AnnotationSet annotationSet = catalogManager.getSampleManager().get(STUDY, sample, null, token).first().getAnnotationSets().get(0);
            SampleVariantStats sampleVariantStats = AvroToAnnotationConverter.convertAnnotationToAvro(annotationSet, SampleVariantStats.class);
            System.out.println(JacksonUtils.getDefaultObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(annotationSet));
            System.out.println(JacksonUtils.getDefaultObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(sampleVariantStats));

//            Assert.assertEquals(DummySampleVariantStatsAnalysisExecutor.getSampleVariantStats(sample), sampleVariantStats);
        }
    }

    @Test
    public void testCohortStats() throws Exception {
        ObjectMap executorParams = new ObjectMap();
        CohortVariantStatsAnalysis analysis = new CohortVariantStatsAnalysis();
        Path outDir = Paths.get(opencga.createTmpOutdir("_cohort_stats"));
        System.out.println("output = " + outDir.toAbsolutePath());
        analysis.setUp(opencga.getOpencgaHome().toString(), catalogManager, variantStorageManager, executorParams, outDir, token);
        List<String> samples = file.getSamples().stream().map(Sample::getId).collect(Collectors.toList());
        analysis.setStudy(STUDY)
                .setSamplesQuery(new Query(SampleDBAdaptor.QueryParams.ID.key(), samples.subList(0, 3)));
        checkExecutionResult(analysis.start(), storageEngine.equals(HadoopVariantStorageEngine.STORAGE_ENGINE_ID));
    }

    @Test
    public void testCohortStatsIndex() throws Exception {
        ObjectMap executorParams = new ObjectMap();
        CohortVariantStatsAnalysis analysis = new CohortVariantStatsAnalysis();
        Path outDir = Paths.get(opencga.createTmpOutdir("_cohort_stats_index"));
        System.out.println("output = " + outDir.toAbsolutePath());
        analysis.setUp(opencga.getOpencgaHome().toString(), catalogManager, variantStorageManager, executorParams, outDir, token);

        analysis.setStudy(STUDY)
                .setCohortName(StudyEntry.DEFAULT_COHORT)
                .setIndexResults(true);
        checkExecutionResult(analysis.start(), storageEngine.equals(HadoopVariantStorageEngine.STORAGE_ENGINE_ID));
    }

    @Test
    public void testExport() throws Exception {
        Path outDir = Paths.get(opencga.createTmpOutdir("_export"));
        System.out.println("outDir = " + outDir);
        VariantExportParams variantExportParams = new VariantExportParams();
        variantExportParams.appendQuery(new Query(VariantQueryParam.REGION.key(), "22"));
        Assert.assertEquals("22", variantExportParams.getRegion());
        variantExportParams.setCt("lof");
        variantExportParams.setCompress(true);
        variantExportParams.setOutputFileName("chr22");

        toolRunner.execute(VariantExportTool.class, variantExportParams.toObjectMap(), outDir, token);
    }

    @Test
    public void testGwas() throws Exception {
        ObjectMap executorParams = new ObjectMap();
        GwasAnalysis analysis = new GwasAnalysis();
        Path outDir = Paths.get(opencga.createTmpOutdir("_gwas"));
        System.out.println("output = " + outDir.toAbsolutePath());
        analysis.setUp(opencga.getOpencgaHome().toString(), catalogManager, variantStorageManager, executorParams, outDir, token);
        List<String> samples = file.getSamples().stream().map(Sample::getId).collect(Collectors.toList());
        analysis.setStudy(STUDY)
                .setCaseCohortSamplesQuery(new Query(SampleDBAdaptor.QueryParams.ID.key(), samples.subList(0, 2)))
                .setControlCohortSamplesQuery(new Query(SampleDBAdaptor.QueryParams.ID.key(), samples.subList(2, 4)));
        checkExecutionResult(analysis.start(), storageEngine.equals(HadoopVariantStorageEngine.STORAGE_ENGINE_ID));
    }

    @Test
    public void testGwasByPhenotype() throws Exception {
        ObjectMap executorParams = new ObjectMap();
        GwasAnalysis analysis = new GwasAnalysis();
        Path outDir = Paths.get(opencga.createTmpOutdir("_gwas_phenotype"));
        System.out.println("output = " + outDir.toAbsolutePath());
        analysis.setUp(opencga.getOpencgaHome().toString(), catalogManager, variantStorageManager, executorParams, outDir, token);

        analysis.setStudy(STUDY)
                .setPhenotype(PHENOTYPE_NAME);
        checkExecutionResult(analysis.start(), storageEngine.equals(HadoopVariantStorageEngine.STORAGE_ENGINE_ID));
    }

    @Test
    public void testGwasIndex() throws Exception {
        // Variant scores can not be loaded in mongodb
        Assume.assumeThat(storageEngine, CoreMatchers.is(CoreMatchers.not(MongoDBVariantStorageEngine.STORAGE_ENGINE_ID)));

        ObjectMap executorParams = new ObjectMap();
        GwasAnalysis analysis = new GwasAnalysis();
        Path outDir = Paths.get(opencga.createTmpOutdir("_gwas_index"));
        System.out.println("output = " + outDir.toAbsolutePath());
        analysis.setUp(opencga.getOpencgaHome().toString(), catalogManager, variantStorageManager, executorParams, outDir, token);

        catalogManager.getCohortManager().create(STUDY, new Cohort().setId("CASE").setSamples(file.getSamples().subList(0, 2)), new QueryOptions(), token);
        catalogManager.getCohortManager().create(STUDY, new Cohort().setId("CONTROL").setSamples(file.getSamples().subList(2, 4)), new QueryOptions(), token);

        analysis.setStudy(STUDY)
                .setCaseCohort("CASE")
                .setControlCohort("CONTROL")
                .setIndex(true)
                .setIndexScoreId("GwasScore");
        checkExecutionResult(analysis.start());

        List<VariantScoreMetadata> scores = variantStorageManager.listVariantScores(STUDY, token);
        System.out.println("scores.get(0) = " + JacksonUtils.getDefaultObjectMapper().writeValueAsString(scores));
        Assert.assertEquals(1, scores.size());
        Assert.assertEquals("GwasScore", scores.get(0).getName());

        for (Variant variant : variantStorageManager.iterable(token)) {
            Assert.assertEquals("GwasScore", variant.getStudies().get(0).getScores().get(0).getId());
        }
    }

    @Test
    public void testKnockoutGenes() throws Exception {
        Path outDir = Paths.get(opencga.createTmpOutdir("_knockout_genes"));
        System.out.println("outDir = " + outDir);
        GeneKnockoutAnalysisParams params = new GeneKnockoutAnalysisParams();
        params.setSample(file.getSamples().stream().map(Sample::getId).collect(Collectors.toList()));

        ExecutionResult er = toolRunner.execute(GeneKnockoutAnalysis.class, params.toObjectMap(), outDir, token);
        checkExecutionResult(er, false);
    }

    public void checkExecutionResult(ExecutionResult er) {
        checkExecutionResult(er, true);
    }

    public void checkExecutionResult(ExecutionResult er, boolean customExecutor) {
        if (customExecutor) {
            if (storageEngine.equals("hadoop")) {
                Assert.assertEquals("hbase-mapreduce", er.getExecutor().getId());
            } else {
                Assert.assertEquals("mongodb-local", er.getExecutor().getId());
            }
        } else {
            Assert.assertEquals("opencga-local", er.getExecutor().getId());
        }
    }
}