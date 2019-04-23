package com.tangem.wallet.binance.client;

import com.tangem.wallet.binance.client.impl.BinanceDexApiAsyncRestClientImpl;
import com.tangem.wallet.binance.client.impl.BinanceDexApiRestClientImpl;

public class BinanceDexApiClientFactory {
    private BinanceDexApiClientFactory() {
    }

    public static BinanceDexApiClientFactory newInstance() {
        return new BinanceDexApiClientFactory();
    }

    public BinanceDexApiRestClient newRestClient() {
        return newRestClient(BinanceDexEnvironment.PROD.getBaseUrl());
    }

    public BinanceDexApiRestClient newRestClient(String baseUrl) {
        return new BinanceDexApiRestClientImpl(baseUrl);
    }

    public BinanceDexApiAsyncRestClient newAsyncRestClient() {
        return newAsyncRestClient(BinanceDexEnvironment.PROD.getBaseUrl());
    }

    public BinanceDexApiAsyncRestClient newAsyncRestClient(String baseUrl) {
        return new BinanceDexApiAsyncRestClientImpl(baseUrl);
    }

}
