package com.tangem.tangem_card.reader;

import java.io.IOException;

public interface NfcReader {
    byte[] getId();

    void setTimeout(int timeout)throws IOException;

    int getTimeout();

    byte[] transceive(byte[] data) throws IOException;

    void ignoreTag() throws IOException;

    void notifyReadResult(boolean success);

    void connect();

    boolean isConnected();
}