package com.hedgefund.imf.store;

import com.hedgefund.ingest.store.SilverTransformer;

import java.nio.file.Path;

/** Delegates to framework generic passthrough (source_key,raw_len,bronze_path). */
public class ImfSilverTransformer {

    private final SilverTransformer delegate = new SilverTransformer();

    public Path transform(Path bronzeRoot, Path silverPath, String fileName) throws Exception {
        return delegate.transformGeneric(bronzeRoot, silverPath, fileName);
    }
}
