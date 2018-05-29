package com.tangem.data.network.task;

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.util.Log;

import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.NfcManager;
import com.tangem.domain.wallet.PINStorage;
import com.tangem.domain.wallet.TangemCard;

/**
 * Created by dvol on 04.02.2018.
 */

public class VerifyCardTask extends Thread {

    IsoDep mIsoDep;
    CardProtocol.Notifications mNotifications;
    private final String logTag = "VerifyCardTask";
    private boolean isCancelled = false;
    private Context mContext;
    private TangemCard mCard;
    private NfcManager mNfcManager;

    public VerifyCardTask(Context context, TangemCard card, NfcManager nfcManager, IsoDep isoDep, CardProtocol.Notifications notifications) {
        mCard = card;
        mContext = context;
        mIsoDep = isoDep;
        mNotifications = notifications;
        mNfcManager = nfcManager;
    }

    @Override
    public void run() {
        if (mIsoDep == null) {
            return;
        }
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
                CardProtocol protocol = new CardProtocol(mContext, mIsoDep, mCard, mNotifications);
                mNotifications.OnReadStart(protocol);
                try {
                    mNotifications.OnReadProgress(protocol, 5);

                    Log.i("VerifyCardTask", "[-- Start verify card --]");

                    if (isCancelled) return;

                    String PIN = mCard.getPIN();
                    protocol.setPIN(PIN);
                    protocol.run_Read();
                    PINStorage.setLastUsedPIN(PIN);
                    mNotifications.OnReadProgress(protocol, 30);
                    if (isCancelled) return;
                    protocol.run_VerifyCard();
                    mNotifications.OnReadProgress(protocol, 60);
                    Log.i("VerifyCardTask", "Manufacturer: " + protocol.getCard().getManufacturer().getOfficialName());
                    if (isCancelled) return;
                    if (protocol.getCard().getStatus() == TangemCard.Status.Loaded) {
                        protocol.run_CheckWalletWithSignatureVerify();
                        mNotifications.OnReadProgress(protocol, 90);
                    }



//                        if (isCancelled) return;
//                    if (protocol.getCard().getStatus() == TangemCard.Status.Loaded) {
//                        protocol.run_CheckWithSignatureVerify();
//                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    protocol.setError(e);

                } finally {
                    Log.i("VerifyCardTask", "[-- Finish verify card --]");
                    mNotifications.OnReadFinish(protocol);
                }
            } finally {
                mNfcManager.IgnoreTag(mIsoDep.getTag());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void cancel(Boolean AllowInterrupt) {
        try {
            if (this.isAlive()) {
                isCancelled = true;
                join(500);
            }
            if (this.isAlive() && AllowInterrupt) {
                interrupt();
                mNotifications.OnReadCancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
