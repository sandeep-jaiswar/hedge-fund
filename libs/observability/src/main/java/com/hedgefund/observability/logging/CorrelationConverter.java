package com.hedgefund.observability.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class CorrelationConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String correlationId = event.getMDCPropertyMap().get("correlationId");
        String source = event.getMDCPropertyMap().get("source");
        String id = correlationId != null ? correlationId : "no-id";
        String src = source != null ? source : "-";
        return String.format("[%s:%s]", src, id);
    }
}
