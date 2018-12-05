package com.tangem.tangemcard.tasks;

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.util.Log;

import com.tangem.tangemcard.reader.CardProtocol;
import com.tangem.tangemcard.data.local.Firmwares;
import com.tangem.tangemcard.reader.NfcManager;
import com.tangem.tangemcard.data.TangemCard;

import java.util.Arrays;

/**
 * Created by dvol on 04.02.2018.
 */

public class VerifyCardTask extends CustomReadCardTask {
    public static final String TAG = VerifyCardTask.class.getSimpleName();

    public VerifyCardTask(Context context, TangemCard card, NfcManager nfcManager, IsoDep isoDep, CardProtocol.Notifications notifications) {
        super(context, card, nfcManager, isoDep, notifications);
    }

    @Override
    public void run_Task() throws Exception {
        mNotifications.onReadProgress(protocol, 20);
        if (isCancelled) return;
        protocol.run_VerifyCard();
        mNotifications.onReadProgress(protocol, 50);
        Log.i(TAG, "Manufacturer: " + protocol.getCard().getManufacturer().getOfficialName());
        if (isCancelled) return;
        if (protocol.getCard().getStatus() == TangemCard.Status.Loaded) {
            protocol.run_CheckWalletWithSignatureVerify();
            mNotifications.onReadProgress(protocol, 80);
        }
        if (isCancelled) return;
        Firmwares.VerifyCodeRecord record=Firmwares.selectRandomVerifyCodeBlock(mCard.getFirmwareVersion());
        if (isCancelled) return;
        if( record!=null ) {
            byte[] returnedDigest = protocol.run_VerifyCode(record.hashAlg, record.blockIndex, record.blockCount, record.challenge);
            mCard.setCodeConfirmed(Arrays.equals(returnedDigest, record.digest));
        }else{
            mCard.setCodeConfirmed(null);
        }
        mNotifications.onReadProgress(protocol, 90);

    }

}