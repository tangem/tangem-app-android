package com.tangem.data.nfc;

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.util.Log;

import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.NfcManager;
import com.tangem.domain.wallet.PINStorage;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.util.Util;

import java.util.ArrayList;

public class ReadCardInfoTask extends Thread {
    public static final String TAG = ReadCardInfoTask.class.getSimpleName();

    private IsoDep mIsoDep;
    private CardProtocol.Notifications mNotifications;
    private boolean isCancelled = false;
    private Context mContext;
    private NfcManager mNfcManager;

    private ArrayList<String> lastRead_UnsuccessfullPINs = new ArrayList<>();
    private TangemCard.EncryptionMode lastRead_Encryption = null;
    private String lastRead_UID;

    public ReadCardInfoTask(Context context, NfcManager nfcManager, String lastRead_UID, IsoDep isoDep, CardProtocol.Notifications notifications) {
        mContext = context;
        mIsoDep = isoDep;
        mNotifications = notifications;
        mNfcManager = nfcManager;
        this.lastRead_UID = lastRead_UID;
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
                CardProtocol protocol = new CardProtocol(mContext, mIsoDep, mNotifications);
                mNotifications.OnReadStart(protocol);
                try {
                    mNotifications.OnReadProgress(protocol, 5);

                    byte[] UID = mIsoDep.getTag().getId();
                    String sUID = Util.byteArrayToHexString(UID);
                    if (!lastRead_UID.equals(sUID)) {
                        lastRead_UID = sUID;
                        lastRead_UnsuccessfullPINs.clear();
                        lastRead_Encryption = null;
                    }

                    Log.i(TAG, "[-- Start read card info --]");

                    if (isCancelled) return;

                    protocol.setPIN(PINStorage.getDefaultPIN());
                    protocol.clearReadResult();

                    if (lastRead_Encryption == null) {
                        Log.i(TAG, "Try get supported encryption mode");
                        protocol.run_GetSupportedEncryption();
                    } else {
                        Log.i(TAG, "Use already defined encryption mode: " + lastRead_Encryption.name());
                        protocol.getCard().encryptionMode = lastRead_Encryption;
                    }

                    if (protocol.haveReadResult()) {
                        //already have read result (obtained while get supported encryption), only read issuer data and define offline balance
                        protocol.parseReadResult();
                        protocol.run_ReadWriteIssuerData();
                        mNotifications.OnReadProgress(protocol, 60);
                        PINStorage.setLastUsedPIN(protocol.getCard().getPIN());
                    } else {
                        //don't have read result - may be don't get supported encryption on this try, need encryption or need another PIN
                        if (lastRead_Encryption == null) {
                            // we try get supported encryption on this time
                            lastRead_Encryption = protocol.getCard().encryptionMode;
                            if (protocol.getCard().encryptionMode == TangemCard.EncryptionMode.None) {
                                // default pin not accepted
                                lastRead_UnsuccessfullPINs.add(PINStorage.getDefaultPIN());
                            }
                        }

                        boolean pinFound = false;
                        for (String PIN : PINStorage.getPINs()) {
                            Log.e(TAG, "PIN: " + PIN);

                            boolean skipPin = false;
                            for (int i = 0; i < lastRead_UnsuccessfullPINs.size(); i++) {
                                if (lastRead_UnsuccessfullPINs.get(i).equals(PIN)) {
                                    skipPin = true;
                                    break;
                                }
                            }

                            if (skipPin) {
                                Log.e(TAG, "Skip PIN - already checked before");
                                continue;
                            }

                            try {
                                protocol.setPIN(PIN);
                                if (protocol.getCard().encryptionMode != TangemCard.EncryptionMode.None) {
                                    protocol.CreateProtocolKey();
                                }
                                protocol.run_Read();
                                mNotifications.OnReadProgress(protocol, 60);
                                PINStorage.setLastUsedPIN(PIN);
                                pinFound = true;
                                protocol.getCard().setPIN(PIN);
                                break;
                            } catch (CardProtocol.TangemException_InvalidPIN e) {
                                Log.e(TAG, e.getMessage());
                                lastRead_UnsuccessfullPINs.add(PIN);
                            }
                        }
                        if (!pinFound) {
                            throw new CardProtocol.TangemException_InvalidPIN("No valid PIN found!");
                        }
                    }

                    protocol.run_CheckPIN2isDefault();

                } catch (Exception e) {
                    e.printStackTrace();
                    protocol.setError(e);

                } finally {
                    Log.i(TAG, "[-- Finish read card info --]");
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