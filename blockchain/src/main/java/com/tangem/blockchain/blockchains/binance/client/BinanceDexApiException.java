package com.tangem.blockchain.blockchains.binance.client;


public class BinanceDexApiException extends RuntimeException {
    private static final long serialVersionUID = 3788669840036201041L;
    private BinanceDexApiError error;

    public BinanceDexApiException(BinanceDexApiError error) {
        this.error = error;
    }

    public BinanceDexApiException(Throwable cause) {
        super(cause);
    }

    public BinanceDexApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public BinanceDexApiError getError() {
        return error;
    }

    @Override
    public String getMessage() {
        if (error != null) {
            return error.getMessage();
        }
        return super.getMessage();
    }
}
