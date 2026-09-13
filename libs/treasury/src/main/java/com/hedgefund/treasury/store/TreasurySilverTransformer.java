package com.hedgefund.treasury.store;

import com.hedgefund.ingest.store.SilverTransformer;

import java.io.IOException;
import java.nio.file.Path;

/** Delegates to framework generic passthrough (source_key,raw_len,bronze_path). */
public class TreasurySilverTransformer {

    private final SilverTransformer delegate = new SilverTransformer();

    public Path transform(Path bronzeRoot, Path silverPath, String fileName) throws IOException {
        return delegate.transformGeneric(bronzeRoot, silverPath, fileName);
    }
}
