package org.gbif.identity.email;

import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;
import javax.mail.Session;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Guice module for emails related to the Identity service.
 * Requires: properties smtp.host, from, and bcc prefixed by identity.mail
 * Binds: {@link IdentityEmailManager}.
 */
public class IdentityEmailModule extends AbstractModule {

  private static final String propertyPrefix = "identity.mail.";
  private String SMTP_SERVER = "smtp.host";
  private String EMAIL_FROM = "from";
  private String EMAIL_BCC = "bcc";
  private Properties filteredProperties;

  public IdentityEmailModule(Properties properties) {
    filteredProperties = PropertiesUtil.filterProperties(properties, propertyPrefix);
  }

  @Override
  protected void configure() {
    bind(IdentityEmailManager.class).to(IdentityEmailManagerImpl.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Singleton
  private Session providesMailSession() {
    Properties props = new Properties();
    props.setProperty("mail.smtp.host", filteredProperties.getProperty(SMTP_SERVER));
    props.setProperty("mail.from", filteredProperties.getProperty(EMAIL_FROM));
    return Session.getInstance(props, null);
  }

  @Provides
  private @Named("bcc") String provideBccAddress () {
    return filteredProperties.getProperty(EMAIL_BCC);
  }

}
