/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
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
package org.gbif.registry.ws.it.collections;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.vocabulary.collections.AccessionStatus;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.resources.collections.CollectionResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the {@link CollectionResource}. */
public class CollectionIT extends ExtendedCollectionEntityTest<Collection> {

  private static final String NAME = "name";
  private static final String DESCRIPTION = "dummy description";
  private static final AccessionStatus ACCESSION_STATUS = AccessionStatus.INSTITUTIONAL;
  private static final String CODE_UPDATED = "code2";
  private static final String NAME_UPDATED = "name2";
  private static final String DESCRIPTION_UPDATED = "dummy description updated";
  private static final AccessionStatus ACCESSION_STATUS_UPDATED = AccessionStatus.PROJECT;

  // query params
  private static final String CODE_PARAM = "code";
  private static final String NAME_PARAM = "name";
  private static final String INSTITUTION_PARAM = "institution";
  private static final String ALT_CODE_PARAM = "alternativeCode";

  @Autowired
  public CollectionIT(
      MockMvc mockMvc,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer,
      IdentityService identityService) {
    super(mockMvc, principalProvider, esServer, identityService, Collection.class);
  }

  @Test
  public void listTest() throws Exception {
    Collection collection1 = newEntity();
    collection1.setCode("c1");
    collection1.setName("n1");
    Address address = new Address();
    address.setAddress("dummy address");
    address.setCity("city");
    collection1.setAddress(address);
    collection1.setAlternativeCodes(Collections.singletonMap("alt", "test"));
    UUID key1 = createEntityCall(collection1);

    Collection collection2 = newEntity();
    collection2.setCode("c2");
    collection2.setName("n2");
    Address address2 = new Address();
    address2.setAddress("dummy address2");
    address2.setCity("city2");
    collection2.setAddress(address2);
    UUID key2 = createEntityCall(collection2);

    // query param
    assertEquals(2, listEntitiesCall(DEFAULT_QUERY_PARAMS.get()).getResults().size());
    assertEquals(2, listEntitiesCall(Q_SEARCH_PARAMS.apply("dummy")).getResults().size());

    // empty queries are ignored and return all elements
    assertEquals(2, listEntitiesCall(Q_SEARCH_PARAMS.apply("")).getResults().size());

    List<Collection> collections = listEntitiesCall(Q_SEARCH_PARAMS.apply("city")).getResults();
    assertEquals(1, collections.size());
    assertEquals(key1, collections.get(0).getKey());

    collections = listEntitiesCall(Q_SEARCH_PARAMS.apply("city2")).getResults();
    assertEquals(1, collections.size());
    assertEquals(key2, collections.get(0).getKey());

    assertEquals(2, listEntitiesCall(Q_SEARCH_PARAMS.apply("c")).getResults().size());
    assertEquals(2, listEntitiesCall(Q_SEARCH_PARAMS.apply("dum add")).getResults().size());
    assertEquals(0, listEntitiesCall(Q_SEARCH_PARAMS.apply("<")).getResults().size());
    assertEquals(0, listEntitiesCall(Q_SEARCH_PARAMS.apply("\"<\"")).getResults().size());
    assertEquals(2, listEntitiesCall(Q_SEARCH_PARAMS.apply(" ")).getResults().size());

    // code and name params
    Map<String, List<String>> params = DEFAULT_QUERY_PARAMS.get();
    params.put(CODE_PARAM, Collections.singletonList("c1"));
    assertEquals(1, listEntitiesCall(params).getResults().size());

    params = DEFAULT_QUERY_PARAMS.get();
    params.put(NAME_PARAM, Collections.singletonList("n2"));
    assertEquals(1, listEntitiesCall(params).getResults().size());

    params = DEFAULT_QUERY_PARAMS.get();
    params.put(CODE_PARAM, Collections.singletonList("c1"));
    params.put(NAME_PARAM, Collections.singletonList("n1"));
    assertEquals(1, listEntitiesCall(params).getResults().size());

    params.put(CODE_PARAM, Collections.singletonList("c2"));
    assertEquals(0, listEntitiesCall(params).getResults().size());

    // alternative code
    params = DEFAULT_QUERY_PARAMS.get();
    params.put(ALT_CODE_PARAM, Collections.singletonList("alt"));
    assertEquals(1, listEntitiesCall(params).getResults().size());

    params.put(ALT_CODE_PARAM, Collections.singletonList("foo"));
    assertEquals(0, listEntitiesCall(params).getResults().size());

    // update address
    collection2 = getEntityCall(key2);
    collection2.getAddress().setCity("city3");
    updateEntityCall(collection2);
    assertEquals(1, listEntitiesCall(Q_SEARCH_PARAMS.apply("city3")).getResults().size());

    deleteEntityCall(key2);
    assertEquals(0, listEntitiesCall(Q_SEARCH_PARAMS.apply("city3")).getResults().size());
  }

  @Test
  public void listByInstitutionTest() throws Exception {
    // institutions
    Institution institution1 = new Institution();
    institution1.setCode("code1");
    institution1.setName("name1");
    UUID institutionKey1 = createInstitutionCall(institution1);

    Institution institution2 = new Institution();
    institution2.setCode("code2");
    institution2.setName("name2");
    UUID institutionKey2 = createInstitutionCall(institution2);

    Collection collection1 = newEntity();
    collection1.setInstitutionKey(institutionKey1);
    createEntityCall(collection1);

    Collection collection2 = newEntity();
    collection2.setInstitutionKey(institutionKey1);
    createEntityCall(collection2);

    Collection collection3 = newEntity();
    collection3.setInstitutionKey(institutionKey2);
    createEntityCall(collection3);

    Map<String, List<String>> params = DEFAULT_QUERY_PARAMS.get();
    params.put(INSTITUTION_PARAM, Collections.singletonList(institutionKey1.toString()));
    assertEquals(2, listEntitiesCall(params).getResults().size());

    params.put(INSTITUTION_PARAM, Collections.singletonList(institutionKey2.toString()));
    assertEquals(1, listEntitiesCall(params).getResults().size());

    params.put(INSTITUTION_PARAM, Collections.singletonList(UUID.randomUUID().toString()));
    assertEquals(0, listEntitiesCall(params).getResults().size());
  }

  @Test
  public void listMultipleParamsTest() throws Exception {
    // institutions
    Institution institution1 = new Institution();
    institution1.setCode("code1");
    institution1.setName("name1");
    UUID institutionKey1 = createInstitutionCall(institution1);

    Institution institution2 = new Institution();
    institution2.setCode("code2");
    institution2.setName("name2");
    UUID institutionKey2 = createInstitutionCall(institution2);

    Collection collection1 = newEntity();
    collection1.setCode("code1");
    collection1.setInstitutionKey(institutionKey1);
    createEntityCall(collection1);

    Collection collection2 = newEntity();
    collection2.setCode("code2");
    collection2.setInstitutionKey(institutionKey1);
    createEntityCall(collection2);

    Collection collection3 = newEntity();
    collection3.setInstitutionKey(institutionKey2);
    createEntityCall(collection3);

    Map<String, List<String>> params = DEFAULT_QUERY_PARAMS.get();
    params.put(Q_PARAM, Collections.singletonList("code1"));
    params.put(INSTITUTION_PARAM, Collections.singletonList(institutionKey1.toString()));
    assertEquals(1, listEntitiesCall(params).getResults().size());

    params.put(Q_PARAM, Collections.singletonList("foo"));
    assertEquals(0, listEntitiesCall(params).getResults().size());

    params.put(Q_PARAM, Collections.singletonList("code2"));
    assertEquals(1, listEntitiesCall(params).getResults().size());

    params.put(INSTITUTION_PARAM, Collections.singletonList(institutionKey2.toString()));
    assertEquals(0, listEntitiesCall(params).getResults().size());
  }

  @Test
  public void testSuggest() throws Exception {
    Collection collection1 = newEntity();
    collection1.setCode("CC");
    collection1.setName("Collection name");
    UUID key1 = createEntityCall(collection1);

    Collection collection2 = newEntity();
    collection2.setCode("CC2");
    collection2.setName("Collection name2");
    UUID key2 = createEntityCall(collection2);

    assertEquals(2, suggestCall("collection").size());
    assertEquals(2, suggestCall("CC").size());
    assertEquals(1, suggestCall("CC2").size());
    assertEquals(1, suggestCall("name2").size());
  }

  @Override
  protected Collection newEntity() {
    Collection collection = new Collection();
    collection.setCode(UUID.randomUUID().toString());
    collection.setName(NAME);
    collection.setDescription(DESCRIPTION);
    collection.setActive(true);
    collection.setAccessionStatus(ACCESSION_STATUS);
    return collection;
  }

  @Override
  protected void assertNewEntity(Collection collection) {
    assertEquals(NAME, collection.getName());
    assertEquals(DESCRIPTION, collection.getDescription());
    assertEquals(ACCESSION_STATUS, collection.getAccessionStatus());
    assertTrue(collection.isActive());
  }

  @Override
  protected Collection updateEntity(Collection collection) {
    collection.setCode(CODE_UPDATED);
    collection.setName(NAME_UPDATED);
    collection.setDescription(DESCRIPTION_UPDATED);
    collection.setAccessionStatus(ACCESSION_STATUS_UPDATED);
    return collection;
  }

  @Override
  protected void assertUpdatedEntity(Collection collection) {
    assertEquals(CODE_UPDATED, collection.getCode());
    assertEquals(NAME_UPDATED, collection.getName());
    assertEquals(DESCRIPTION_UPDATED, collection.getDescription());
    assertEquals(ACCESSION_STATUS_UPDATED, collection.getAccessionStatus());
    assertNotEquals(collection.getCreated(), collection.getModified());
  }

  @Override
  protected Collection newInvalidEntity() {
    return new Collection();
  }

  @Override
  protected String getBasePath() {
    return "/grscicoll/collection/";
  }
}