package dev.smallbit.pinpoint.models;

import org.apache.commons.lang3.ArrayUtils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.shell.table.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

@Service
public class ElasticSearchIndexer {

  // TODO make this Bean compabible
  private ElasticsearchClient setupClient() {
    var restClient = RestClient.builder(new HttpHost("localhost", 9200)).build();
    var transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
    var client = new ElasticsearchClient(transport);
    return client;
  }

  // Store all the information in ES
  public void updateIndex(HashMap<String, Document> dictornary) {
    var client = setupClient();
    dictornary.forEach(
        (key, value) -> {
          System.out.println("Indexing " + key);
          try {
            IndexResponse response =
                client.index(i -> i.index("gutenberg").id(key).document(value));
            System.out.println(response);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
  }

  // TODO return something like a Collection of matches and confidence
  public Optional<Document> search(String term) {
    System.out.println("Searching for " + term);
    var client = setupClient();
    try {
      var response =
          client.search(
              s -> s.index("gutenberg").query(q -> q.match(t -> t.field("content").query(term))),
              Document.class);
      System.out.println(response);
      // NOTE we only care about the first result for now. We might change this to return
      // SearchResult<Document> instead
      if (response.hits().hits().size() > 0) {
        System.out.println(response.hits().hits().size());
        Document[] arr =
            response.hits().hits().stream().map(h -> h.source()).toArray(Document[]::new);
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
    // System.out.println("AARRRRRRR:" + arr);
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
