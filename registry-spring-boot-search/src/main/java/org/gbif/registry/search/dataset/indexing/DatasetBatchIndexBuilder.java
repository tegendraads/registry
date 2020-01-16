package org.gbif.registry.search.dataset.indexing;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.search.dataset.indexing.es.EsClient;
import org.gbif.registry.search.dataset.indexing.es.IndexingConstants;
import org.gbif.registry.search.dataset.indexing.ws.GbifWsClient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * A builder that will clear and build a new dataset index by paging over the given service.
 */
@SpringBootApplication
@Slf4j
public class DatasetBatchIndexBuilder implements CommandLineRunner{

  // controls how many results we request while paging over the WS
  private static final int PAGE_SIZE = 100;

  /**
   * Pages over all datasets and adds them to ElasticSearch.
   */
  @Override
  public void run(String... args)  {
      log.info("Building a new Dataset index");
      Stopwatch stopwatch = Stopwatch.createStarted();
      GbifWsClient gbifWsClient = GbifWsClient.create(args[0]);
      EsClient esClient = new EsClient(args[1].split(","));
      String indexName  = "dataset_" + new Date().getTime();
      esClient.createIndex(indexName, IndexingConstants.DATASET_RECORD_TYPE, IndexingConstants.DEFAULT_INDEXING_SETTINGS, IndexingConstants.MAPPING_FILE);

      ExecutorService executor = Executors.newWorkStealingPool();

      List<CompletableFuture<BulkResponse>> jobs = new ArrayList<>();
      DatasetJsonConverter datasetJsonConverter = DatasetJsonConverter.create(gbifWsClient);

      onAllDatasets(gbifWsClient, pagingResponse ->
        jobs.add(CompletableFuture.supplyAsync(() -> index(pagingResponse, datasetJsonConverter,indexName,esClient),
                                               executor)));

      CompletableFuture.allOf(jobs.toArray(new CompletableFuture[]{}));

      logIndexingErrors(jobs);
      executor.shutdown();
      esClient.updateSettings(indexName, IndexingConstants.DEFAULT_SEARCH_SETTINGS);
      esClient.swapAlias(IndexingConstants.ALIAS, indexName);
      esClient.close();
      log.info("Finished building Dataset index in {} secs", stopwatch.elapsed(TimeUnit.SECONDS));
  }

  @SneakyThrows
  private BulkResponse index(PagingResponse<Dataset> pagingResponse, DatasetJsonConverter datasetJsonConverter, String indexName, EsClient esClient) {

      BulkRequest bulkRequest = new BulkRequest();
      pagingResponse.getResults().forEach(dataset -> {
        ObjectNode jsonNode = datasetJsonConverter.convert(dataset);
        bulkRequest.add(new IndexRequest().index(indexName).source(jsonNode.toString(), XContentType.JSON)
                          .opType(DocWriteRequest.OpType.INDEX)
                          .id(dataset.getKey().toString())
                          .type(IndexingConstants.DATASET_RECORD_TYPE));

      });
      // Batching updates to Es proves quicker with batches of 100 - 1000 showing similar performance
      log.info("Indexing {} datasets until at offset {}", pagingResponse.getLimit(), pagingResponse.getOffset());
      return esClient.bulk(bulkRequest);
  }

  private static void onAllDatasets(GbifWsClient gbifWsClient, Consumer<PagingResponse<Dataset>> responseConsumer) {
    PagingRequest page = new PagingRequest(0, PAGE_SIZE);
    PagingResponse<Dataset> response = gbifWsClient.listDatasets(new PagingRequest(0,0));
    do {
      log.debug("Requesting {} datasets starting at offset {}", page.getLimit(), page.getOffset());
      PagingResponse<Dataset> pagingResponse = gbifWsClient.listDatasets(page);
      response.setEndOfRecords(pagingResponse.isEndOfRecords());
      responseConsumer.accept(pagingResponse);
      page.nextPage();
    } while (!response.isEndOfRecords());

  }

  private static void logIndexingErrors(List<CompletableFuture<BulkResponse>> jobs) {
    jobs.forEach(job -> {
      try {
        BulkResponse bulkResponse = job.get();
        if (bulkResponse.hasFailures()) {
          log.error("Error in indexing job {}", bulkResponse.buildFailureMessage());
        }
      } catch (InterruptedException| ExecutionException ex) {
        log.error("Error executing job", ex);
      }
    });
  }

  public static void main (String[] args) {
    SpringApplication.run(DatasetBatchIndexBuilder.class, args).close();
  }
}
