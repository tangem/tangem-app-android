package com.tangem.data.nfc;

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.util.Log;

import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.NfcManager;
import com.tangem.data.db.PINStorage;
import com.tangem.domain.wallet.TangemCard;

public class SwapPINTask extends Thread {
    public static final String TAG = SwapPINTask.class.getSimpleName();

    private Context mContext;
    private TangemCard mCard;
    private NfcManager mNfcManager;
    private String newPIN, newPIN2;
    private IsoDep mIsoDep;
    private CardProtocol.Notifications mNotifications;
    private boolean isCancelled = false;

    public SwapPINTask(Context context, TangemCard card, NfcManager nfcManager, String newPIN, String newPIN2, IsoDep isoDep, CardProtocol.Notifications notifications) {
        this.newPIN = newPIN;
        this.newPIN2 = newPIN2;
        mCard = card;
        mContext = context;
        mNfcManager = nfcManager;
        mIsoDep = isoDep;
        mNotifications = notifications;
    }

    @Override
    public void run() {
        if (mIsoDep == null) {
            return;
        }
        CardProtocol protocol = new CardProtocol(mContext, mIsoDep, mCard, mNotifications);

        mNotifications.onReadStart(protocol);
        try {

            // for Samsung's bugs -
            // Workaround for the Samsung Galaxy S5 (since the
            // first connection always hangs on transceive).
            int timeout = mIsoDep.getTimeout();
            mIsoDep.connect();
            mIsoDep.close();
            mIsoDep.connect();
            mIsoDep.setTimeout(timeout);
            try {

                mNotifications.onReadProgress(protocol, 5);

                Log.i(TAG, "[-- Start swap pin --]");

                if (isCancelled) return;

                if (mCard.getPauseBeforePIN2() > 0) {
                    mNotifications.onReadWait(mCard.getPauseBeforePIN2());
                }

//                    try {
                protocol.run_SwapPIN(PINStorage.getPIN2(), newPIN, newPIN2, false);
                protocol.setPIN(newPIN);
                mCard.setPIN(newPIN);
//                    } finally {
//                        mNotifications.onReadWait(0);
//                    }

                mNotifications.onReadProgress(protocol, 50);

                protocol.run_Read();

                mNotifications.onReadProgress(protocol, 100);

            } finally {
                mNfcManager.ignoreTag(mIsoDep.getTag());
                mNotifications.onReadWait(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            protocol.setError(e);

        } finally {
            Log.i(TAG, "[-- Finish purge --]");
            mNotifications.onReadFinish(protocol);
        }
    }

    public void cancel(Boolean AllowInterrupt) {
        try {
            if (isAlive()) {
                isCancelled = true;
                join(500);
            }
            if (isAlive() && AllowInterrupt) {
                interrupt();
                mNotifications.onReadCancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}