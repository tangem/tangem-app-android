package com.tangem.tangemcard.reader;

public class TLVException extends Exception {

    private static final long serialVersionUID = 1L;

    public TLVException(String message){
        super(message);
    }

    public TLVException(String message, Throwable cause) {
        super(message, cause);
    }

    public TLVException(Throwable cause) {
        super(cause);
    }
}