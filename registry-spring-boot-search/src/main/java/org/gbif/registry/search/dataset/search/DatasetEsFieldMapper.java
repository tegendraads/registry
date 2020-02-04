package org.gbif.registry.search.dataset.search;

import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.registry.search.dataset.search.common.EsFieldMapper;

import com.google.common.collect.ImmutableBiMap;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FieldValueFactorFunctionBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;

public class DatasetEsFieldMapper implements EsFieldMapper<DatasetSearchParameter> {

  private static final ImmutableBiMap<DatasetSearchParameter,String> SEARCH_TO_ES_MAPPING = ImmutableBiMap.<DatasetSearchParameter,String>builder()
    .put(DatasetSearchParameter.TAXON_KEY, "taxonKey")
    .put(DatasetSearchParameter.CONTINENT, "continent")
    .put(DatasetSearchParameter.COUNTRY, "country")
    .put(DatasetSearchParameter.PUBLISHING_COUNTRY, "publishingCountry")
    .put(DatasetSearchParameter.YEAR, "year")
    .put(DatasetSearchParameter.DECADE, "decade")
    .put(DatasetSearchParameter.HOSTING_ORG, "hostingOrganizationKey")
    .put(DatasetSearchParameter.KEYWORD, "keyword")
    .put(DatasetSearchParameter.LICENSE, "license")
    .put(DatasetSearchParameter.MODIFIED_DATE, "modified")
    .put(DatasetSearchParameter.PROJECT_ID, "project.identifier")
    .put(DatasetSearchParameter.PUBLISHING_ORG, "publishingOrganizationKey")
    .put(DatasetSearchParameter.RECORD_COUNT,"occurrenceCount")
    .put(DatasetSearchParameter.SUBTYPE, "subtype")
    .put(DatasetSearchParameter.TYPE, "type")
    .put(DatasetSearchParameter.DATASET_TITLE, "title")
    .build();

  private static final String[] EXCLUDE_FIELDS = new String[]{"all","taxonKey"};

  private static final String[] DATASET_TITLE_SUGGEST_FIELDS = new String[]{"title", "type", "subtype", "description"};

  private static final String[] DATASET_HIGHLIGHT_FIELDS = new String[]{"title", "description"};

  private static final FieldValueFactorFunctionBuilder FULLTEXT_SCORE_FUNCTION = ScoreFunctionBuilders
    .fieldValueFactorFunction("dataScore")
    .modifier(FieldValueFactorFunction.Modifier.LN2P)
    .missing(0d);



  @Override
  public DatasetSearchParameter get(String esField) {
    return SEARCH_TO_ES_MAPPING.inverse().get(esField);
  }

  @Override
  public String get(DatasetSearchParameter datasetSearchParameter) {
    return SEARCH_TO_ES_MAPPING.get(datasetSearchParameter);
  }

  @Override
  public String[] excludeFields() {
    return EXCLUDE_FIELDS;
  }

  @Override
  public String[] includeSuggestFields(DatasetSearchParameter searchParameter) {
    if (DatasetSearchParameter.DATASET_TITLE == searchParameter) {
      return DATASET_TITLE_SUGGEST_FIELDS;
    }
    return new String[]{SEARCH_TO_ES_MAPPING.get(searchParameter)};
  }

  @Override
  public String[] highlightingFields() {
    return DATASET_HIGHLIGHT_FIELDS;
  }

  @Override
  public QueryBuilder fullTextQuery(String q) {

    return new FunctionScoreQueryBuilder(QueryBuilders.multiMatchQuery(q)
                                           .field("title", 20.0f)
                                           .field("keyword", 10.0f)
                                           .field("description",8.0f)
                                           .field("publishingOrganizationTitle",5.0f)
                                           .field("hostingOrganizationTitle", 5.0f)
                                           .field("metadata", 3.0f)
                                           .field("projectId" ,2.0f)
                                           .field("all",1.0f)
                                           .tieBreaker(0.2f).minimumShouldMatch("25%").slop(100),
                                         FULLTEXT_SCORE_FUNCTION)
      .boostMode(CombineFunction.MULTIPLY);
  }
}
