package com.tangem.tangemcard.data.external;

public interface FirmwaresDigestsProvider {
    VerifyCodeRecord selectRandomVerifyCodeBlock(String firmwareVersion);

    class VerifyCodeRecord {
        public String hashAlg;
        public int blockIndex;
        public int blockCount;
        public byte[] challenge;
        public byte[] digest;
    }

}
