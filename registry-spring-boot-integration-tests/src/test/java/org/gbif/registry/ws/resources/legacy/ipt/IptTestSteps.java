package org.gbif.registry.ws.resources.legacy.ipt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.utils.LegacyInstallations;
import org.gbif.registry.utils.LenientAssert;
import org.gbif.registry.utils.Parsers;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.gbif.registry.ws.model.LegacyDataset;
import org.gbif.registry.ws.model.LegacyInstallation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.gbif.registry.ws.resources.legacy.ipt.AssertLegacyInstallation.assertLegacyInstallations;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestEmailConfiguration.class, RegistryIntegrationTestsConfiguration.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class IptTestSteps {

  private MockMvc mvc;
  private ResultActions result;

  private HttpHeaders requestParamsData;
  private Organization organization;
  private Installation actualInstallation;
  private Dataset actualDataset;
  private UUID organizationKey;
  private UUID installationKey;
  private Integer contactKeyBeforeSecondUpdate;
  private Integer endpointKeyBeforeSecondUpdate;
  private Date installationCreationDate;
  private String installationCreatedBy;

  @Autowired
  private DatasetService datasetService;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private DataSource ds;

  private Connection connection;

  @Autowired
  private OrganizationService organizationService;

  @Autowired
  private InstallationService installationService;

  @Before("@IPT")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/ipt/ipt_register_cleanup.sql"));
    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/ipt/ipt_register_prepare.sql"));

    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();
  }

  @After("@IPT")
  public void tearDown() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/ipt/ipt_register_cleanup.sql"));

    connection.close();
  }

  @Given("organization {string} with key {string}")
  public void prepareOrganization(String orgName, String orgKey) {
    organizationKey = UUID.fromString(orgKey);
    organization = organizationService.get(organizationKey);
  }

  @Given("installation {string} with key {string}")
  public void prepareInstallation(String instName, String installationKey) {
    this.installationKey = UUID.fromString(installationKey);
    actualInstallation = installationService.get(this.installationKey);
    installationCreationDate = actualInstallation.getCreated();
    installationCreatedBy = actualInstallation.getCreatedBy();
  }

  @Given("new installation to register")
  public void installationToCreate() {
    requestParamsData = LegacyInstallations.buildParams(organizationKey);
  }

  @Given("new dataset to register")
  public void datasetToRegister() {
    requestParamsData = LegacyInstallations.buildDatasetParams(organizationKey, installationKey);
  }

  @Given("without field {string}")
  public void removePrimaryContactFromParams(String field) {
    requestParamsData.remove(field);
  }

  @Given("installation to update")
  public void installationToUpdate() {
    requestParamsData = LegacyInstallations.buildParams(organizationKey);
  }

  @When("register new installation for organization {string} using organization key {string} and password {string}")
  public void registerIpt(String orgName, String organisationKey, String password) throws Exception {
    result = mvc
      .perform(
        post("/registry/ipt/register")
          .params(requestParamsData)
          .contentType(APPLICATION_FORM_URLENCODED)
          .accept(APPLICATION_XML)
          .with(httpBasic(organisationKey, password)))
      .andDo(print());
  }

  @When("register new dataset using organization key {string} and password {string}")
  public void registerDataset(String installationKey, String password) throws Exception {
    result = mvc
      .perform(
        post("/registry/ipt/resource")
          .params(requestParamsData)
          .contentType(APPLICATION_FORM_URLENCODED)
          .accept(APPLICATION_XML)
          .with(httpBasic(installationKey, password))
      ).andDo(print());
  }

  @When("update installation {string} using installation key {string} and password {string}")
  public void updateIpt(String instName, String installationKey, String password) throws Exception {
    result = mvc
      .perform(
        post("/registry/ipt/update/{key}", installationKey)
          .params(requestParamsData)
          .contentType(APPLICATION_FORM_URLENCODED)
          .accept(APPLICATION_XML)
          .with(httpBasic(installationKey, password)))
      .andDo(print());
  }

  @Then("response status should be {int}")
  public void checkResponseStatus(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @Then("installation UUID is returned")
  public void checkInstallationUuid() throws Exception {
    MvcResult mvcResult = result.andReturn();
    String contentAsString = mvcResult.getResponse().getContentAsString();
    Parsers.saxParser.parse(Parsers.getUtf8Stream(contentAsString), Parsers.legacyIptEntityHandler);
    assertNotNull("Registered IPT key should be in response", UUID.fromString(Parsers.legacyIptEntityHandler.key));
  }

  @Then("dataset UUID is returned")
  public void checkDatasetUuid() throws Exception {
    MvcResult mvcResult = result.andReturn();
    String contentAsString = mvcResult.getResponse().getContentAsString();
    Parsers.saxParser.parse(Parsers.getUtf8Stream(contentAsString), Parsers.legacyIptEntityHandler);
    assertNotNull("Registered Dataset key should be in response", UUID.fromString(Parsers.legacyIptEntityHandler.key));
  }

  @Then("registered installation is")
  public void checkRegisteredInstallationValidity() {
    LegacyInstallation expected = LegacyInstallations.newInstance(organizationKey);
    actualInstallation = installationService.get(UUID.fromString(Parsers.legacyIptEntityHandler.key));
    assertLegacyInstallations(expected, actualInstallation);
  }

  @Then("registered dataset is")
  public void checkRegisteredDatasetValidity(LegacyDataset expectedDataset) {
    actualDataset = datasetService.get(UUID.fromString(Parsers.legacyIptEntityHandler.key));
    copyGeneratedFieldsForDataset(expectedDataset, actualDataset);
    LenientAssert.assertLenientEquals("Datasets do not match", expectedDataset, actualDataset);
    assertNotNull(actualDataset.getCreatedBy());
    assertNotNull(actualDataset.getModifiedBy());
  }

  private void copyGeneratedFieldsForDataset(Dataset expectedDataset, Dataset actualDataset) {
    expectedDataset.setDoi(actualDataset.getDoi());
    expectedDataset.setCitation(actualDataset.getCitation());
  }

  @Then("registered dataset contacts are")
  public void checkDatasetContacts(List<Contact> expectedContacts) {
    for (int i = 0; i < expectedContacts.size(); i++) {
      Contact actualContact = actualDataset.getContacts().get(i);
      LenientAssert.assertLenientEquals("Contact does not match", expectedContacts.get(i), actualDataset.getContacts().get(i));
      assertNotNull(actualContact.getCreatedBy());
      assertNotNull(actualContact.getModifiedBy());
    }
  }

  @Then("registered dataset endpoints are")
  public void checkDatasetEndpoints(List<Endpoint> expectedEndpoints) {
    for (int i = 0; i < expectedEndpoints.size(); i++) {
      Endpoint actualEndpoint = actualDataset.getEndpoints().get(i);
      LenientAssert.assertLenientEquals("Endpoint does not match", expectedEndpoints.get(i), actualEndpoint);
      assertNotNull(actualEndpoint.getCreatedBy());
      assertNotNull(actualEndpoint.getModifiedBy());
    }
  }

  @Then("updated installation is valid")
  public void checkUpdatedInstallationValidity() {
    LegacyInstallation expected = LegacyInstallations.newInstance(organizationKey);
    actualInstallation = installationService.get(installationKey);
    assertLegacyInstallations(expected, actualInstallation);
    assertEquals(installationCreationDate, actualInstallation.getCreated());
    assertEquals(installationCreatedBy, actualInstallation.getCreatedBy());
  }

  @Then("total number of installation is {int}")
  public void checkNumberOfInstallations(int installationsNumber) {
    assertEquals(installationsNumber, installationService.list(new PagingRequest(0, 10)).getResults().size());
  }

  @Then("total number of contacts is {int}")
  public void checkNumberOfContacts(int contactsNumber) {
    assertEquals(contactsNumber, actualInstallation.getContacts().size());
  }

  @Then("total number of endpoints is {int}")
  public void checkNumberOfEndpoints(int endpointsNumber) {
    assertEquals(endpointsNumber, actualInstallation.getEndpoints().size());
  }

  @Given("store contactKey and endpointKey")
  public void storeContactKeyAndEndpointKey() {
    contactKeyBeforeSecondUpdate = actualInstallation.getContacts().get(0).getKey();
    endpointKeyBeforeSecondUpdate = actualInstallation.getEndpoints().get(0).getKey();
  }

  @Then("contactKey is the same")
  public void checkContactKeySameAfterUpdate() {
    // compare contact key and make sure it doesn't change after update (Contacts are mutable)
    assertEquals(contactKeyBeforeSecondUpdate, actualInstallation.getContacts().get(0).getKey());
  }

  @Then("endpointKey was updated")
  public void checkEndpointKeyNewAfterUpdate() {
    // compare endpoint key and make sure it does change after update (Endpoints are not mutable)
    assertNotEquals(endpointKeyBeforeSecondUpdate, actualInstallation.getEndpoints().get(0).getKey());
  }
}
