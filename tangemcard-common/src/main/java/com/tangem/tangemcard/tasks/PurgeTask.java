package com.tangem.tangemcard.tasks;

import com.tangem.tangemcard.data.external.PINsProvider;
import com.tangem.tangemcard.reader.NfcReader;
import com.tangem.tangemcard.reader.CardProtocol;
import com.tangem.tangemcard.data.TangemCard;
import com.tangem.tangemcard.data.external.CardDataSubstitutionProvider;

public class PurgeTask extends CustomReadCardTask {
    public static final String TAG = PurgeTask.class.getSimpleName();

    public PurgeTask(TangemCard card, NfcReader reader, CardDataSubstitutionProvider cardDataSubstitutionProvider, PINsProvider pinsProvider, CardProtocol.Notifications notifications) {
        super(card, reader, cardDataSubstitutionProvider, pinsProvider, notifications);
    }

    @Override
    public void run_Task() throws Exception {
        if (mCard.getPauseBeforePIN2() > 0) {
            mNotifications.onReadWait(mCard.getPauseBeforePIN2());
        }
        protocol.run_PurgeWallet(pinsProvider.getPIN2());
        mNotifications.onReadProgress(protocol, 50);
        protocol.run_Read();
        mNotifications.onReadProgress(protocol, 100);

    }
}