package com.hedgefund.baostock.store;
import com.hedgefund.ingest.store.BronzeWriter;
import java.io.IOException;
import java.nio.file.Path;
public class BaostockBronzeWriter {
    private final BronzeWriter delegate;
    public BaostockBronzeWriter(Path bronzeRoot) { this.delegate = new BronzeWriter(bronzeRoot); }
    public Path write(String key, String raw) throws IOException { return delegate.writeNdjson(key, raw); }
}
