package com.tangem.wallet.ethID;

import android.os.Bundle;
import android.util.Log;

import com.tangem.wallet.CoinData;

public class EthIdData extends CoinData {
    private boolean hasApprovalTx = false;
    private Long approvalAddressNonce;
    private byte[] CKDpub = null;

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

    @Override
    public void clearInfo() {
        super.clearInfo();
        hasApprovalTx = false;
        approvalAddressNonce = null;
    }

    @Override
    public void loadFromBundle(Bundle B) {
        super.loadFromBundle(B);

        if (B.containsKey("ApprovalAddressNonce")) {
            approvalAddressNonce = B.getLong("ApprovalAddressNonce");
        }
        if (B.containsKey("CKDpub")) CKDpub = B.getByteArray("CKDpub");
        hasApprovalTx = B.getBoolean("HasApprovalTx");
    }

    @Override
    public void saveToBundle(Bundle B) {
        super.saveToBundle(B);
        try {
            if (approvalAddressNonce != null) B.putLong("ApprovalAddressNonce", approvalAddressNonce);
            if (CKDpub != null) B.putByteArray("CKDpub", CKDpub);
            B.putBoolean("HasApprovalTx", hasApprovalTx);

        } catch (Exception e) {
            Log.e("Can't save to bundle ", e.getMessage());
        }

    }
}
