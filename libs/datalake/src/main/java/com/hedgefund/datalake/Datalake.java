package com.hedgefund.datalake;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

/**
 * Local file-based datalake — Floci-compatible without Docker.
 * Layout mirrors Floci S3+Glue: s3://hedge-bronze/market_ticks/ -> datalake/data/bronze/market_ticks/
 */
public class Datalake {
    private static final Logger log = LoggerFactory.getLogger(Datalake.class);
    private final Path root;
    private final Path dataRoot;
    private final Path catalogPath;

    public Datalake(Path root) {
        this.root = root.toAbsolutePath().normalize();
        this.dataRoot = this.root.resolve("data");
        this.catalogPath = this.root.resolve("catalog/glue.json");
    }

    private static volatile Datalake cached;

    public static Datalake defaultLocal() {
        Datalake c = cached;
        if (c != null) return c;
        Path cwd = Path.of(System.getProperty("user.dir", "")).toAbsolutePath().normalize();
        for (Path cur = cwd; cur != null; cur = cur.getParent()) {
            Path candidate = cur.resolve("datalake");
            if (Files.exists(candidate.resolve("catalog/glue.json")) || Files.exists(candidate.resolve("data"))) {
                c = new Datalake(candidate);
                cached = c;
                return c;
            }
        }
        c = new Datalake(Path.of("datalake"));
        cached = c;
        return c;
    }

    public Path getRoot() { return root; }
    public Path getCatalogPath() { return catalogPath; }

    /** Resolve Floci S3 URI to local path, e.g. s3://hedge-bronze/market_ticks/ -> datalake/data/bronze/market_ticks/ */
    public Path resolveS3(String s3Uri) {
        // s3://bucket/key -> data/<bucket without hedge- prefix?>
        // hedge-bronze -> bronze, hedge-silver -> silver, hedge-gold -> gold
        String withoutScheme = s3Uri.replace("s3://", "");
        String[] parts = withoutScheme.split("/", 2);
        String bucket = parts[0];
        String key = parts.length > 1 ? parts[1] : "";
        String localBucket = switch (bucket) {
            case "hedge-bronze" -> "bronze";
            case "hedge-silver" -> "silver";
            case "hedge-gold" -> "gold";
            case "floci-firehose-results" -> "bronze/firehose";
            default -> bucket;
        };
        return dataRoot.resolve(localBucket).resolve(key);
    }

    public GlueCatalog loadCatalog() throws IOException {
        if (!Files.exists(catalogPath)) {
            log.warn("Catalog not found at {}, returning empty", catalogPath);
            return new GlueCatalog(List.of(), List.of());
        }
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return om.readValue(catalogPath.toFile(), GlueCatalog.class);
    }

    /** Provision sample data by delegating to Python script if available, else Java fallback */
    public void provisionSampleData() throws IOException, InterruptedException {
        Path script = root.resolve("scripts/provision.py");
        if (Files.exists(script)) {
            log.info("Provisioning via {}", script);
            var pb = new ProcessBuilder("python3", script.toString());
            pb.inheritIO();
            int rc = pb.start().waitFor();
            if (rc != 0) throw new IOException("provision.py failed: " + rc);
            return;
        }
        // Fallback: ensure dirs exist
        Files.createDirectories(dataRoot.resolve("bronze/market_ticks"));
        Files.createDirectories(dataRoot.resolve("silver/ohlcv"));
        Files.createDirectories(dataRoot.resolve("gold/positions"));
        log.info("Provision dirs created at {}", dataRoot);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GlueCatalog(List<Database> databases, List<Table> tables) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Database(String Name, String Description, String LocationUri) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Table(String DatabaseName, String Name, StorageDescriptor StorageDescriptor, String LocalPath) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StorageDescriptor(String Location, List<Column> Columns) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Column(String Name, String Type) {}
}
