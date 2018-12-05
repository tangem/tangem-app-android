package com.tangem.tangemcard.tasks;

import android.content.Context;
import android.nfc.tech.IsoDep;
import android.util.Log;

import com.tangem.tangemcard.reader.CardProtocol;
import com.tangem.tangemcard.reader.NfcManager;
import com.tangem.tangemcard.data.local.PINStorage;
import com.tangem.tangemcard.data.TangemCard;

public class CreateNewWalletTask extends CustomReadCardTask {
    public static final String TAG = CreateNewWalletTask.class.getSimpleName();

    public CreateNewWalletTask(Context context, TangemCard card, NfcManager nfcManager, IsoDep isoDep, CardProtocol.Notifications notifications) {
        super(context, card, nfcManager, isoDep, notifications);
    }

    @Override
    public void run_Task() throws Exception {
        mNotifications.onReadProgress(protocol, 20);
        protocol.run_VerifyCard();

        Log.i(TAG, "Manufacturer: " + protocol.getCard().getManufacturer().getOfficialName());

        mNotifications.onReadProgress(protocol, 30);
        if (isCancelled) return;
        protocol.run_CreateWallet(PINStorage.getPIN2());
        mNotifications.onReadProgress(protocol, 60);
        if (isCancelled) return;

        protocol.run_Read();
        parseReadResult();
    }

}