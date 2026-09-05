package com.hedgefund.eia.store;
import java.nio.file.*;
import java.util.stream.*;
public class EiaSilverTransformer {
    public Path transform(Path bronzeRoot, Path silverPath, String fileName) throws Exception {
        Files.createDirectories(silverPath);
        Path out=silverPath.resolve(fileName);
        StringBuilder sb=new StringBuilder();
        // header depends on source, use generic key,raw
        sb.append("source_key,raw_len,bronze_path\n");
        if(Files.exists(bronzeRoot)){
            try(Stream<Path> s=Files.walk(bronzeRoot)){
                s.filter(p->p.getFileName().toString().startsWith("data.")).forEach(p->{
                    try{
                        String key=p.getParent().getFileName().toString().replace("key=","");
                        long len=Files.size(p);
                        sb.append(key).append(",").append(len).append(",").append(p.toString().replace(",","_")).append("\n");
                    }catch(Exception e){ throw new RuntimeException(e); }
                });
            }
        }
        Path tmp=out.resolveSibling(out.getFileName()+".tmp");
        Files.writeString(tmp, sb.toString());
        Files.move(tmp,out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return out;
    }
}
