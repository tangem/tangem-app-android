package com.tangem.tangemcard.tasks;


import com.tangem.tangemcard.data.external.CardDataSubstitutionProvider;
import com.tangem.tangemcard.data.external.PINsProvider;
import com.tangem.tangemcard.reader.CardProtocol;
import com.tangem.tangemcard.data.TangemCard;
import com.tangem.tangemcard.reader.NfcReader;

public class SwapPINTask extends CustomReadCardTask {
    public static final String TAG = SwapPINTask.class.getSimpleName();

    private String newPIN, newPIN2;

    public SwapPINTask(TangemCard card, NfcReader reader, CardDataSubstitutionProvider cardDataSubstitutionProvider, PINsProvider pinsProvider, CardProtocol.Notifications notifications, String newPIN, String newPIN2) {
        super(card, reader, cardDataSubstitutionProvider, pinsProvider, notifications);
        this.newPIN = newPIN;
        this.newPIN2 = newPIN2;
    }

    @Override
    public void run_Task() throws Exception {

        if (mCard.getPauseBeforePIN2() > 0) {
            mNotifications.onReadWait(mCard.getPauseBeforePIN2());
        }

        protocol.run_SetPIN(pinsProvider.getPIN2(), newPIN, newPIN2, false);
        protocol.setPIN(newPIN);
        mCard.setPIN(newPIN);

        mNotifications.onReadProgress(protocol, 50);

        protocol.run_Read();

        mNotifications.onReadProgress(protocol, 100);

    }
    
}