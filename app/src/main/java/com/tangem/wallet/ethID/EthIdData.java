package com.tangem.wallet.ethID;

import com.tangem.wallet.CoinData;

public class EthIdData extends CoinData {
    private boolean hasApprovalTx = false;
    private long approvalAddressNonce;
    private byte[] CKDpub;

    public boolean isHasApprovalTx() {
        return hasApprovalTx;
    }

    public void setHasApprovalTx(boolean hasApprovalTx) {
        this.hasApprovalTx = hasApprovalTx;
    }

    public long getApprovalAddressNonce() {
        return approvalAddressNonce;
    }

    public void setApprovalAddressNonce(long approvalAddressNonce) {
        this.approvalAddressNonce = approvalAddressNonce;
    }

    public byte[] getCKDpub() {
        return CKDpub;
    }

    public void setCKDpub(byte[] CKDpub) {
        this.CKDpub = CKDpub;
    }
}
