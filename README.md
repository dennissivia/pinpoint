## What is this?

This app showcases document retrieval in Java.
Examples include:

* [ ] WIP: Elasticsearch Client
* [ ] TODO: Lucene
* [ ] TODO: A self implemented search using inverted index

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
