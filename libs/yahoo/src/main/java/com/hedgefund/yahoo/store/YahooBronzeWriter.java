package com.hedgefund.yahoo.store;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedgefund.yahoo.model.Bar;
import java.nio.file.*;
import java.util.List;
public class YahooBronzeWriter {
    private final Path bronzeRoot;
    private final ObjectMapper om=new ObjectMapper();
    public YahooBronzeWriter(Path bronzeRoot){ this.bronzeRoot=bronzeRoot; }
    public Path write(String symbol, List<Bar> bars, String rawJson) throws Exception {
        Path dir=bronzeRoot.resolve("symbol="+symbol);
        Files.createDirectories(dir);
        if(rawJson!=null) Files.writeString(dir.resolve("_raw.json"), rawJson);
        StringBuilder sb=new StringBuilder();
        for(Bar b: bars) sb.append(om.writeValueAsString(b)).append("\n");
        Path p=dir.resolve("data.ndjson");
        Path tmp=p.resolveSibling(p.getFileName()+".tmp");
        Files.writeString(tmp, sb.toString());
        Files.move(tmp,p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return p;
    }
}
