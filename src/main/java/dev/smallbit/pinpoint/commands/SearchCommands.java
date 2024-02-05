package dev.smallbit.pinpoint.commands;

import dev.smallbit.pinpoint.models.Document;
import dev.smallbit.pinpoint.models.ElasticSearchIndexer;
import dev.smallbit.pinpoint.models.LuceneIndexer;
import dev.smallbit.pinpoint.models.QueryTypeRank;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.context.InteractionMode;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@ShellComponent()
public class SearchCommands {

  @Autowired private final ElasticSearchIndexer elasticSearchIndexer;
  @Autowired private final LuceneIndexer luceneIndexer;

  public SearchCommands(ElasticSearchIndexer indexer, LuceneIndexer luceneIndexer) {
    this.elasticSearchIndexer = indexer;
    this.luceneIndexer = luceneIndexer;
  }

  @ShellMethod(key = "search", value = "Search for a term")
  public String search(@ShellOption String searchTerm) {
    var resultDoc = elasticSearchIndexer.search(searchTerm);
    var resultDoc2 =
        luceneIndexer.search("content", searchTerm, LuceneIndexer.SearchQueryType.TERM);
    System.out.println(resultDoc2.get().id());

    if (resultDoc.isEmpty()) {
      return "No results found for: " + searchTerm;
    } else {
      var document = resultDoc.get();
      return String.format(
          "Found: %s in %s's work: %s", document.excerpt(), document.author(), document.id());
    }
  }

  @ShellMethod(key = "compareSearch", value = "Compare search scoring of different query types")
  public void compareSearch(@ShellOption String searchTerm) {
    // todo maybe result the TopDocs and print command line friendly
    var rankList = luceneIndexer.compareSearchScoring("content", searchTerm);
    printShellTable(rankList);
  }

  @ShellMethod(key = "index", value = "Index the given directory")
  public String index() {
    System.out.println("Indexing...");
    var dir = "src/main/resources/data/gutenberg/";
    var glob = "*.txt";
    var data = indexFiles(dir, glob);
    // test out ES
    elasticSearchIndexer.updateIndex(data);
    System.out.println("Indexed " + data.size() + " files with ElasticSearch.");
    luceneIndexer.updateIndex(data);
    System.out.println("Indexed " + data.size() + " files with Lucene.");
    return "finished indexing successfully";
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
                System.out.println("Added file contents of: " + file);
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

  private void printShellTable(List<QueryTypeRank> rankList) {

    var rankArray = rankList.stream().map(d -> d.toRow()).toArray(String[][]::new);

    // add a heading row
    var heading = new String[] {"Type", "Num of Hits", "Top Score"};
    var tableData = ArrayUtils.addAll(new String[][] {heading}, rankArray);

    TableModel model = new ArrayTableModel(tableData);
    TableBuilder tableBuilder = new TableBuilder(model);
    tableBuilder.addFullBorder(BorderStyle.fancy_light);

    Table table = tableBuilder.build();
    String output = table.render(200);
    System.out.println(output);
  }
}
