package dev.smallbit.pinpoint.models;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
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

  private final String indexDirName = "lucenePinpointTempIndex";
  private Path indexFullPath;

  public void updateIndex(HashMap<String, dev.smallbit.pinpoint.models.Document> dictionary) {
    try {
      updateIndexInternal(dictionary);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Bean
  private Path indexPath() throws IOException {
    if (this.indexFullPath == null) {
      Path path = Files.createTempDirectory(this.indexDirName);
      this.indexFullPath = path.toAbsolutePath();
    }
    return this.indexFullPath;
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
      String fieldName, String queryString) {
    try {
      Query query = new QueryParser(fieldName, getAnalyzer()).parse(queryString);
      IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFullPath));
      var searcher = new org.apache.lucene.search.IndexSearcher(reader);
      TopDocs topDocs = searcher.search(query, 5);

      List<Document> documents = new ArrayList<>();
      for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
        documents.add(searcher.doc(scoreDoc.doc));
      }

      System.out.println("Found " + documents.size() + " documents");
      documents.stream().forEach(d -> System.out.println(d.get("id")));

      var firstDoc = documents.get(0);
      var author = firstDoc.get("author");
      var excerpt = firstDoc.get("excerpt");
      var id = firstDoc.get("id");
      var content = firstDoc.get("content");

      var result = new dev.smallbit.pinpoint.models.Document(author, id, content, excerpt);
      System.out.println(result.excerpt());

      return Optional.of(result);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    //    return Optional.empty();
  }

  @Bean
  private StandardAnalyzer getAnalyzer() {
    return new StandardAnalyzer();
  }
}
