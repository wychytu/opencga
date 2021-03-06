/*
 * Copyright 2015-2017 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.app.cli.main.executors.catalog;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.app.cli.main.executors.OpencgaCommandExecutor;
import org.opencb.opencga.app.cli.main.options.VariableCommandOptions;
import org.opencb.opencga.catalog.db.api.SampleDBAdaptor;
import org.opencb.opencga.catalog.db.api.StudyDBAdaptor.VariableSetParams;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.core.models.VariableSet;
import org.opencb.opencga.core.response.RestResponse;

import java.io.File;
import java.io.IOException;

/**
 * Created by by sgallego on 6/15/16.
 */
public class VariableCommandExecutor extends OpencgaCommandExecutor {

    private VariableCommandOptions variableCommandOptions;

    public VariableCommandExecutor(VariableCommandOptions variableCommandOptions) {
        super(variableCommandOptions.commonCommandOptions);
        this.variableCommandOptions = variableCommandOptions;
    }


    @Override
    public void execute() throws Exception {
        logger.debug("Executing variables command line");

        String subCommandString = getParsedSubCommand(variableCommandOptions.jCommander);
        RestResponse queryResponse = null;
        switch (subCommandString) {
            case "create":
                queryResponse = create();
                break;
            case "info":
                queryResponse = info();
                break;
            case "search":
                queryResponse = search();
                break;
            case "delete":
                queryResponse = delete();
                break;
            case "field-add":
                queryResponse = fieldAdd();
                break;
            case "field-delete":
                queryResponse = fieldDelete();
                break;
            case "field-rename":
                queryResponse = fieldRename();
                break;
            default:
                logger.error("Subcommand not valid");
                break;
        }

        createOutput(queryResponse);
    }

    private RestResponse<VariableSet> create() throws CatalogException, IOException {
        logger.debug("Creating variable");

        ObjectMap params = new ObjectMap();
        params.put(VariableSetParams.UNIQUE.key(), variableCommandOptions.createCommandOptions.unique);
        params.put(VariableSetParams.CONFIDENTIAL.key(), variableCommandOptions.createCommandOptions.confidential);
        params.putIfNotEmpty(VariableSetParams.DESCRIPTION.key(), variableCommandOptions.createCommandOptions.description);
        params.putIfNotEmpty(VariableSetParams.ID.key(), variableCommandOptions.createCommandOptions.name);

        ObjectMapper mapper = new ObjectMapper();
        ObjectMap variables = mapper.readValue(new File(variableCommandOptions.createCommandOptions.jsonFile), ObjectMap.class);

        params.putIfNotNull(VariableSetParams.VARIABLE.key(), variables.get("variables"));

        return openCGAClient.getVariableClient().create(resolveStudy(variableCommandOptions.createCommandOptions.study), params);
    }

    private RestResponse info() throws CatalogException, IOException {
        logger.debug("Getting variable information");

        QueryOptions queryOptions = new QueryOptions();
        queryOptions.putIfNotEmpty(VariableSetParams.STUDY_UID.key(), resolveStudy(variableCommandOptions.infoCommandOptions.studyId));
        queryOptions.putIfNotEmpty(QueryOptions.INCLUDE, variableCommandOptions.infoCommandOptions.include);
        queryOptions.putIfNotEmpty(QueryOptions.EXCLUDE, variableCommandOptions.infoCommandOptions.exclude);
        return openCGAClient.getVariableClient().get(variableCommandOptions.infoCommandOptions.id, queryOptions);
    }

    private RestResponse<VariableSet> search() throws CatalogException, IOException {
        logger.debug("Searching variable");

        Query query = new Query();
        query.put(SampleDBAdaptor.QueryParams.STUDY.key(), resolveStudy(variableCommandOptions.searchCommandOptions.study));

        QueryOptions queryOptions = new QueryOptions();
        queryOptions.putIfNotEmpty(VariableSetParams.ID.key(), variableCommandOptions.searchCommandOptions.name);
        queryOptions.putIfNotEmpty(VariableSetParams.DESCRIPTION.key(), variableCommandOptions.searchCommandOptions.description);
        queryOptions.putIfNotEmpty(VariableSetParams.ATTRIBUTES.key(), variableCommandOptions.searchCommandOptions.attributes);
        queryOptions.putIfNotEmpty(QueryOptions.INCLUDE, variableCommandOptions.searchCommandOptions.include);
        queryOptions.putIfNotEmpty(QueryOptions.EXCLUDE, variableCommandOptions.searchCommandOptions.exclude);
        queryOptions.putIfNotEmpty(QueryOptions.LIMIT, variableCommandOptions.searchCommandOptions.limit);
        queryOptions.putIfNotNull(QueryOptions.SKIP, variableCommandOptions.searchCommandOptions.skip);
        //TODO add when fixed the ws
        //queryOptions.putIfNotNull("count", variableCommandOptions.searchCommandOptions.count);

        return openCGAClient.getVariableClient().search(query, queryOptions);
    }

    private RestResponse<VariableSet> delete() throws CatalogException, IOException {
        logger.debug("Deleting variable");

        return openCGAClient.getVariableClient().delete(variableCommandOptions.deleteCommandOptions.id, new ObjectMap());
    }

    private RestResponse<VariableSet> fieldAdd() throws CatalogException, IOException {
        logger.debug("Adding variables to variable set");

        ObjectMapper mapper = new ObjectMapper();
        Object variables = mapper.readValue(new File(variableCommandOptions.fieldAddCommandOptions.jsonFile), ObjectMap.class)
                .get("variables");
        return openCGAClient.getVariableClient().addVariable(variableCommandOptions.fieldAddCommandOptions.id, variables);
    }

    private RestResponse<VariableSet> fieldDelete() throws CatalogException, IOException {
        logger.debug("Deleting the variable field");

        ObjectMap objectMap = new ObjectMap();
        return openCGAClient.getVariableClient().fieldDelete(variableCommandOptions.fieldDeleteCommandOptions.id,
                variableCommandOptions.fieldDeleteCommandOptions.name, objectMap);
    }

    private RestResponse<VariableSet> fieldRename() throws CatalogException, IOException {
        logger.debug("Rename the variable field");

        return openCGAClient.getVariableClient().fieldRename(
                variableCommandOptions.fieldRenameCommandOptions.id, variableCommandOptions.fieldRenameCommandOptions.oldName,
                variableCommandOptions.fieldRenameCommandOptions.newName, new ObjectMap());
    }
}
