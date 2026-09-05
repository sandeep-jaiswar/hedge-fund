package com.hedgefund.yahoo.store;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedgefund.yahoo.model.Bar;
import java.nio.file.*;
import java.util.*;
public class YahooSilverTransformer {
    private final ObjectMapper om=new ObjectMapper();
    public Path transform(Path bronzeRoot, Path silverPath) throws Exception {
        Files.createDirectories(silverPath);
        Path out=silverPath.resolve("yahoo_ohlcv.csv");
        StringBuilder csv=new StringBuilder();
        csv.append("symbol,date,epoch,open,high,low,close,adj_close,volume\n");
        if(!Files.exists(bronzeRoot)) { Files.writeString(out, csv.toString()); return out; }
        // dedup by symbol|date
        Map<String, Bar> dedup=new LinkedHashMap<>();
        Files.walk(bronzeRoot).filter(p->p.getFileName().toString().equals("data.ndjson")).forEach(p->{
            try{
                for(String line: Files.readAllLines(p)){
                    if(line.isBlank()) continue;
                    Bar b=om.readValue(line, Bar.class);
                    dedup.put(b.symbol()+"|"+b.date(), b);
                }
            }catch(Exception e){ throw new RuntimeException(e); }
        });
        for(Bar b: dedup.values()){
            csv.append(String.format("%s,%s,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%d\n", b.symbol(), b.date(), b.epoch(), b.open(), b.high(), b.low(), b.close(), b.adjClose(), b.volume()));
        }
        Path tmp=out.resolveSibling(out.getFileName()+".tmp");
        Files.writeString(tmp, csv.toString());
        Files.move(tmp,out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return out;
    }
}
