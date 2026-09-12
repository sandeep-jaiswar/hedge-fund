package com.hedgefund.sec.store;

import com.hedgefund.ingest.store.BronzeWriter;

import java.io.IOException;
import java.nio.file.Path;

public class SecBronzeWriter {

    private final BronzeWriter delegate;

    public SecBronzeWriter(Path bronzeRoot) {
        this.delegate = new BronzeWriter(bronzeRoot);
    }

    public Path write(String key, String raw) throws IOException {
        return delegate.writeRawJson(key, raw);
    }

    public Path writeCsv(String key, String csv) throws IOException {
        return delegate.writeCsv(key, csv);
    }
}
