package dev.smallbit.pinpoint.models;

import java.lang.reflect.Array;

public record QueryTypeRank(
    LuceneIndexer.SearchQueryType queryType, long numHits, double topScore) {

  public String[] toRow() {
    return new String[] {queryType.toString(), String.valueOf(numHits), String.valueOf(topScore)};
  }
}
