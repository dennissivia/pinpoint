package dev.smallbit.pinpoint.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Document(String author, String id, String content, String excerpt) {}
