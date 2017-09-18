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

package org.opencb.opencga.server.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.generic.GenericRecord;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.opencb.biodata.models.alignment.Alignment;
import org.opencb.biodata.models.feature.Genotype;
import org.opencb.biodata.models.variant.VariantSource;
import org.opencb.biodata.models.variant.stats.VariantStats;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.core.QueryResponse;
import org.opencb.opencga.catalog.db.api.IndividualDBAdaptor;
import org.opencb.opencga.catalog.managers.CatalogManagerTest;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.core.models.Individual;
import org.opencb.opencga.storage.core.alignment.json.AlignmentDifferenceJsonMixin;
import org.opencb.opencga.storage.core.variant.io.json.mixin.GenericRecordAvroJsonMixin;
import org.opencb.opencga.storage.core.variant.io.json.mixin.GenotypeJsonMixin;
import org.opencb.opencga.storage.core.variant.io.json.mixin.VariantSourceJsonMixin;
import org.opencb.opencga.storage.core.variant.io.json.mixin.VariantStatsJsonMixin;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by jacobo on 22/06/15.
 */
public class IndividualWSServerTest {

    private static WSServerTestUtils serverTestUtils;
    private WebTarget webTarget;
    private static ObjectMapper jsonObjectMapper;
    private String sessionId;
    private long studyId;
    private long in1;
    private long in2;
    private long in3;
    private long in4;

    {
        jsonObjectMapper = new ObjectMapper();
        jsonObjectMapper.addMixIn(GenericRecord.class, GenericRecordAvroJsonMixin.class);
        jsonObjectMapper.addMixIn(VariantSource.class, VariantSourceJsonMixin.class);
        jsonObjectMapper.addMixIn(VariantStats.class, VariantStatsJsonMixin.class);
        jsonObjectMapper.addMixIn(Genotype.class, GenotypeJsonMixin.class);
        jsonObjectMapper.addMixIn(Alignment.AlignmentDifference.class, AlignmentDifferenceJsonMixin.class);
        jsonObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        jsonObjectMapper.configure(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS, true);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    static public void initServer() throws Exception {
        serverTestUtils = new WSServerTestUtils();
        serverTestUtils.setUp();
        serverTestUtils.initServer();
    }

    @AfterClass
    static public void shutdownServer() throws Exception {
        serverTestUtils.shutdownServer();
    }

    @Before
    public void init() throws Exception {
        webTarget = serverTestUtils.getWebTarget();
        sessionId = OpenCGAWSServer.catalogManager.getUserManager().login("user", CatalogManagerTest.PASSWORD);
        studyId = OpenCGAWSServer.catalogManager.getStudyManager().getId("user", "1000G:phase1");
        in1 = OpenCGAWSServer.catalogManager.getIndividualManager().create(studyId, "in1", "f1", (long) -1, (long) -1, null, "", "", "",
                "", "", Individual.KaryotypicSex.UNKNOWN, Individual.LifeStatus.UNKNOWN, Individual.AffectationStatus.UNKNOWN, null,
                sessionId).first().getId();
        in2 = OpenCGAWSServer.catalogManager.getIndividualManager().create(studyId, "in2", "f1", (long) -1, (long) -1, null, "", "", "",
                "", "", Individual.KaryotypicSex.UNKNOWN, Individual.LifeStatus.UNKNOWN, Individual.AffectationStatus.UNKNOWN, null, sessionId).first().getId();
        in3 = OpenCGAWSServer.catalogManager.getIndividualManager().create(studyId, "in3", "f2", (long) -1, (long) -1, null, "", "", "",
                "", "", Individual.KaryotypicSex.UNKNOWN, Individual.LifeStatus.UNKNOWN, Individual.AffectationStatus.UNKNOWN, null, sessionId).first().getId();
        in4 = OpenCGAWSServer.catalogManager.getIndividualManager().create(studyId, "in4", "f2", (long) -1, (long) -1, null, "", "", "",
                "", "", Individual.KaryotypicSex.UNKNOWN, Individual.LifeStatus.UNKNOWN, Individual.AffectationStatus.UNKNOWN, null, sessionId).first().getId();
    }

    @After
    public void after() throws Exception {
        // It is here to avoid restarting the server again and again
        serverTestUtils.setUp();
    }

    @Test
    public void createIndividualTest() throws IOException {
        ObjectMap params = new ObjectMap()
                .append("name", "new_individual1")
                .append("sex", "FEMALE");
        String json = webTarget.path("individuals").path("create")
                .queryParam("study", studyId)
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sessionId)
                .post(Entity.json(jsonObjectMapper.writeValueAsString(params)))
                .readEntity(String.class);

        QueryResponse<Individual> response = WSServerTestUtils.parseResult(json, Individual.class);

        Individual individual = response.getResponse().get(0).first();
        assertEquals(Individual.Sex.FEMALE, individual.getSex());
        assertEquals("new_individual1", individual.getName());
        assertTrue(individual.getId() > 0);
    }

    @Test
    public void getIndividualTest() throws IOException {
        String json = webTarget.path("individuals").path(Long.toString(in1)).path("info")
                .queryParam("study", studyId)
                .queryParam("exclude", "projects.studies.individuals.sex")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sessionId)
                .get(String.class);

        QueryResponse<Individual> response = WSServerTestUtils.parseResult(json, Individual.class);

        Individual individual = response.getResponse().get(0).first();
        assertEquals("f1", individual.getFamily());
        assertEquals(null, individual.getSex());
        assertTrue(individual.getId() > 0);
    }

    @Test
    public void searchIndividualTest() throws IOException, CatalogException {
        String json = webTarget.path("individuals").path("search")
                .queryParam("study", studyId)
                .queryParam("family", "f1")
                .queryParam("exclude", "projects.studies.individuals.sex")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sessionId)
                .get(String.class);

        QueryResponse<Individual> response = WSServerTestUtils.parseResult(json, Individual.class);

        List<Individual> result = response.getResponse().get(0).getResult();

        assertEquals(2, result.size());
        for (Individual individual : result) {
            assertEquals("f1", individual.getFamily());
            assertEquals(null, individual.getSex());
            assertTrue(individual.getId() > 0);
        }

        // We look for an individual with father "in2"
        json = webTarget.path("individuals").path("search")
                .queryParam("study", studyId)
                .queryParam("father", "in2")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sessionId)
                .get(String.class);
        response = WSServerTestUtils.parseResult(json, Individual.class);
        assertEquals(0, response.getResponse().get(0).getNumResults());

        // We update to make the individual in1 be the child of in2
        ObjectMap params = new ObjectMap(IndividualDBAdaptor.QueryParams.FATHER.key(), "in2");
        OpenCGAWSServer.catalogManager.getIndividualManager().update(String.valueOf(studyId), "in1", params, QueryOptions.empty(),
                sessionId);
        // and query again. We look for an individual with father "in2"
        json = webTarget.path("individuals").path("search")
                .queryParam("study", studyId)
                .queryParam("father", "in2")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sessionId)
                .get(String.class);
        response = WSServerTestUtils.parseResult(json, Individual.class);
        assertEquals(1, response.getResponse().get(0).getNumResults());

    }

    @Test
    public void updateIndividualTest() throws IOException {
        ObjectMap params = new ObjectMap()
                .append("mother", "in2")
                .append("father", "in3")
                .append("multiples", new ObjectMap()
                        .append("type", "my type")
                        .append("siblings", Arrays.asList("in4")));
        String json = webTarget.path("individuals").path(Long.toString(in1)).path("update")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sessionId)
                .post(Entity.json(jsonObjectMapper.writeValueAsString(params)))
                .readEntity(String.class);

        QueryResponse<Individual> response = WSServerTestUtils.parseResult(json, Individual.class);

        Individual individual = response.getResponse().get(0).first();
        assertTrue(individual.getFather().getId() > 0);
        assertTrue(individual.getMother().getId() > 0);
        assertEquals(1, individual.getMultiples().getSiblings().size());
        assertEquals("in4", individual.getMultiples().getSiblings().get(0));
    }

    @Test
    public void deleteIndividualTest() throws IOException, CatalogException {
        String json = webTarget.path("individuals").path(Long.toString(in1)).path("delete")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + sessionId)
                .get(String.class);

        QueryResponse<Individual> response = WSServerTestUtils.parseResult(json, Individual.class);

        Individual individual = response.getResponse().get(0).first();

        // FIXME: This will have to return an exception once we improve the delete behaviour
//        thrown.expect(CatalogException.class);
        OpenCGAWSServer.catalogManager.getIndividualManager().get(null, String.valueOf((Long) individual.getId()), null, sessionId);

    }

}
