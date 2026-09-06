package com.hedgefund.ingestionui.service;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Wires ingestion-ui into monorepo datalake + Floci: after each ingest, best-effort sync that source's
 * bronze/silver to s3://hedge-* if Floci is reachable at http://localhost:4566/_floci/health.
 * Falls back gracefully (no Docker required).
 */
@Service
public class FlociSyncService {
    private static final Logger log = LoggerFactory.getLogger(FlociSyncService.class);
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    public boolean isFlociRunning() {
        try {
            var req = HttpRequest.newBuilder(URI.create("http://localhost:4566/_floci/health"))
                    .timeout(Duration.ofSeconds(2)).GET().build();
            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() >= 200 && resp.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }

    public void syncAfterIngest(String source) {
        if (!isFlociRunning()) {
            log.info("Floci not running, skip sync for {}", source);
            return;
        }
        // best-effort: run python sync in background (monorepo script handles per-source s3://hedge-*)
        try {
            String script = "datalake/scripts/sync-all-to-floci.py";
            Path scriptPath = resolveScript(script);
            if (scriptPath == null) {
                log.warn("Floci sync script not found: {}", script);
                return;
            }
            log.info("Syncing {} to Floci via {}", source, scriptPath);
            var pb = new ProcessBuilder("python3", scriptPath.toString());
            pb.directory(scriptPath.getParent().getParent().getParent().toFile());
            pb.redirectErrorStream(true);
            var proc = pb.start();
            // wait at most 30s, don't block JobRunr worker forever
            boolean done = proc.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            if (done) {
                String out = new String(proc.getInputStream().readNBytes(8192));
                log.info("Floci sync done for {} exit={} out={}", source, proc.exitValue(), out.substring(0, Math.min(400, out.length())));
            } else {
                proc.destroyForcibly();
                log.warn("Floci sync timeout for {}", source);
            }
        } catch (Exception e) {
            log.warn("Floci sync failed for {}: {}", source, e.toString());
        }
    }

    private Path resolveScript(String script) {
        Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (Path p = cwd; p != null; p = p.getParent()) {
            Path candidate = p.resolve(script);
            if (candidate.toFile().exists()) return candidate.normalize();
        }
        return null;
    }
}
