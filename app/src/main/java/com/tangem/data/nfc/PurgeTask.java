package com.tangem.data.nfc;

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.util.Log;

import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.NfcManager;
import com.tangem.domain.wallet.PINStorage;
import com.tangem.domain.wallet.TangemCard;

public class PurgeTask extends Thread {
    public static final String TAG = PurgeTask.class.getSimpleName();

    private String txOutAddress;
    private Context mContext;
    private TangemCard mCard;
    private NfcManager mNfcManager;
    private IsoDep mIsoDep;
    private CardProtocol.Notifications mNotifications;
    private boolean isCancelled = false;

    public PurgeTask(Context context, TangemCard card, NfcManager nfcManager, IsoDep isoDep, CardProtocol.Notifications notifications) {
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

        mNotifications.OnReadStart(protocol);
        try {

            // for Samsung's bugs - Workaround for the Samsung Galaxy S5 (since the first connection always hangs on transceive).
            int timeout = mIsoDep.getTimeout();
            mIsoDep.connect();
            mIsoDep.close();
            mIsoDep.connect();
            mIsoDep.setTimeout(timeout);
            try {
                mNotifications.OnReadProgress(protocol, 5);

                Log.i(TAG, "[-- Start purge --]");

                if (isCancelled) return;

                if (mCard.getPauseBeforePIN2() > 0) {
                    mNotifications.OnReadWait(mCard.getPauseBeforePIN2());
                }

//                    try {
                protocol.run_PurgeWallet(PINStorage.getPIN2());
//                    } finally {
//                        mNotifications.OnReadWait(0);
//                    }

                mNotifications.OnReadProgress(protocol, 50);

                protocol.run_Read();

                mNotifications.OnReadProgress(protocol, 100);

                if (isCancelled)
                    return;

            } finally {
                mNfcManager.ignoreTag(mIsoDep.getTag());
            }
        } catch (Exception e) {
            e.printStackTrace();
            protocol.setError(e);

        } finally {
            Log.i(TAG, "[-- Finish purge --]");
            mNotifications.OnReadFinish(protocol);
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
                mNotifications.OnReadCancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}