package dev.smallbit.pinpoint.commands;

import dev.smallbit.pinpoint.models.Document;
import dev.smallbit.pinpoint.models.ElasticSearchIndexer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

@ShellComponent
public class SearchCommands {

  @Autowired private final ElasticSearchIndexer elasticSearchIndexer;

  public SearchCommands(ElasticSearchIndexer indexer) {
    this.elasticSearchIndexer = indexer;
  }

  @ShellMethod(key = "search", value = "Search for a term")
  public String search(@ShellOption String searchTerm) {
    var resultDoc = elasticSearchIndexer.search(searchTerm);
    if (resultDoc.isEmpty()) {
      return "No results found for: " + searchTerm;
    } else {
      var document = resultDoc.get();
      return String.format(
          "Found: %s in %s's work: %s", document.excerpt(), document.author(), document.id());
    }
  }

  @ShellMethod(key = "index", value = "Index the given directory")
  public String index() {
    System.out.println("Indexing...");
    var dir = "src/main/resources/data/gutenberg/";
    var glob = "*.txt";
    var data = indexFiles(dir, glob);
    // test out ES
    elasticSearchIndexer.updateIndex(data);
    return "Indexed " + data.size() + " files with ElasticSearch.";
  }

  // TODO: Can we just propagate the IOExceptions instead? We cant have partial results then, but it
  // will clean up the code
  private HashMap<String, Document> indexFiles(String pathString, String globString) {
    var dataMap = new HashMap<String, Document>();

    try {
      Files.newDirectoryStream(Paths.get(pathString), globString)
          .forEach(
              file -> {
                var document = toDocument(file);
                dataMap.put(file.getFileName().toString(), document);
                System.out.println("Could not read file: " + file);
              });
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("Could not read files from directory: " + globString);
    }
    return dataMap;
  }

  private Document toDocument(Path filename) {
    try {
      var path = filename.toAbsolutePath();

      var id = path.getFileName().toString();
      var content = Files.readString(path, StandardCharsets.ISO_8859_1);
      var excerpt = content.substring(0, Math.min(content.length(), 100));
      var authorName = filename.getFileName().toString().split("-")[0];
      return new Document(authorName, id, content, excerpt);
    } catch (IOException e) {
      throw new RuntimeException("Could not read file: " + filename);
    }
  }
}
