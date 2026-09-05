package com.hedgefund.worldbank.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PageEnvelope(int page, int pages, int per_page, int total, String sourceid, String lastupdated) {}
