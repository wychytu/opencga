package org.opencb.opencga.app.cli.admin.executors;

import org.apache.commons.lang3.StringUtils;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.app.cli.admin.executors.migration.AnnotationSetMigration;
import org.opencb.opencga.app.cli.admin.executors.migration.NewVariantMetadataMigration;
import org.opencb.opencga.app.cli.admin.executors.migration.storage.NewProjectMetadataMigration;
import org.opencb.opencga.app.cli.admin.executors.migration.storage.NewStudyMetadata;
import org.opencb.opencga.app.cli.admin.options.MigrationCommandOptions;
import org.opencb.opencga.catalog.db.api.DBIterator;
import org.opencb.opencga.catalog.db.api.FileDBAdaptor;
import org.opencb.opencga.catalog.db.api.StudyDBAdaptor;
import org.opencb.opencga.catalog.db.mongodb.MongoDBAdaptorFactory;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.managers.CatalogManager;
import org.opencb.opencga.catalog.managers.StudyManager;
import org.opencb.opencga.catalog.utils.UUIDUtils;
import org.opencb.opencga.core.common.TimeUtils;
import org.opencb.opencga.core.models.FileIndex;
import org.opencb.opencga.core.models.Job;
import org.opencb.opencga.core.models.Study;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created on 08/09/17.
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public class MigrationCommandExecutor extends AdminCommandExecutor {

    private final MigrationCommandOptions migrationCommandOptions;

    public MigrationCommandExecutor(MigrationCommandOptions migrationCommandOptions) {
        super(migrationCommandOptions.getCommonOptions());
        this.migrationCommandOptions = migrationCommandOptions;
    }

    @Override
    public void execute() throws Exception {
        logger.debug("Executing migration command line");

        String subCommandString = migrationCommandOptions.getSubCommand();
        switch (subCommandString) {
//            case "latest":
            case "v1.3.0":
                v1_3_0();
                break;
            case "v1.4.0":
                v1_4_0();
                break;
            case "v2.0.0":
                v2_0_0();
                break;
            default:
                logger.error("Subcommand '{}' not valid", subCommandString);
                break;
        }
    }

    private void v1_3_0() throws Exception {
        logger.info("MIGRATING v1.3.0");
        MigrationCommandOptions.MigrateV1_3_0CommandOptions options = migrationCommandOptions.getMigrateV130CommandOptions();

        if (options.files != null && !options.files.isEmpty()) {
            // Just migrate files. Do not even connect to Catalog!
            NewVariantMetadataMigration migration = new NewVariantMetadataMigration(storageConfiguration, null, options);
            for (String file : options.files) {
                migration.migrateVariantFileMetadataFile(Paths.get(file));
            }
        } else {
            setCatalogDatabaseCredentials(options, options.commonOptions);

            try (CatalogManager catalogManager = new CatalogManager(configuration)) {
                String sessionId = catalogManager.getUserManager().login("admin", options.commonOptions.adminPassword);

                // Catalog
                String basePath = appHome + "/scripts/migration/v1.3.0/";

                String authentication = "";
                if (StringUtils.isNotEmpty(configuration.getCatalog().getDatabase().getUser())
                        && StringUtils.isNotEmpty(configuration.getCatalog().getDatabase().getPassword())) {
                    authentication = "-u " + configuration.getCatalog().getDatabase().getUser() + " -p "
                            + configuration.getCatalog().getDatabase().getPassword() + " --authenticationDatabase "
                            + configuration.getCatalog().getDatabase().getOptions().getOrDefault("authenticationDatabase", "admin") + " ";
                }

                String catalogCli = "mongo " + authentication + configuration.getCatalog().getDatabase().getHosts().get(0) + "/"
                        + catalogManager.getCatalogDatabase() + " opencga_catalog_v1.2.x_to_1.3.0.js";

                logger.info("Migrating Catalog. Running {} from {}", catalogCli, basePath);
                ProcessBuilder processBuilder = new ProcessBuilder(catalogCli.split(" "));
                processBuilder.directory(new File(basePath));
                Process p = processBuilder.start();

                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = input.readLine()) != null) {
                    logger.info(line);
                }
                input.close();

                p.waitFor();
                if (p.exitValue() != 0) {
                    throw new IllegalStateException("Error migrating catalog database!");
                }

                // Storage

                new NewVariantMetadataMigration(storageConfiguration, catalogManager, options).migrate(sessionId);
            }
        }
    }

    private void v1_4_0() throws Exception {
        MigrationCommandOptions.MigrateV1_4_0CommandOptions options = migrationCommandOptions.getMigrateV140CommandOptions();

        boolean skipAnnotations = false;
        boolean skipCatalogJS = false;
        boolean skipStorage = false;
        switch (options.what) {
            case CATALOG:
                skipStorage = true;
                break;
            case STORAGE:
                skipAnnotations = true;
                skipCatalogJS = true;
                break;
            case ANNOTATIONS:
                skipCatalogJS = true;
                skipStorage = true;
                break;
            case CATALOG_NO_ANNOTATIONS:
                skipAnnotations = true;
                skipStorage = true;
                break;
            case ALL:
            default:
                break;
        }

        setCatalogDatabaseCredentials(options, options.commonOptions);

        try (CatalogManager catalogManager = new CatalogManager(configuration)) {
            // We get a non-expiring token
            String token = catalogManager.getUserManager().login("admin", options.commonOptions.adminPassword);
            String nonExpiringToken = catalogManager.getUserManager().getNonExpiringToken("admin", token);

            // Catalog
            if (!skipCatalogJS) {
                logger.info("Starting Catalog migration for 1.4.0");
                runMigration(catalogManager, appHome + "/scripts/migration/v1.4.0/", "opencga_catalog_v1.3.x_to_1.4.0.js");
            }

            if (!skipAnnotations) {
                logger.info("Starting annotation migration for 1.4.0");

                // Migrate annotationSets
                new AnnotationSetMigration(catalogManager).migrate();

                logger.info("Finished annotation migration");
            }

            if (!skipStorage) {
                new NewProjectMetadataMigration(storageConfiguration, catalogManager, options).migrate(nonExpiringToken);
                new NewStudyMetadata(storageConfiguration, catalogManager).migrate(nonExpiringToken);
            }

        }
    }


    private void v2_0_0() throws Exception {
        MigrationCommandOptions.MigrateV2_0_0CommandOptions options = migrationCommandOptions.getMigrateV200CommandOptions();

        setCatalogDatabaseCredentials(options, options.commonOptions);

        try (CatalogManager catalogManager = new CatalogManager(configuration)) {
            // 1. Catalog Javascript migration
            logger.info("Starting Catalog migration for 2.0.0");
            runMigration(catalogManager, appHome + "/scripts/migration/v2.0.0/", "opencga_catalog_v1.4.2_to_v.2.0.0.js");

            // Create default JOBS folder for analysis
            MongoDBAdaptorFactory dbAdaptorFactory = new MongoDBAdaptorFactory(configuration);
            QueryOptions queryOptions = new QueryOptions(QueryOptions.INCLUDE, Arrays.asList(StudyDBAdaptor.QueryParams.UID.key(),
                    StudyDBAdaptor.QueryParams.FQN.key(), StudyDBAdaptor.QueryParams.URI.key(), StudyDBAdaptor.QueryParams.RELEASE.key()));
            DBIterator<Study> iterator = dbAdaptorFactory.getCatalogStudyDBAdaptor().iterator(new Query(), queryOptions);

            Query fileQuery = new Query(FileDBAdaptor.QueryParams.PATH.key(), "JOBS/");
            while (iterator.hasNext()) {
                Study study = iterator.next();
                fileQuery.append(FileDBAdaptor.QueryParams.STUDY_UID.key(), study.getUid());
                if (dbAdaptorFactory.getCatalogFileDBAdaptor().count(fileQuery).getNumMatches() == 0) {
                    logger.info("Creating JOBS/ folder for study {}", study.getFqn());

                    // JOBS folder does not exist
                    org.opencb.opencga.core.models.File file = new org.opencb.opencga.core.models.File("JOBS",
                            org.opencb.opencga.core.models.File.Type.DIRECTORY, org.opencb.opencga.core.models.File.Format.UNKNOWN,
                            org.opencb.opencga.core.models.File.Bioformat.UNKNOWN,
                            Paths.get(options.jobFolder).normalize().toAbsolutePath().resolve("JOBS").toUri(),
                            "JOBS/", null, TimeUtils.getTime(), TimeUtils.getTime(), "Default jobs folder",
                            new org.opencb.opencga.core.models.File.FileStatus(), false, 0, null, null, Collections.emptyList(), new Job(),
                            Collections.emptyList(), new FileIndex(), study.getRelease(), Collections.emptyList(), Collections.emptyMap(),
                            Collections.emptyMap());
                    file.setUuid(UUIDUtils.generateOpenCGAUUID(UUIDUtils.Entity.FILE));
                    file.setTags(Collections.emptyList());
                    file.setId(file.getPath().replace("/", ":"));

                    dbAdaptorFactory.getCatalogFileDBAdaptor().insert(study.getUid(), file, null, QueryOptions.empty());

                    // Create physical folder
                    catalogManager.getCatalogIOManagerFactory().get(file.getUri()).createDirectory(file.getUri(), true);
                } else {
                    logger.info("JOBS/ folder already present for study {}", study.getFqn());
                }
            }
        }
    }

    private void runMigration(CatalogManager catalogManager, String scriptFolder, String scriptFileName)
            throws IOException, InterruptedException, CatalogException {
        String authentication = "";
        if (StringUtils.isNotEmpty(configuration.getCatalog().getDatabase().getUser())
                && StringUtils.isNotEmpty(configuration.getCatalog().getDatabase().getPassword())) {
            authentication = "-u " + configuration.getCatalog().getDatabase().getUser() + " -p "
                    + configuration.getCatalog().getDatabase().getPassword() + " --authenticationDatabase "
                    + configuration.getCatalog().getDatabase().getOptions().getOrDefault("authenticationDatabase", "admin") + " ";
        }

        String catalogCli = "mongo " + authentication
                + StringUtils.join(configuration.getCatalog().getDatabase().getHosts(), ",") + "/"
                + catalogManager.getCatalogDatabase() + " " + scriptFileName;

        logger.info("Migrating Catalog. Running {} from {}", catalogCli, scriptFolder);
        ProcessBuilder processBuilder = new ProcessBuilder(catalogCli.split(" "));
        processBuilder.directory(new File(scriptFolder));
        Process p = processBuilder.start();

        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = input.readLine()) != null) {
            logger.info(line);
        }
        p.waitFor();
        input.close();

        if (p.exitValue() == 0) {
            logger.info("Finished Catalog migration");
        } else {
            throw new CatalogException("Error migrating catalog database!");
        }
    }

}
