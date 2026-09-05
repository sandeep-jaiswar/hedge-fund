package com.hedgefund.fred.store;
import java.nio.file.*;
public class FredBronzeWriter {
    private final Path bronzeRoot;
    public FredBronzeWriter(Path bronzeRoot){ this.bronzeRoot=bronzeRoot; }
    public Path write(String key, String raw) throws Exception {
        String safe=key.replaceAll("[^a-zA-Z0-9._-]","_");
        Path dir=bronzeRoot.resolve("key="+safe);
        Files.createDirectories(dir);
        Path p=dir.resolve("data.raw");
        Path tmp=p.resolveSibling(p.getFileName()+".tmp");
        Files.writeString(tmp, raw);
        Files.move(tmp,p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return p;
    }
    public Path writeCsv(String key, String csv) throws Exception {
        String safe=key.replaceAll("[^a-zA-Z0-9._-]","_");
        Path dir=bronzeRoot.resolve("key="+safe);
        Files.createDirectories(dir);
        Path p=dir.resolve("data.csv");
        Path tmp=p.resolveSibling(p.getFileName()+".tmp");
        Files.writeString(tmp, csv);
        Files.move(tmp,p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return p;
    }
}
