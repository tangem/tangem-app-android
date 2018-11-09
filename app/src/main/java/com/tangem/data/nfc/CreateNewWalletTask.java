package com.tangem.data.nfc;

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.util.Log;

import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.NfcManager;
import com.tangem.data.db.PINStorage;
import com.tangem.domain.wallet.TangemCard;

public class CreateNewWalletTask extends Thread {
    public static final String TAG = CreateNewWalletTask.class.getSimpleName();

    private Context mContext;
    private TangemCard mCard;
    private NfcManager mNfcManager;
    private IsoDep mIsoDep;
    private CardProtocol.Notifications mNotifications;
    private boolean isCancelled = false;

    public CreateNewWalletTask(Context context, TangemCard card, NfcManager nfcManager, IsoDep isoDep, CardProtocol.Notifications notifications) {
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

                Log.i(TAG, "[-- Start create new wallet --]");

                if (isCancelled) return;
                protocol.run_VerifyCard();

                Log.i(TAG, "Manufacturer: " + protocol.getCard().getManufacturer().getOfficialName());

                mNotifications.onReadProgress(protocol, 30);
                if (isCancelled) return;

//                    if (mCard.getPauseBeforePIN2() > 0) {
//                        mNotifications.onReadWait(mCard.getPauseBeforePIN2());
//                    }
//                    try {
                protocol.run_CreateWallet(PINStorage.getPIN2());
//                    } finally {
//                        mNotifications.onReadWait(0);
//                    }
                mNotifications.onReadProgress(protocol, 60);
                if (isCancelled) return;

                protocol.run_Read();

            } finally {
                mNfcManager.ignoreTag(mIsoDep.getTag());
            }
        } catch (Exception e) {
            e.printStackTrace();
            protocol.setError(e);

        } finally {
            Log.i(TAG, "[-- Finish create new wallet --]");
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