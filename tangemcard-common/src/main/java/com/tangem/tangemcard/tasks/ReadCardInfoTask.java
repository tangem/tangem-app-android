package com.tangem.tangemcard.tasks;

import com.tangem.tangemcard.data.external.CardDataSubstitutionProvider;
import com.tangem.tangemcard.data.external.PINsProvider;
import com.tangem.tangemcard.reader.NfcReader;
import com.tangem.tangemcard.reader.CardProtocol;
import com.tangem.tangemcard.data.TangemCard;

public class ReadCardInfoTask extends CustomReadCardTask {
    public static final String TAG = ReadCardInfoTask.class.getSimpleName();

    public ReadCardInfoTask(NfcReader reader, CardDataSubstitutionProvider cardDataSubstitutionProvider, PINsProvider pinsProvider, CardProtocol.Notifications notifications) {
        super(null, reader, cardDataSubstitutionProvider, pinsProvider, notifications);
    }

    /**
     * Verify if default PIN2 is set on the card and enable UseDefaultPIN2 flag {@link TangemCard}
     * Works in firmware 1.12 and later, for older version UseDefaultPIN2 is always null
     * This function NEVER triggers security delay
     *
     * @throws Exception - if something went wrong
     */
    private void run_CheckPIN2isDefault() throws Exception {
        // can obtain SetPIN(to default) answer without security delay - try check if PIN2 is default with card request
        if (mCard.isFirmwareNewer("1.19") || (mCard.isFirmwareNewer("1.12") && (mCard.getPauseBeforePIN2() == 0 || mCard.useSmartSecurityDelay()))) {
            try {
                protocol.run_SetPIN(CardProtocol.DefaultPIN2, mCard.getPIN(), CardProtocol.DefaultPIN2, true);
                mCard.setUseDefaultPIN2(true);
            } catch (CardProtocol.TangemException_NeedPause e) {
                mCard.setUseDefaultPIN2(null);
            } catch (CardProtocol.TangemException_InvalidPIN e) {
                mCard.setUseDefaultPIN2(false);
            }
        } else {
            mCard.setUseDefaultPIN2(null);
        }
    }

    @Override
    public void run_Task() throws Exception {
        mNotifications.onReadProgress(protocol, 20);
        run_ReadOrWriteIssuerData();
        mNotifications.onReadProgress(protocol, 50);
        run_CheckPIN2isDefault();
        mNotifications.onReadProgress(protocol, 90);
    }

}