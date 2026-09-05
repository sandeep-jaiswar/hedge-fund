package com.hedgefund.worldbank.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Flattens bronze NDJSON to silver CSV (typed, deduped). Later can COPY to parquet via DuckDB. */
public class SilverTransformer {
    private static final Logger log = LoggerFactory.getLogger(SilverTransformer.class);
    private final Path bronzeRoot;
    private final Path silverRoot;

    public SilverTransformer(Path datalakeRoot, String bronzeRel, String silverRel){
        this.bronzeRoot = datalakeRoot.resolve(bronzeRel);
        this.silverRoot = datalakeRoot.resolve(silverRel);
    }

    public void transform() throws IOException {
        if(!Files.exists(bronzeRoot)){ log.warn("No bronze at {}", bronzeRoot); return; }
        // walk bronze ndjson files
        List<Path> ndjsons = Files.walk(bronzeRoot).filter(p->p.getFileName().toString().equals("data.ndjson")).toList();
        if(ndjsons.isEmpty()){ log.warn("No data.ndjson under {}", bronzeRoot); return; }
        // aggregate rows
        // dedup key indicator|country_iso3|date
        Map<String,String[]> dedup = new LinkedHashMap<>();
        for(Path nd: ndjsons){
            try(BufferedReader br = Files.newBufferedReader(nd)){
                String line;
                while((line=br.readLine())!=null){
                    if(line.isBlank()) continue;
                    // crude parse via Jackson would be better, but keep simple for now
                    // use simple extraction
                    Map<String,String> m = parseFlat(line);
                    String key = m.get("indicator_id")+"|"+m.get("country_iso3")+"|"+m.get("date");
                    // keep last (latest ingested wins)
                    dedup.put(key, toRow(m));
                }
            }
        }
        Path outDir = silverRoot.resolve("worldbank_observations");
        Files.createDirectories(outDir);
        Path csv = outDir.resolve("observations.csv");
        Path tmp = outDir.resolve("observations.csv.tmp");
        try(BufferedWriter w = Files.newBufferedWriter(tmp)){
            w.write("indicator_id,indicator_name,country_id,country_name,country_iso3,date,year,period_type,value,unit,obs_status,decimal,source_id,lastupdated,is_valid");
            w.newLine();
            for(String[] row: dedup.values()){
                w.write(String.join(",", row));
                w.newLine();
            }
        }
        Files.move(tmp, csv, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        log.info("Silver wrote {} rows to {}", dedup.size(), csv);
    }

    private String[] toRow(Map<String,String> m){
        String indicator_id = esc(m.get("indicator_id"));
        String indicator_name = esc(m.get("indicator_name"));
        String country_id = esc(m.get("country_id"));
        String country_name = esc(m.get("country_name"));
        String iso3 = esc(m.get("country_iso3"));
        String date = esc(m.get("date"));
        String year = date.replaceAll("[^0-9]","").length()>=4 ? date.replaceAll("[^0-9]","").substring(0,4) : date;
        String period_type = date.contains("M")?"M": date.contains("Q")?"Q":"Y";
        String value = m.get("value"); if(value==null||value.equals("null")) value="";
        String unit = esc(m.get("unit"));
        String obs = esc(m.get("obs_status"));
        String dec = m.get("decimal"); if(dec==null||dec.equals("null")) dec="";
        String src = esc(m.get("source_id"));
        String lu = esc(m.get("lastupdated"));
        String is_valid = value.isEmpty()?"false":"true";
        return new String[]{indicator_id,indicator_name,country_id,country_name,iso3,date,year,period_type,value,unit,obs,dec,src,lu,is_valid};
    }

    private String esc(String s){ if(s==null) return ""; return "\""+s.replace("\"","\"\"")+"\""; }

    // very small parser for our known flat structure
    private Map<String,String> parseFlat(String json){
        Map<String,String> m=new HashMap<>();
        // manual extraction fallback
        m.put("indicator_id", extract(json, "\"indicator\":{\"id\":\"", "\""));
        m.put("indicator_name", extract(json, "\"value\":\"", "\"", 1)); // second value is indicator name; approximate
        // better: extract via regex
        // For correctness, use Jackson
        try{
            com.fasterxml.jackson.databind.ObjectMapper om2=new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode n=om2.readTree(json);
            m.put("indicator_id", n.path("indicator").path("id").asText(""));
            m.put("indicator_name", n.path("indicator").path("value").asText(""));
            m.put("country_id", n.path("country").path("id").asText(""));
            m.put("country_name", n.path("country").path("value").asText(""));
            m.put("country_iso3", n.path("countryiso3code").asText(""));
            m.put("date", n.path("date").asText(""));
            com.fasterxml.jackson.databind.JsonNode v=n.get("value");
            m.put("value", v==null||v.isNull()?null: v.asText());
            m.put("unit", n.path("unit").asText(""));
            m.put("obs_status", n.path("obs_status").asText(""));
            com.fasterxml.jackson.databind.JsonNode d=n.get("decimal");
            m.put("decimal", d==null||d.isNull()?null: d.asText());
            m.put("source_id", n.path("_sourceid").asText(""));
            m.put("lastupdated", n.path("_lastupdated").asText(""));
        }catch(Exception e){ }
        return m;
    }
    private String extract(String s,String start,String end){ int a=s.indexOf(start); if(a<0) return ""; a+=start.length(); int b=s.indexOf(end,a); if(b<0) return ""; return s.substring(a,b); }
    private String extract(String s,String start,String end,int nth){ int idx=0; for(int i=0;i<nth;i++){ idx=s.indexOf(start, idx); if(idx<0) return ""; idx+=start.length(); } int b=s.indexOf(end, idx); if(b<0) return ""; return s.substring(idx,b); }
}
