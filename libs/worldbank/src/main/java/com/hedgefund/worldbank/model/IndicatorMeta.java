package com.hedgefund.worldbank.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IndicatorMeta(
        String id,
        String name,
        String unit,
        SourceRef source,
        String sourceNote,
        String sourceOrganization,
        List<TopicRef> topics
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SourceRef(String id, String value) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TopicRef(String id, String value) {}
}
