package dev.smallbit.pinpoint.models;

import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.apache.commons.lang3.ArrayUtils;

import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.table.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

@Service
public class ElasticSearchIndexer {

  @Bean
  private ElasticsearchClient restClient() {
    var restClient = RestClient.builder(new HttpHost("localhost", 9200)).build();
    var transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
    return new ElasticsearchClient(transport);
  }

  // Store all the information in ES
  public void updateIndex(HashMap<String, Document> dictornary) {
    dictornary.forEach(
        (key, value) -> {
          System.out.println("Indexing " + key);
          try {
            IndexResponse response =
                restClient().index(i -> i.index("gutenberg").id(key).document(value));
            System.out.println(response);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  // TODO return something like a Collection of matches and confidence
  public Optional<Document> search(String term) {
    System.out.println("Searching for " + term);
    try {
      var response =
          restClient()
              .search(
                  s ->
                      s.index("gutenberg").query(q -> q.match(t -> t.field("content").query(term))),
                  Document.class);
      //      System.out.println(response);

      var contentQuery = MatchQuery.of(m -> m.field("content").query(term))._toQuery();
      var response2 =
          restClient()
              .search(
                  s -> s.index("gutenberg").query(q -> q.bool(b -> b.must(contentQuery))),
                  Document.class);
      System.out.println("results using search: " + response.hits().hits().size());
      System.out.println("results using match query: " + response2.hits().hits().size());
      // NOTE we only care about the first result for now. We might change this to return
      // SearchResult<Document> instead

      if (!response.hits().hits().isEmpty()) {
        System.out.println(response.hits().hits().size());
        Document[] arr = response.hits().hits().stream().map(Hit::source).toArray(Document[]::new);
        printShellTable(arr);

        return Optional.of(response.hits().hits().get(0).source());
      } else {
        return Optional.empty();
      }
    } catch (ElasticsearchException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void printShellTable(Document[] arr) {
    var arrarr = Arrays.stream(arr).map(d -> d.toArray()).toArray(String[][]::new);

    // add a heading row
    var heading = new String[] {"Author", "ID", "Excerpt"};
    var tableData = ArrayUtils.addAll(new String[][] {heading}, arrarr);

    TableModel model = new ArrayTableModel(tableData);
    TableBuilder tableBuilder = new TableBuilder(model);
    tableBuilder.addFullBorder(BorderStyle.fancy_light);

    Table table = tableBuilder.build();
    String output = table.render(200);
    System.out.println(output);
  }
}
