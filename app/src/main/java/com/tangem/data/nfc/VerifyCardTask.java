package com.tangem.data.nfc;

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.FW;
import com.tangem.domain.cardReader.NfcManager;
import com.tangem.domain.wallet.PINStorage;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.util.Util;

import java.util.Arrays;

/**
 * Created by dvol on 04.02.2018.
 */

public class VerifyCardTask extends Thread {
    public static final String TAG = VerifyCardTask.class.getSimpleName();

    private IsoDep mIsoDep;
    private CardProtocol.Notifications mNotifications;
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

                    Log.i(TAG, "[-- Start verify card --]");

                    if (isCancelled) return;

                    String PIN = mCard.getPIN();
                    protocol.setPIN(PIN);
                    protocol.run_Read(false);
                    PINStorage.setLastUsedPIN(PIN);
                    mNotifications.OnReadProgress(protocol, 20);
                    if (isCancelled) return;
                    protocol.run_VerifyCard();
                    mNotifications.OnReadProgress(protocol, 50);
                    Log.i(TAG, "Manufacturer: " + protocol.getCard().getManufacturer().getOfficialName());
                    if (isCancelled) return;
                    if (protocol.getCard().getStatus() == TangemCard.Status.Loaded) {
                        protocol.run_CheckWalletWithSignatureVerify();
                        mNotifications.OnReadProgress(protocol, 80);
                    }
                    if (isCancelled) return;
                    FW.VerifyCodeRecord record=FW.selectRandomVerifyCodeBlock(mCard.getFirmwareVersion());
                    if (isCancelled) return;
                    if( record!=null ) {
                        byte[] returnedDigest = protocol.run_VerifyCode(record.hashAlg, record.blockIndex, record.blockCount, record.challenge);
                        mCard.setCodeConfirmed(Arrays.equals(returnedDigest, record.digest));
                    }else{
                        mCard.setCodeConfirmed(null);
                    }
                    mNotifications.OnReadProgress(protocol, 90);
                } catch (Exception e) {
                    e.printStackTrace();
                    protocol.setError(e);

                } finally {
                    Log.i(TAG, "[-- Finish verify card --]");
                    mNotifications.OnReadFinish(protocol);
                }
            } finally {
                mNfcManager.ignoreTag(mIsoDep.getTag());
            }
        } catch (Exception e) {
            e.printStackTrace();
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