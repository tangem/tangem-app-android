package com.tangem.tangem_card.tasks;

import com.tangem.tangem_card.data.TangemCard;
import com.tangem.tangem_card.data.external.CardDataSubstitutionProvider;
import com.tangem.tangem_card.data.external.FirmwaresDigestsProvider;
import com.tangem.tangem_card.data.external.PINsProvider;
import com.tangem.tangem_card.reader.CardProtocol;
import com.tangem.tangem_card.reader.NfcReader;
import com.tangem.tangem_card.util.Log;

import java.util.Arrays;

/**
 * Created by dvol on 04.02.2018.
 */

public class VerifyCardTask extends CustomReadCardTask {
    public static final String TAG = VerifyCardTask.class.getSimpleName();

    private FirmwaresDigestsProvider firmwaresDigestsProvider;
    public VerifyCardTask(TangemCard card, NfcReader reader, CardDataSubstitutionProvider cardDataSubstitutionProvider, PINsProvider pinsProvider, FirmwaresDigestsProvider firmwaresDigestsProvider, CardProtocol.Notifications notifications) {
        super(card, reader, cardDataSubstitutionProvider, pinsProvider, notifications);
        this.firmwaresDigestsProvider=firmwaresDigestsProvider;
    }

    @Override
    public void run_Task() throws Exception {
        mNotifications.onReadProgress(protocol, 20);
        if (isCancelled) return;
        protocol.run_VerifyCard();
        mNotifications.onReadProgress(protocol, 50);
        Log.i(TAG, "Manufacturer: " + protocol.getCard().getManufacturer().getOfficialName());
        if (isCancelled) return;
        if (protocol.getCard().getStatus() == TangemCard.Status.Loaded) {
            protocol.run_CheckWalletWithSignatureVerify();
            mNotifications.onReadProgress(protocol, 80);
        }
        if (isCancelled) return;
        FirmwaresDigestsProvider.VerifyCodeRecord record = firmwaresDigestsProvider.selectRandomVerifyCodeBlock(mCard.getFirmwareVersion());
        if (isCancelled) return;
        if (record != null) {
            byte[] returnedDigest = protocol.run_VerifyCode(record.hashAlg, record.blockIndex, record.blockCount, record.challenge);
            mCard.setCodeConfirmed(Arrays.equals(returnedDigest, record.digest));
        } else {
            mCard.setCodeConfirmed(null);
        }
        mNotifications.onReadProgress(protocol, 90);

    }

}