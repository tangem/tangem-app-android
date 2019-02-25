package com.tangem.tangemcommon.tasks;

import com.tangem.tangemcommon.data.external.PINsProvider;
import com.tangem.tangemcommon.reader.NfcReader;
import com.tangem.tangemcommon.reader.CardProtocol;
import com.tangem.tangemcommon.data.TangemCard;
import com.tangem.tangemcommon.data.external.CardDataSubstitutionProvider;

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