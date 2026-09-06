package com.hedgefund.ingestionui.model;

public enum SourceId {
    worldbank, yahoo, cboe, investing, baostock, eastmoney, sina, tencent,
    gmd, bea, bls, eia, fdic, calcfi, oecd, imf, sec, treasury, fred,
    defillama, coinbase, binance;

    public static SourceId from(String s) {
        return SourceId.valueOf(s.toLowerCase());
    }
}
