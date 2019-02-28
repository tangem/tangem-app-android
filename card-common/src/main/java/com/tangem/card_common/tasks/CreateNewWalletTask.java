package com.tangem.card_common.tasks;

import com.tangem.card_common.data.external.CardDataSubstitutionProvider;
import com.tangem.card_common.data.external.PINsProvider;
import com.tangem.card_common.reader.CardProtocol;
import com.tangem.card_common.data.TangemCard;
import com.tangem.card_common.reader.NfcReader;
import com.tangem.card_common.util.Log;

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