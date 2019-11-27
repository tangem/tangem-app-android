package com.tangem.tangem_card.tasks;

import com.tangem.tangem_card.data.TangemCard;
import com.tangem.tangem_card.data.external.CardDataSubstitutionProvider;
import com.tangem.tangem_card.data.external.PINsProvider;
import com.tangem.tangem_card.reader.CardProtocol;
import com.tangem.tangem_card.reader.NfcReader;
import com.tangem.tangem_card.util.Log;

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