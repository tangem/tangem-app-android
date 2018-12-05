package com.tangem.tangemcard.tasks;

import android.content.Context;
import android.nfc.tech.IsoDep;

import com.tangem.tangemcard.reader.CardProtocol;
import com.tangem.tangemcard.reader.NfcManager;
import com.tangem.tangemcard.data.local.PINStorage;
import com.tangem.tangemcard.data.TangemCard;

public class PurgeTask extends CustomReadCardTask {
    public static final String TAG = PurgeTask.class.getSimpleName();

    public PurgeTask(Context context, TangemCard card, NfcManager nfcManager, IsoDep isoDep, CardProtocol.Notifications notifications) {
        super(context, card, nfcManager, isoDep, notifications);
    }

    @Override
    public void run_Task() throws Exception {
        if (mCard.getPauseBeforePIN2() > 0) {
            mNotifications.onReadWait(mCard.getPauseBeforePIN2());
        }
        protocol.run_PurgeWallet(PINStorage.getPIN2());
        mNotifications.onReadProgress(protocol, 50);
        protocol.run_Read();
        mNotifications.onReadProgress(protocol, 100);

    }
}