package com.tangem.card_common.tasks;

import com.tangem.card_common.data.TangemCard;
import com.tangem.card_common.data.external.CardDataSubstitutionProvider;
import com.tangem.card_common.data.external.PINsProvider;
import com.tangem.card_common.reader.CardProtocol;
import com.tangem.card_common.reader.NfcReader;
import com.tangem.card_common.reader.TLV;
import com.tangem.card_common.reader.TLVList;
import com.tangem.card_common.util.Log;

import java.io.ByteArrayOutputStream;

public class OneTouchSignTask extends ReadCardInfoTask {
    public static final String TAG = OneTouchSignTask.class.getSimpleName();

    /**
     * Transaction Engine request/notifications during sign process
     */
    public interface TransactionToSign {
        boolean isSigningOnCardSupported(TangemCard card);

        byte[][] getHashesToSign(TangemCard card) throws Exception;

        byte[] getRawDataToSign(TangemCard card) throws Exception;

        String getHashAlgToSign(TangemCard card) throws Exception;

        byte[] getIssuerTransactionSignature(TangemCard card, byte[] dataToSignByIssuer) throws Exception;

        void onSignCompleted(TangemCard card, byte[] signature) throws Exception;
    }

    private TransactionToSign transactionToSign;

    public OneTouchSignTask(NfcReader reader, CardDataSubstitutionProvider cardDataSubstitutionProvider, PINsProvider pinsProvider, CardProtocol.Notifications notifications, TransactionToSign transactionToSign) {
        super(reader, cardDataSubstitutionProvider, pinsProvider, notifications);
        this.transactionToSign = transactionToSign;
    }

    @Override
    public void run_Task() throws Exception {
        super.run_Task();
        Log.i(TAG, "Manufacturer: " + protocol.getCard().getManufacturer().getOfficialName());

        mNotifications.onReadProgress(protocol, 30);
        if (isCancelled) return;

        if (mCard.getPauseBeforePIN2() > 0) {
            mNotifications.onReadWait(mCard.getPauseBeforePIN2());
        }

        if (!transactionToSign.isSigningOnCardSupported(mCard)) {
            throw new CardProtocol.TangemException("Signing method isn't supported!");
        }

        TLVList signResult;
        switch (mCard.getSigningMethod()) {
            case Sign_Hash:
                signResult = protocol.run_SignHashes(pinsProvider.getPIN2(), transactionToSign.getHashesToSign(mCard), null, null, null);
                break;
            case Sign_Hash_Validated_By_Issuer:
            case Sign_Hash_Validated_By_Issuer_And_WriteIssuerData:
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                byte[][] hashes = transactionToSign.getHashesToSign(mCard);
                if (hashes.length > 10) throw new CardProtocol.TangemException("To much hashes in one transaction!");
                for (int i = 0; i < hashes.length; i++) {
                    if (i != 0 && hashes[0].length != hashes[i].length)
                        throw new CardProtocol.TangemException("Hashes length must be identical!");
                    bs.write(hashes[i]);
                }
                signResult = protocol.run_SignHashes(pinsProvider.getPIN2(), hashes, transactionToSign.getIssuerTransactionSignature(mCard, bs.toByteArray()), null, null);
                break;
            case Sign_Raw:
                signResult = protocol.run_SignRaw(pinsProvider.getPIN2(), transactionToSign.getHashAlgToSign(mCard), transactionToSign.getRawDataToSign(mCard), null, null, null);
                break;
            case Sign_Raw_Validated_By_Issuer:
            case Sign_Raw_Validated_By_Issuer_And_WriteIssuerData:
                byte[] txOut = transactionToSign.getRawDataToSign(mCard);
                signResult = protocol.run_SignRaw(pinsProvider.getPIN2(), transactionToSign.getHashAlgToSign(mCard), txOut, transactionToSign.getIssuerTransactionSignature(mCard, txOut), null, null);
                break;
            default:
                throw new CardProtocol.TangemException("Signing method isn't supported!");
        }

        transactionToSign.onSignCompleted(mCard, signResult.getTLV(TLV.Tag.TAG_Signature).Value);
        mNotifications.onReadProgress(protocol, 100);

    }
}
