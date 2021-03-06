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

import org.opencb.commons.datastore.core.FacetField;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.client.config.ClientConfiguration;
import org.opencb.opencga.core.api.ParamConstants;
import org.opencb.opencga.core.models.File;
import org.opencb.opencga.core.models.FileTree;
import org.opencb.opencga.core.models.Job;
import org.opencb.opencga.core.response.RestResponse;

import java.io.IOException;

/**
 * Created by swaathi on 10/05/16.
 */
public class FileClient extends AnnotationClient<File> {

    private static final String FILES_URL = "files";

    public FileClient(String userId, String sessionId, ClientConfiguration configuration) {
        super(userId, sessionId, configuration);

        this.category = FILES_URL;
        this.clazz = File.class;
    }

    public RestResponse<File> createFolder(String studyId, String path, ObjectMap params) throws IOException {
        params = addParamsToObjectMap(params, "path", path, "directory", true);
        ObjectMap myParams = new ObjectMap()
                .append("study", studyId)
                .append("body", params);
        return execute(FILES_URL, "create", myParams, POST, File.class);
    }

    public RestResponse<File> relink(String fileId, String uri, QueryOptions options) throws IOException {
        ObjectMap params = new ObjectMap(options);
        params = addParamsToObjectMap(params, "uri", uri);
        return execute(FILES_URL, fileId.replace("/", ":"), "relink", params, GET, File.class);
    }

    public RestResponse<Job> unlink(String study, String fileId, ObjectMap params) throws IOException {
        params.putIfNotNull("study", study);
        return execute(FILES_URL, fileId.replace("/", ":"), "unlink", params, DELETE, Job.class);
    }

    public RestResponse<File> content(String fileId, ObjectMap params) throws IOException {
        return execute(FILES_URL, fileId.replace("/", ":"), "content", params, GET, File.class);
    }

    public RestResponse<File> download(String fileId, String fileTarget, ObjectMap params) throws IOException {
        params = params != null ? params : new ObjectMap();
        params.put("OPENCGA_DESTINY", fileTarget);
        return execute(FILES_URL, fileId.replace("/", ":"), "download", params, GET, File.class);
    }

    public RestResponse<File> grep(String fileId, String pattern, ObjectMap params) throws IOException {
        params = addParamsToObjectMap(params, "pattern", pattern);
        return execute(FILES_URL, fileId.replace("/", ":"), "grep", params, GET, File.class);
    }

    public RestResponse<File> list(String folderId, ObjectMap options) throws IOException {
        folderId = folderId.replace('/', ':');
        return execute(FILES_URL, folderId, "list", options, GET, File.class);
    }

    public RestResponse<Job> delete(String study, String fileId, ObjectMap params) throws IOException {
        params.putIfNotNull("study", study);
        return execute(FILES_URL, fileId.replace("/", ":"), "delete", params, DELETE, Job.class);
    }

    public RestResponse<FileTree> tree(String folderId, ObjectMap params) throws IOException {
        return execute(FILES_URL, folderId.replace("/", ":"), "tree", params, GET, FileTree.class);
    }

    public RestResponse<File> refresh(String fileId, ObjectMap options) throws IOException {
        return execute(FILES_URL, fileId.replace("/", ":"), "refresh", options, GET, File.class);
    }

    public RestResponse<File> upload(String studyId, String filePath, ObjectMap params) throws IOException {
        params = addParamsToObjectMap(params, ParamConstants.STUDY_PARAM, studyId, "file", filePath);
        return execute(FILES_URL, "upload", params, POST, File.class);
    }

    public RestResponse<File> groupBy(String studyId, String fields, ObjectMap params) throws IOException {
        params = addParamsToObjectMap(params, ParamConstants.STUDY_PARAM, studyId, "fields", fields);
        return execute(FILES_URL, "groupBy", params, GET, File.class);
    }

    public RestResponse<FacetField> stats(String study, Query query, QueryOptions queryOptions) throws IOException {
        ObjectMap params = new ObjectMap(query);
        params.putAll(queryOptions);
        params.put("study", study);
        return execute(FILES_URL, "stats", params, GET, FacetField.class);
    }
}
