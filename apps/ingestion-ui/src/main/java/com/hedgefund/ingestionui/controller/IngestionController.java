package com.hedgefund.ingestionui.controller;

import com.hedgefund.ingestionui.service.IngestionJobService;
import java.time.Instant;
import java.util.*;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.StorageProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.OPTIONS})
@RestController
@RequestMapping("/api/ingest")
public class IngestionController {
    private final IngestionJobService jobService;
    private final StorageProvider storageProvider;

    public IngestionController(IngestionJobService jobService, StorageProvider storageProvider) {
        this.jobService = jobService;
        this.storageProvider = storageProvider;
    }

    @GetMapping("/sources")
    public List<Map<String, Object>> sources() {
        return List.of(
            Map.of("id","worldbank","name","World Bank WDI","config","config/worldbank/worldbank.yaml"),
            Map.of("id","yahoo","name","Yahoo Finance","config","config/yahoo/yahoo.yaml"),
            Map.of("id","cboe","name","CBOE (VIX)","config","config/cboe/cboe.yaml"),
            Map.of("id","binance","name","Binance","config","config/binance/binance.yaml"),
            Map.of("id","coinbase","name","Coinbase","config","config/coinbase/coinbase.yaml"),
            Map.of("id","defillama","name","DefiLlama","config","config/defillama/defillama.yaml"),
            Map.of("id","tencent","name","Tencent","config","config/tencent/tencent.yaml"),
            Map.of("id","sina","name","Sina","config","config/sina/sina.yaml"),
            Map.of("id","eastmoney","name","EastMoney","config","config/eastmoney/eastmoney.yaml"),
            Map.of("id","baostock","name","Baostock","config","config/baostock/baostock.yaml"),
            Map.of("id","investing","name","Investing","config","config/investing/investing.yaml"),
            Map.of("id","fred","name","FRED","config","config/fred/fred.yaml"),
            Map.of("id","treasury","name","Treasury","config","config/treasury/treasury.yaml"),
            Map.of("id","sec","name","SEC EDGAR","config","config/sec/sec.yaml"),
            Map.of("id","imf","name","IMF","config","config/imf/imf.yaml"),
            Map.of("id","oecd","name","OECD","config","config/oecd/oecd.yaml"),
            Map.of("id","calcfi","name","CalcFi","config","config/calcfi/calcfi.yaml"),
            Map.of("id","fdic","name","FDIC","config","config/fdic/fdic.yaml"),
            Map.of("id","eia","name","EIA","config","config/eia/eia.yaml"),
            Map.of("id","bls","name","BLS","config","config/bls/bls.yaml"),
            Map.of("id","bea","name","BEA","config","config/bea/bea.yaml"),
            Map.of("id","gmd","name","GMD","config","config/gmd/gmd.yaml")
        );
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
        return Map.of("status","UP","dashboard","http://localhost:8000/dashboard","api","/api/ingest/sources");
    }
}
