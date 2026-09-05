package com.hedgefund.yahoo.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown=true)
public record Bar(String symbol, String date, long epoch, double open, double high, double low, double close, double adjClose, long volume) {}
