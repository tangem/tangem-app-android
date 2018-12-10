package com.tangem.tangemcard.tasks;


import com.tangem.tangemcard.data.external.CardDataSubstitutionProvider;
import com.tangem.tangemcard.data.external.PINsProvider;
import com.tangem.tangemcard.reader.CardProtocol;
import com.tangem.tangemcard.data.TangemCard;
import com.tangem.tangemcard.reader.NfcReader;
import com.tangem.tangemcard.util.Log;

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