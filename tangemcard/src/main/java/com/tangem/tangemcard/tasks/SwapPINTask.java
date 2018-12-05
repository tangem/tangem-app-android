package com.tangem.tangemcard.tasks;

import android.content.Context;
import android.nfc.tech.IsoDep;

import com.tangem.tangemcard.reader.CardProtocol;
import com.tangem.tangemcard.reader.NfcManager;
import com.tangem.tangemcard.data.local.PINStorage;
import com.tangem.tangemcard.data.TangemCard;

public class SwapPINTask extends CustomReadCardTask {
    public static final String TAG = SwapPINTask.class.getSimpleName();

    private String newPIN, newPIN2;

    public SwapPINTask(Context context, TangemCard card, NfcManager nfcManager, String newPIN, String newPIN2, IsoDep isoDep, CardProtocol.Notifications notifications) {
        super(context, card, nfcManager, isoDep, notifications);
        this.newPIN = newPIN;
        this.newPIN2 = newPIN2;
    }

    @Override
    public void run_Task() throws Exception {

        if (mCard.getPauseBeforePIN2() > 0) {
            mNotifications.onReadWait(mCard.getPauseBeforePIN2());
        }

        protocol.run_SetPIN(PINStorage.getPIN2(), newPIN, newPIN2, false);
        protocol.setPIN(newPIN);
        mCard.setPIN(newPIN);

        mNotifications.onReadProgress(protocol, 50);

        protocol.run_Read();

        mNotifications.onReadProgress(protocol, 100);

    }
    
}