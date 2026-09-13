package com.hedgefund.worldbank.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.stream.Collectors;

/**
 * Reads bronze NDJSON via DuckDB, deduplicates, and writes Parquet directly.
 * No intermediate CSV — avoids the 676MB+ memory problem.
 */
public class SilverTransformer {
    private static final Logger log = LoggerFactory.getLogger(SilverTransformer.class);
    private final Path bronzeRoot;
    private final Path silverRoot;

    public SilverTransformer(Path datalakeRoot, String bronzeRel, String silverRel) {
        this.bronzeRoot = datalakeRoot.resolve(bronzeRel);
        this.silverRoot = datalakeRoot.resolve(silverRel);
    }

    public void transform() throws Exception {
        if (!Files.exists(bronzeRoot)) { log.warn("No bronze at {}", bronzeRoot); return; }

        String fileList;
        try (var stream = Files.walk(bronzeRoot)) {
            String paths = stream
                .filter(p -> p.getFileName().toString().equals("data.ndjson"))
                .map(p -> "'" + p.toAbsolutePath().toString().replace("'", "''") + "'")
                .collect(Collectors.joining(", "));
            if (paths.isEmpty()) { log.warn("No data.ndjson under {}", bronzeRoot); return; }
            fileList = "[" + paths + "]";
        }

        Path outDir = silverRoot.resolve("worldbank_observations");
        Files.createDirectories(outDir);
        Path parquet = outDir.resolve("observations.parquet");
        Path tmpParquet = outDir.resolve("observations.parquet.tmp");

        try (Connection c = DriverManager.getConnection("jdbc:duckdb:")) {
            c.createStatement().execute("INSTALL json; LOAD json;");
            c.createStatement().execute("SET threads=2");
            c.createStatement().execute("SET memory_limit='1.5GB'");

            c.createStatement().execute(String.format("""
                CREATE OR REPLACE TABLE silver AS
                SELECT * EXCLUDE rn FROM (
                  SELECT
                    json_extract_string(indicator, '$.id') AS indicator_id,
                    json_extract_string(indicator, '$.value') AS indicator_name,
                    json_extract_string(country, '$.id') AS country_id,
                    json_extract_string(country, '$.value') AS country_name,
                    countryiso3code AS country_iso3,
                    date,
                    CAST(regexp_extract(date, '\\d+', 0) AS VARCHAR) AS year,
                    CASE WHEN date LIKE '%%M' THEN 'M' WHEN date LIKE '%%Q' THEN 'Q' ELSE 'Y' END AS period_type,
                    value,
                    unit,
                    obs_status,
                    CAST(decimal AS VARCHAR) AS decimal,
                    _sourceid AS source_id,
                    _lastupdated AS lastupdated,
                    ROW_NUMBER() OVER (PARTITION BY json_extract_string(indicator, '$.id'), countryiso3code, date ORDER BY _lastupdated DESC) AS rn
                  FROM read_ndjson(%s, format='newline_delimited', hive_partitioning=false, filename=false)
                ) WHERE rn = 1
                """, fileList));

            long cnt;
            try (ResultSet rs = c.createStatement().executeQuery("SELECT count(*) FROM silver")) {
                rs.next();
                cnt = rs.getLong(1);
            }

            c.createStatement().execute(String.format("""
                COPY (SELECT * FROM silver ORDER BY indicator_id, country_iso3, date)
                TO '%s' (FORMAT PARQUET, COMPRESSION ZSTD, OVERWRITE_OR_IGNORE 1)
                """, tmpParquet.toString().replace("'", "''")));

            Files.move(tmpParquet, parquet, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            log.info("Silver wrote {} rows to {}", cnt, parquet);
        }
    }
}
