package com.hedgefund.defillama.store;
import com.hedgefund.ingest.store.BronzeWriter;
import java.io.IOException;
import java.nio.file.Path;
public class DefillamaBronzeWriter {
    private final BronzeWriter delegate;
    public DefillamaBronzeWriter(Path bronzeRoot) { this.delegate = new BronzeWriter(bronzeRoot); }
    public Path write(String key, String raw) throws IOException { return delegate.writeNdjson(key, raw); }
}
