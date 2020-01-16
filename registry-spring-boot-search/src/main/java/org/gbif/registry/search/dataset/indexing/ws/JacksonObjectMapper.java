package org.gbif.registry.search.dataset.indexing.ws;

import org.gbif.api.jackson.LicenseSerde;
import org.gbif.api.vocabulary.License;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JacksonObjectMapper {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static {
    SimpleModule simpleModule = new SimpleModule();
    simpleModule.addSerializer(License.class, new LicenseSerde.LicenseJsonSerializer());
    simpleModule.addDeserializer(License.class, new LicenseSerde.LicenseJsonDeserializer());
    OBJECT_MAPPER.registerModule(simpleModule);
    OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }


  public static ObjectMapper get() {
    return OBJECT_MAPPER;
  }

}
