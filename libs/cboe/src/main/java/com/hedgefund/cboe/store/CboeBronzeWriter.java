package com.hedgefund.cboe.store;
import com.hedgefund.ingest.store.BronzeWriter;
import java.io.IOException;
import java.nio.file.Path;
public class CboeBronzeWriter {
    private final BronzeWriter delegate;
    public CboeBronzeWriter(Path bronzeRoot) { this.delegate = new BronzeWriter(bronzeRoot); }
    public Path write(String key, String raw) throws IOException { return delegate.writeNdjson(key, raw); }
}
