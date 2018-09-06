package com.tangem.data.network.model;

public class InfuraBody {
    private String method;
    private String[] params;
    private int id;

    public InfuraBody() {
    }

    public InfuraBody(String method, int id) {
        this.method = method;
        this.id = id;
    }

    public InfuraBody(String method, String[] params, int id) {
        this.method = method;
        this.params = params;
        this.id = id;
    }
}