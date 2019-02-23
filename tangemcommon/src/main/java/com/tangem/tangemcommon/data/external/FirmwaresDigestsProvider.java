package com.tangem.tangemcommon.data.external;

/**
 * This interfaces provide function to randomly select parameters to run one VerifyCode command, check answer and
 * state that card is genuine or not
 */
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
