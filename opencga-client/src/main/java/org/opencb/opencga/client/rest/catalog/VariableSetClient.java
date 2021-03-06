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

package org.opencb.opencga.client.rest.catalog;

import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.opencga.client.config.ClientConfiguration;
import org.opencb.opencga.core.models.VariableSet;
import org.opencb.opencga.core.response.RestResponse;

import java.io.IOException;

/**
 * Created by imedina on 24/05/16.
 */
public class VariableSetClient extends CatalogClient<VariableSet> {

    private static final String VARIABLES_URL = "variableset";

    public VariableSetClient(String sessionId, ClientConfiguration configuration, String userId) {
        super(userId, sessionId, configuration);

        this.category = VARIABLES_URL;
        this.clazz = VariableSet.class;
    }

    public RestResponse<VariableSet> create(String studyId, ObjectMap bodyParams) throws IOException {
        ObjectMap params = new ObjectMap();
        params = addParamsToObjectMap(params, "study", studyId, "body", bodyParams);
        return execute(VARIABLES_URL, "create", params, POST, VariableSet.class);
    }

    public RestResponse<VariableSet> update(String id, String study, ObjectMap bodyParams) throws IOException {
        ObjectMap params = new ObjectMap();
        params.put("body", bodyParams);
        return execute(category, id, "update", params, POST, clazz);
    }

    public RestResponse<VariableSet> addVariable(String variableSetid, Object variables) throws IOException {
        ObjectMap params = new ObjectMap()
                .append("body", variables)
                .append("variableSetId", variableSetid);
        return execute(VARIABLES_URL, "field/add", params, POST, VariableSet.class);
    }

    public RestResponse<VariableSet> deleteVariable(String variableSetid, String variable, ObjectMap params) throws IOException {
        params = addParamsToObjectMap(params, "variableSetId", variableSetid, "name", variable);
        return execute(VARIABLES_URL, "field/delete", params, GET, VariableSet.class);
    }

    public RestResponse<VariableSet> fieldRename(String variableSetId, String oldVariableName, String newVariableName, ObjectMap params)
            throws IOException {
        params = addParamsToObjectMap(params, "variableSetId", variableSetId, "oldName", oldVariableName, "newName", newVariableName);
        return execute(VARIABLES_URL, variableSetId, "field", null, "rename", params, GET, VariableSet.class);
    }

    public RestResponse<VariableSet> fieldDelete(String variableSetId, String variableName, ObjectMap params) throws IOException {
        params = addParamsToObjectMap(params, "variableSetId", variableSetId, "variableName", variableName);
        return execute(VARIABLES_URL, variableSetId, "field", null, "delete", params, GET, VariableSet.class);
    }
}
