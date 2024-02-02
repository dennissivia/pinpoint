package dev.smallbit.pinpoint.models;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;

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
  public Document search(String term) {
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
      return response.hits().hits().get(0).source();

    } catch (ElasticsearchException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
