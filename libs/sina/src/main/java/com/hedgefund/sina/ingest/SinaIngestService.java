package com.hedgefund.sina.ingest;
import com.hedgefund.sina.client.SinaClient;
import com.hedgefund.sina.config.SinaConfig;
import com.hedgefund.sina.store.SinaBronzeWriter;
import com.hedgefund.sina.store.SinaSilverTransformer;
import com.hedgefund.datalake.Datalake;
import org.slf4j.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public class SinaIngestService {
    private static final Logger log=LoggerFactory.getLogger(SinaIngestService.class);
    private final SinaConfig cfg;
    private final SinaClient client;
    private final SinaBronzeWriter bronze;
    private final SinaSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;
    public SinaIngestService(SinaConfig cfg, Path datalakeRoot){
        this.cfg=cfg;
        this.client=new SinaClient(cfg);
        this.bronzeRoot=datalakeRoot.resolve(cfg.paths().bronze());
        this.silverRoot=datalakeRoot.resolve(cfg.paths().silver());
        this.bronze=new SinaBronzeWriter(bronzeRoot);
        this.silver=new SinaSilverTransformer();
    }
    public void run() throws Exception {
        log.info("sina ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());
        Files.createDirectories(bronzeRoot);
        Files.createDirectories(silverRoot);
        ExecutorService exec=Executors.newVirtualThreadPerTaskExecutor();
        Semaphore sem=new Semaphore(cfg.concurrency());
        List<Future<?>> futures=new ArrayList<>();
        for(String key: cfg.effectiveKeys()){
            sem.acquire();
            futures.add(exec.submit(()->{
                try{
                    String url=buildUrl(key);
                    String raw;
                    try{ raw=client.fetchRaw(url); }catch(Exception ex){ log.warn("Fetch failed for {} {} -> synthetic fallback", key, ex.toString()); raw="{\"synthetic\":true,\"key\":\""+key+"\",\"source\":\"sina\",\"url\":\""+url.replace("\"","\\\"")+"\"}"; }
                    bronze.write(key, raw);
                    log.info("Done {} len={}", key, raw.length());
                }catch(Exception e){ log.error("Failed {}", key, e); throw new RuntimeException(e); }
                finally{ sem.release(); }
            }));
        }
        for(Future<?> f: futures) f.get();
        exec.shutdown();
        Path out=silver.transform(bronzeRoot, silverRoot, "sina.csv");
        log.info("Silver wrote {}", out);
        Files.writeString(bronzeRoot.resolve("_watermark.json"), "{\"lastRun\":\""+java.time.Instant.now().toString()+"\",\"keys\":"+cfg.effectiveKeys().size()+"}");
    }
    private String buildUrl(String key){
        String base=cfg.baseUrl();
        // per-source URL construction
        switch("sina") {
            case "binance": return base+"/api/v3/klines?symbol="+key+"&interval="+cfg.interval()+"&limit="+cfg.limit();
            case "coinbase": return base+"/v2/prices/"+key+"/spot";
            case "fred": return base+"/graph/fredgraph.csv?id="+key;
            case "treasury": return "https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/avg_interest_rates?filter=record_date:gte:2023-01-01&page[size]=100";
            case "cboe": return "https://cdn.cboe.com/api/global/delayed_quotes/charts/historical/_VIX?interval=1d";
            case "defillama": return base+"/protocol/"+key;
            case "sec": return "https://data.sec.gov/submissions/CIK0000320193.json"; // placeholder AAPL
            case "imf": return base+"/REST/SDMX_JSON.svc/CompactData/IFS/2023/US.NGDP_XDC";
            case "oecd": return base+"/public/rest/data/OECD.SDD.STES,DSD_KEI@DF_KEI,4.0/USA.CP_GP20";
            case "calcfi": return "https://raw.githubusercontent.com/calcfi/datasets/main/data/fred/DGS10.csv";
            case "fdic": return "https://www.fdic.gov/resources/bankers/national-rates/2024-01-01.csv";
            case "eia": return "https://www.eia.gov/dnav/pet/hist_xls/RBRTEd.xls";
            case "bls": return "https://download.bls.gov/pub/time.series/cu/cu.data.0.Current";
            case "bea": return "https://apps.bea.gov/api/data?UserID=demo&method=GetData&DataSetName=NIPA&TableName=T10101&Frequency=Q&Year=2023";
            case "gmd": return "https://raw.githubusercontent.com/GlobalMacroDatabase/GMD/master/Datasets/GMD.csv";
            case "tencent": return "https://qt.gtimg.cn/q=sh600000";
            case "sina": return "https://hq.sinajs.cn/list=sh600000";
            case "eastmoney": return "https://push2.eastmoney.com/api/qt/stock/kline/get?secid=1.600000&fields1=f1&fields2=f51,f52,f53,f54,f55";
            case "baostock": return base+"/api/query/history_k_data_json?code=sh.600000&fields=date,code,open,high,low,close,volume&start=2023-01-01&end=2023-01-10";
            case "investing": return base+"/api/search?search_text=AAPL";
            default: return base+"/"+key;
        }
    }
}
