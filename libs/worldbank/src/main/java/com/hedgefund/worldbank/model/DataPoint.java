package com.hedgefund.worldbank.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One observation, mirrors World Bank API second array element.
 * Fields nullable: value may be null, iso3 may be null for aggregates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DataPoint(
        IndicatorRef indicator,
        CountryRef country,
        String countryiso3code,
        String date,
        Double value,
        String unit,
        String obs_status,
        Integer decimal
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IndicatorRef(String id, String value) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CountryRef(String id, String value) {}

    public String indicatorId(){ return indicator!=null?indicator.id():null; }
    public String countryId(){ return country!=null?country.id():null; }
}
