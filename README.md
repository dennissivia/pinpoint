## What is this?

This app showcases document retrieval in Java.
Examples include:

* [x] Simple Elasticsearch Client
    * Consider a more advanced example
* [x] Simple Lucene Client
    * Consider a more advanced example
* [ ] TODO (next): A self-implemented search using inverted index

## Get example data (gutenberg books)

You can download free (public domain) ebooks from [Project Gutenberg](https://www.gutenberg.org/).
A collection of these public domain ebooks is also distributed through the nltk library.
Their data page with many samples is at [nltk.org/nltk_data](https://www.nltk.org/nltk_data/).

## Build the application

Compile the app

````bash
./mvnw clean package -DskipTests=true
````

Build a native image with GraalVM for fast startup and low memory usage.

```bash
./mvnw -Pnative native:compile -DskipTests=true
```

## Run the app

```bash
java -jar target/pinpoint-0.1.1.jar help
```

Interactive commands:

* build index: `index`
* search gutenberg texts for a text: `search <text>`

## References

* ES guide: https://www.baeldung.com/elasticsearch-java
* Lucene:
    * https://livebook.manning.com/book/lucene-in-action-second-edition/chapter-3/
    * https://lucene.apache.org/core/9_9_0/core/org/apache/lucene/search/package-summary.html#query

## Troubleshooting

Currently, spring-shell does not exit when elastic.co java client is used.

## Future Ideas

* Consider [PicoCLI](https://picocli.info/) for minimalistic command line interface