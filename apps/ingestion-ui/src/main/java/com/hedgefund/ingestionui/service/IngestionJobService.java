package com.hedgefund.ingestionui.service;

import com.hedgefund.datalake.Datalake;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IngestionJobService {
    private static final Logger log = LoggerFactory.getLogger(IngestionJobService.class);
    private final JobScheduler scheduler;
    private final StorageProvider storageProvider;
    private final FlociSyncService flociSync;
    private final SourceRegistry registry;

    public IngestionJobService(JobScheduler scheduler, StorageProvider storageProvider, FlociSyncService flociSync, SourceRegistry registry) {
        this.scheduler = scheduler;
        this.storageProvider = storageProvider;
        this.flociSync = flociSync;
        this.registry = registry;
    }

    public UUID enqueue(String source, String config) {
        String cfg = config != null ? config : "config/" + source + "/" + source + ".yaml";
        log.info("Enqueue ingest source={} config={}", source, cfg);
        return scheduler.enqueue(() -> runIngest(source, cfg)).asUUID();
    }

    public UUID schedule(String source, String config, java.time.Instant when) {
        String cfg = config != null ? config : "config/" + source + "/" + source + ".yaml";
        return scheduler.schedule(when, () -> runIngest(source, cfg)).asUUID();
    }

    @Job(name = "ingest-%0", retries = 1)
    public void runIngest(String source, String configPath) throws Exception {
        log.info("JobRunr starting ingest source={} config={}", source, configPath);
        Path cfg = Path.of(configPath);
        if (!cfg.isAbsolute()) {
            Path base = Path.of(System.getProperty("user.dir"));
            Path cand = base.resolve(cfg);
            if (Files.exists(cand)) cfg = cand;
            else if (Files.exists(base.resolve("config/" + source + "/" + source + ".yaml"))) cfg = base.resolve("config/" + source + "/" + source + ".yaml");
        }
        Path datalakeRoot = Datalake.defaultLocal().getRoot();
        registry.get(source).runner().run(cfg, datalakeRoot);
        log.info("Ingest done source={}", source);
        // wire into monorepo Floci datalake
        try { flociSync.syncAfterIngest(source); } catch (Exception e) { log.warn("post-ingest sync failed {}", e.toString()); }
    }

    public void delete(UUID jobId) {
        storageProvider.deletePermanently(jobId);
    }
}
