package dev.smallbit.pinpoint.models;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.apache.lucene.document.Document;
import org.apache.lucene.store.Directory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class LuceneIndexer {
  public enum SearchQueryType {
    SIMPLE,
    TERM,
    PREFIX,
    WILDCARD,
    PHRASE,
    FUZZY
  };

  private Path indexFullPath = null;

  public void updateIndex(HashMap<String, dev.smallbit.pinpoint.models.Document> dictionary) {
    try {
      updateIndexInternal(dictionary);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void updateIndexInternal(
      HashMap<String, dev.smallbit.pinpoint.models.Document> dictionary) throws IOException {
    Directory directory = FSDirectory.open(indexPath());

    // TODO test analyzers
    IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
    IndexWriter iwriter = new IndexWriter(directory, config);

    dictionary.forEach(
        (key, value) -> {
          System.out.println("Indexing " + key + "with lucene");

          var doc = new org.apache.lucene.document.Document();
          doc.add(new Field("id", value.id(), TextField.TYPE_STORED));
          doc.add(new Field("author", value.author(), TextField.TYPE_STORED));
          doc.add(new Field("excerpt", value.excerpt(), TextField.TYPE_STORED));
          doc.add(new Field("content", value.content(), TextField.TYPE_STORED));

          try {
            // TODO: Why does the throw above not work here?
            iwriter.addDocument(doc);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
    iwriter.close();
  }

  public Optional<dev.smallbit.pinpoint.models.Document> search(
      String fieldName, String queryString, SearchQueryType queryType) {
    try {
      TopDocs topDocs;

      if (queryType == SearchQueryType.SIMPLE) {
        topDocs = searchWithSimpleQuery(fieldName, queryString);
      } else {
        topDocs = searchByQueryType(fieldName, queryString, queryType);
      }

      List<Document> documents = new ArrayList<>();
      for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
        documents.add(getIndexSearcher().doc(scoreDoc.doc));
      }

      // -- Debug printing
      System.out.println("Found " + documents.size() + " documents");
      documents.forEach(d -> System.out.println(d.get("id")));
      // --

      var result =
          documents.stream()
              .map(LuceneIndexer::fromLuceneDocument)
              .toArray(dev.smallbit.pinpoint.models.Document[]::new);
      var firstDoc = result[0];
      return Optional.of(firstDoc);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public List<QueryTypeRank> compareSearchScoring(String fieldName, String queryString) {
    TopDocs topDocs;
    List<QueryTypeRank> ranks = new ArrayList<>();

    for (SearchQueryType type : SearchQueryType.values()) {
      topDocs = searchByQueryType(fieldName, queryString, type);

      if (topDocs.totalHits.value > 0) {
        var numHits = topDocs.totalHits.value;
        var topScore = topDocs.scoreDocs[0].score;
        var rank = new QueryTypeRank(type, numHits, topScore);
        ranks.add(rank);
      } else {
        var rank = new QueryTypeRank(type, 0, 0.0);
        ranks.add(rank);
      }
    }
    return ranks;
  }

  private TopDocs searchByQueryType(
      String fieldName, String queryString, SearchQueryType queryType) {
    try {
      Term term = new Term(fieldName, queryString);

      Query query;
      switch (queryType) {
        case TERM:
          query = new TermQuery(term);
          break;
        case PREFIX:
          query = new PrefixQuery(term);
          break;
        case WILDCARD:
          query = new WildcardQuery(term); // new Term(fieldName, "*" + queryString + "*"));
          break;
        case PHRASE:
          System.out.println(
              "Warning: We currently only have one string, so phrase query isn't really useful.");
          query = new PhraseQuery(term.field(), term.toString());
          break;
        case FUZZY:
          query = new FuzzyQuery(term);
          break;
        default:
          query = new TermQuery(term);
      }
      var searcher = getIndexSearcher();
      return searcher.search(query, 5);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public TopDocs searchWithSimpleQuery(String fieldName, String queryString) {
    try {
      var query = new QueryParser(fieldName, getAnalyzer()).parse(queryString);
      var searcher = getIndexSearcher();
      return searcher.search(query, 5);

    } catch (ParseException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  // @Bean
  private IndexSearcher getIndexSearcher() {
    try {
      var reader = DirectoryReader.open(FSDirectory.open(indexPath()));
      return new org.apache.lucene.search.IndexSearcher(reader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Bean
  private StandardAnalyzer getAnalyzer() {
    return new StandardAnalyzer();
  }

  @Bean
  private Path indexPath() throws IOException {
    if (this.indexFullPath == null) {
      String indexDirName = "lucenePinpointTempIndex";
      Path path = Files.createTempDirectory(indexDirName);
      System.out.println("Created new index at: " + path.toString());
      this.indexFullPath = path.toAbsolutePath();
    }
    return this.indexFullPath;
  }

  private static dev.smallbit.pinpoint.models.Document fromLuceneDocument(Document luceneDoc) {
    var author = luceneDoc.get("author");
    var excerpt = luceneDoc.get("excerpt");
    var id = luceneDoc.get("id");
    var content = luceneDoc.get("content");
    return new dev.smallbit.pinpoint.models.Document(author, id, content, excerpt);
  }
}
