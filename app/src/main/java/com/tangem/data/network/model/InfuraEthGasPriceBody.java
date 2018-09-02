package com.tangem.data.network.model;

public class InfuraEthGasPriceBody {
    private String method;
    private String[] params;
    private int id;

    public InfuraEthGasPriceBody(String method, int id) {
        this.method = method;
        this.id = id;
    }
}