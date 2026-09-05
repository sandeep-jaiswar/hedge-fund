package com.hedgefund.worldbank.store;

import com.hedgefund.worldbank.client.WorldBankClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Writes raw World Bank pages to datalake bronze (1 file per page + ndjson flattened). */
public class BronzeWriter {
    private static final Logger log = LoggerFactory.getLogger(BronzeWriter.class);
    private final Path bronzeRoot;

    public BronzeWriter(Path datalakeRoot, String bronzeRel) {
        this.bronzeRoot = datalakeRoot.resolve(bronzeRel);
    }

    public Path bronzeRoot(){ return bronzeRoot; }

    /** Write batch results. Partition: bronzeRoot/indicator={code}/ or full batch if multi-indicator. */
    public void writeBatch(List<String> indicatorBatch, String date, List<WorldBankClient.FetchResult> results) throws IOException {
        return; // deprecated, use batchIndex version
    }

    public void writeBatch(int batchIdx, List<String> indicatorBatch, String date, List<WorldBankClient.FetchResult> results) throws IOException {
        String batchId = indicatorBatch.size()==1? indicatorBatch.get(0) : String.format("batch_%04d", batchIdx);
        Path dir = bronzeRoot.resolve("indicator="+sanitize(batchId)).resolve("date="+sanitize(date==null?"all":date));
        Files.createDirectories(dir);
        int pg=1;
        for(WorldBankClient.FetchResult r: results){
            String fname = String.format("_raw_page=%d.json", pg);
            Path p = dir.resolve(fname);
            // Write tmp then atomic move
            Path tmp = dir.resolve(fname+".tmp");
            Files.writeString(tmp, r.rawBody());
            Files.move(tmp, p, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            log.info("Wrote bronze {}", p);
            pg++;
        }
        // Also flatten to ndjson for easy DuckDB read_json
        Path ndjson = dir.resolve("data.ndjson");
        try (var w = Files.newBufferedWriter(ndjson)) {
            for(WorldBankClient.FetchResult r: results){
                for(var dp: r.points()){
                    // write one JSON per line with extra _metadata
                    String line = String.format("{\"indicator\":{\"id\":\"%s\",\"value\":\"%s\"},\"country\":{\"id\":\"%s\",\"value\":\"%s\"},\"countryiso3code\":\"%s\",\"date\":\"%s\",\"value\":%s,\"unit\":\"%s\",\"obs_status\":\"%s\",\"decimal\":%s,\"_lastupdated\":\"%s\",\"_sourceid\":\"%s\"}",
                            esc(dp.indicatorId()), esc(dp.indicator()!=null?dp.indicator().value():""), esc(dp.countryId()), esc(dp.country()!=null?dp.country().value():""), esc(dp.countryiso3code()), esc(dp.date()), dp.value()==null?"null":dp.value().toString(), esc(dp.unit()), esc(dp.obs_status()), dp.decimal()==null?"null":dp.decimal().toString(), esc(results.get(0).envelope().lastupdated()), esc(results.get(0).envelope().sourceid()));
                    w.write(line);
                    w.newLine();
                }
            }
        }
        // meta
        Path meta = dir.resolve("_meta.json");
        Files.writeString(meta, String.format("{\"indicatorBatch\":%s,\"date\":\"%s\",\"pages\":%d,\"total\":%d,\"lastupdated\":\"%s\",\"writtenAt\":\"%s\"}",
                indicatorBatch.toString(), date, results.get(0).envelope().pages(), results.get(0).envelope().total(), results.get(0).envelope().lastupdated(), Instant.now().toString()));
    }

    private String sanitize(String s){ return s.replaceAll("[^a-zA-Z0-9._=-]","_"); }
    private String esc(String s){ return s==null?"":s.replace("\"","\\\""); }
}
