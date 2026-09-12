package com.hedgefund.calcfi.store;

import com.hedgefund.ingest.store.BronzeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class CalcfiBronzeWriter {
    private static final Logger log = LoggerFactory.getLogger(CalcfiBronzeWriter.class);
    private final BronzeWriter delegate;

    public CalcfiBronzeWriter(Path bronzeRoot) {
        this.delegate = new BronzeWriter(bronzeRoot);
    }

    public Path write(String key, String raw) throws Exception {
        return delegate.writeNdjson(key, raw);
    }

    public Path writeCsv(String key, String csv) throws Exception {
        return delegate.writeCsv(key, csv);
    }
}
