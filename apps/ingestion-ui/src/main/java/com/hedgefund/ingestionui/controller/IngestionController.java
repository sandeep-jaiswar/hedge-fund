package com.hedgefund.ingestionui.controller;

import com.hedgefund.ingestionui.service.FlociSyncService;
import com.hedgefund.ingestionui.service.IngestionJobService;
import com.hedgefund.ingestionui.service.SourceRegistry;
import java.time.Instant;
import java.util.*;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.StorageProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = {"http://localhost:8080","http://localhost:3000","http://127.0.0.1:8080"}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.OPTIONS})
@RestController
@RequestMapping("/api/ingest")
public class IngestionController {
    private final IngestionJobService jobService;
    private final StorageProvider storageProvider;
    private final FlociSyncService flociSync;
    private final SourceRegistry registry;

    public IngestionController(IngestionJobService jobService, StorageProvider storageProvider, FlociSyncService flociSync, SourceRegistry registry) {
        this.jobService = jobService;
        this.storageProvider = storageProvider;
        this.flociSync = flociSync;
        this.registry = registry;
    }

    @GetMapping("/sources")
    public List<Map<String, Object>> sources() {
        return registry.asControllerMaps();
    }

    @PostMapping("/start/{source}")
    public ResponseEntity<Map<String,Object>> start(@PathVariable String source,
                                                    @RequestParam(required=false) String config,
                                                    @RequestParam(required=false) String when) {
        UUID id;
        if (when != null && !when.isBlank()) {
            id = jobService.schedule(source, config, Instant.parse(when));
        } else {
            id = jobService.enqueue(source, config);
        }
        return ResponseEntity.ok(Map.of("jobId", id.toString(), "source", source, "config", config != null ? config : "config/"+source+"/"+source+".yaml"));
    }

    @PostMapping("/start-all")
    public ResponseEntity<List<Map<String,Object>>> startAll(@RequestParam(required=false) String configSuffix) {
        List<Map<String,Object>> out = new ArrayList<>();
        for (var s : sources()) {
            String id = (String)s.get("id");
            String cfg = configSuffix != null ? "config/"+id+"/"+id+"-"+configSuffix+".yaml" : null;
            UUID jobId = jobService.enqueue(id, cfg);
            out.add(Map.of("source", id, "jobId", jobId.toString()));
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/jobs")
    public Map<String,Object> jobs() {
        try {
            var stats = storageProvider.getJobStats();
            return Map.of(
                "scheduled", stats.getScheduled(),
                "enqueued", stats.getEnqueued(),
                "processing", stats.getProcessing(),
                "succeeded", stats.getSucceeded(),
                "failed", stats.getFailed(),
                "total", stats.getTotal()
            );
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<Map<String,Object>> job(@PathVariable UUID id) {
        try {
            var job = storageProvider.getJobById(id);
            return ResponseEntity.ok(Map.of(
                "id", job.getId().toString(),
                "jobName", job.getJobName(),
                "state", job.getJobState().getName().toString(),
                "createdAt", job.getCreatedAt().toString(),
                "updatedAt", job.getUpdatedAt().toString()
            ));
        } catch (JobNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<Map<String,Object>> cancel(@PathVariable UUID id) {
        try {
            jobService.delete(id);
            return ResponseEntity.ok(Map.of("deleted", id.toString()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public Map<String,Object> health() {
        return Map.of("status","UP","dashboard","http://localhost:8000/dashboard","api","/api/ingest/sources","floci", flociSync.isFlociRunning() ? "http://localhost:4566/_floci/health" : "down");
    }

    @PostMapping("/sync")
    public Map<String,Object> sync() {
        flociSync.syncAfterIngest("all");
        return Map.of("synced","s3://hedge-* via datalake/scripts/sync-all-to-floci.py","floci", flociSync.isFlociRunning());
    }
}
