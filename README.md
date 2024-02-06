## What is this?

This app showcases document retrieval in Java.
Examples include:

* [x] Simple Elasticsearch Client
    * Consider a more advanced example
* [x] Simple Lucene Client
    * Consider a more advanced example
* [ ] TODO (next): A self implemented search using inverted index

## Get example data (gutenberg books)

You can download free (public domain) ebooks from [Project Gutenberg](https://www.gutenberg.org/).
A collection of these public domain ebooks is also distributed through the nltk library.
Their data page with many samples is at [nltk.org/nltk_data](https://www.nltk.org/nltk_data/).

## Build the application

Compile application to native image

```bash
./mvnw -Pnative native:compile -DskipTests=true
```
## Purpose

This repository is a minimal example of how including elasticsearch-rest-client causes issues with spring-shell.

## Running the app

To run the app, build the jar and run it with the default help command:

```bash
./mvnw clean package -DskipTests=true
java -jar target/shelltest-0.0.1-SNAPSHOT.jar help
```

If `elasticsearch-rest-client` is in the `pom.xml` the app does NOT finish. If that entry is removed it exits normally.

## Troubleshooting

Currently spring-shell does not exit when elastic.co java client is used.