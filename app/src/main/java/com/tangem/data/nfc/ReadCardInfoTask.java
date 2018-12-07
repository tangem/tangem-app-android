package com.tangem.data.nfc;

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.util.Log;

import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.NfcManager;
import com.tangem.data.db.PINStorage;
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

    // this fields are static to optimize process when need enter pin and scan card again
    private static ArrayList<String> lastRead_UnsuccessfullPINs = new ArrayList<>();
    private static TangemCard.EncryptionMode lastRead_Encryption = null;
    private static String lastRead_UID;

    public static void resetLastReadInfo() {
        lastRead_UID = "";
        lastRead_Encryption = null;
        lastRead_UnsuccessfullPINs.clear();
    }

    public ReadCardInfoTask(Context context, NfcManager nfcManager, IsoDep isoDep, CardProtocol.Notifications notifications) {
        mContext = context;
        mIsoDep = isoDep;
        mNotifications = notifications;
        mNfcManager = nfcManager;
//        ReadCardInfoTask.lastRead_UID = lastRead_UID;
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
                mNotifications.onReadStart(protocol);
                try {
                    mNotifications.onReadProgress(protocol, 5);

                    byte[] UID = mIsoDep.getTag().getId();
                    String sUID = Util.byteArrayToHexString(UID);
                    if (!lastRead_UID.equals(sUID)) {
                        resetLastReadInfo();
                    }
