# GBIF Registry WS Client

This project provides the WS client to the registry web services.
It implements the GBIF Registry API.

Internally the module uses [OpenFeign](https://github.com/OpenFeign/feign)
and [Spring Cloud OpenFeign](https://cloud.spring.io/spring-cloud-openfeign/reference/html/).

Common classes and configuration for clients can be found
in the project [gbif-common-ws](https://github.com/gbif/gbif-common-ws),
branch registry-spring-boot-integration.

The registry-ws client can be configured in the following ways:

## Read-only mode

Example:

```java
// set this to the web service URL.  It might be localhost:8080 for local development
String wsUrl = "http://api.gbif.org/v1/";
ClientFactory clientFactory = new ClientFactory(wsUrl);
DatasetServcie datasetClient = clientFactory.newInstance(DatasetClient.class);
```

## Read-write mode

This includes authentication functionality.
Make sure you are using right properties `wsUrl`, `appKey` and `secretKey`.
Example:

```java
// set this to the web service URL.  It might be localhost:8080 for local development
String wsUrl = "http://api.gbif.org/v1/";
String appKey = "app.key";
String secretKey = "secret-key";
String username = "username";
ClientFactory clientFactory = new ClientFactory(username, wsUrl, appKey, secretKey);
DatasetServcie datasetClient = clientFactory.newInstance(DatasetClient.class);
```

[Parent](../README.md)

