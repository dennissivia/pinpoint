package dev.smallbit.pinpoint.models;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.HashMap;
import org.apache.lucene.document.Document;
import org.apache.lucene.store.Directory;
import org.springframework.stereotype.Service;

@Service
public class LuceneIndexer {
  public void updateIndex(HashMap<String, dev.smallbit.pinpoint.models.Document> dictionary) {
    try {
      updateIndexInternal(dictionary);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void updateIndexInternal(
      HashMap<String, dev.smallbit.pinpoint.models.Document> dictionary) throws IOException {
    Path indexPath = Files.createTempDirectory("tempIndex");
    Directory directory = FSDirectory.open(indexPath);

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

  public Optional<Document> search(String term) {
    return Optional.empty();
  }
}
