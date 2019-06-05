package com.tangem.data.network.model;

public class InfuraBody {
    private String method;
    private Object[] params;
    private int id;
    private String jsonrpc = "2.0";

    public InfuraBody() {
    }

    // body for eth gasPrice
    public InfuraBody(String method, int id) {
        this.method = method;
        this.id = id;
    }

    // body for eth getBalance, eth getTransactionCount, eth sendRawTransaction
    public InfuraBody(String method, String[] params, int id) {
        this.method = method;
        this.params = params;
        this.id = id;
    }

    // body for eth call
    public InfuraBody(String method, Object[] params, int id) {
        this.method = method;
        this.params = params;
        this.id = id;
    }

    public static class EthCallParams {
        private String data;
        private String to;

        public EthCallParams(String data, String to) {
            this.data = data;
            this.to = to;
        }
    }

}