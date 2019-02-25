package com.tangem.tangemcommon.tasks;

import com.tangem.tangemcommon.data.external.CardDataSubstitutionProvider;
import com.tangem.tangemcommon.data.external.PINsProvider;
import com.tangem.tangemcommon.reader.CardProtocol;
import com.tangem.tangemcommon.data.TangemCard;
import com.tangem.tangemcommon.reader.NfcReader;
import com.tangem.tangemcommon.util.Log;

public class CreateNewWalletTask extends CustomReadCardTask {
    public static final String TAG = CreateNewWalletTask.class.getSimpleName();

    public CreateNewWalletTask(TangemCard card, NfcReader reader, CardDataSubstitutionProvider cardDataSubstitutionProvider, PINsProvider pinsProvider, CardProtocol.Notifications notifications) {
        super(card, reader, cardDataSubstitutionProvider, pinsProvider, notifications);
    }

    @Override
    public void run_Task() throws Exception {
        mNotifications.onReadProgress(protocol, 20);
        protocol.run_VerifyCard();

        Log.i(TAG, "Manufacturer: " + protocol.getCard().getManufacturer().getOfficialName());

        mNotifications.onReadProgress(protocol, 30);
        if (isCancelled) return;
        protocol.run_CreateWallet(pinsProvider.getPIN2());
        mNotifications.onReadProgress(protocol, 60);
        if (isCancelled) return;

        protocol.run_Read();
        parseReadResult();
    }

}