package com.tangem.tangem_card.tasks;

import com.tangem.tangem_card.data.TangemCard;
import com.tangem.tangem_card.data.external.CardDataSubstitutionProvider;
import com.tangem.tangem_card.data.external.PINsProvider;
import com.tangem.tangem_card.reader.CardProtocol;
import com.tangem.tangem_card.reader.NfcReader;

/**
 * Created by dvol on 19.01.2020.
 */

public class WriteIDCardTask extends CustomReadCardTask {
    public static final String TAG = WriteIDCardTask.class.getSimpleName();

    private TangemCard.IDCardData idCardData;
    private byte[] issuerPrivateDataKey;
    public WriteIDCardTask(TangemCard card, NfcReader reader, CardDataSubstitutionProvider cardDataSubstitutionProvider, PINsProvider pinsProvider, TangemCard.IDCardData idCardData, byte[] issuerPrivateDataKey, CardProtocol.Notifications notifications) {
        super(card, reader, cardDataSubstitutionProvider, pinsProvider, notifications);
        this.idCardData=idCardData;
        this.issuerPrivateDataKey=issuerPrivateDataKey;
    }

    @Override
    public void run_Task() throws Exception {
        mNotifications.onReadProgress(protocol, 0);
        if (isCancelled) return;

        if( !protocol.getCard().isIDCard() )
            throw new Exception("It's not an IDCard!");

        protocol.run_WriteIssuerDataEx(mNotifications,idCardData.toTLVList().toBytes(),issuerPrivateDataKey);
        mNotifications.onReadProgress(protocol, 100);
    }

}