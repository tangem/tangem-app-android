package com.tangem.tangemcard.tasks;

import com.tangem.tangemcard.data.external.CardDataSubstitutionProvider;
import com.tangem.tangemcard.data.TangemCard;
import com.tangem.tangemcard.data.external.PINsProvider;
import com.tangem.tangemcard.reader.CardProtocol;
import com.tangem.tangemcard.reader.NfcReader;
import com.tangem.tangemcard.reader.TLV;
import com.tangem.tangemcard.reader.TLVList;
import com.tangem.tangemcard.util.Log;

import java.io.ByteArrayOutputStream;

public class SignTask extends CustomReadCardTask {
    public static final String TAG = SignTask.class.getSimpleName();

    /**
     * Payment Engine request/notifications during sign process
     */
    public interface PaymentToSign {
        boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod);

        byte[][] getHashesToSign() throws Exception;

        byte[] getRawDataToSign() throws Exception;

        String getHashAlgToSign() throws Exception;

        byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception;

        void onSignCompleted(byte[] signature) throws Exception;
    }

    private PaymentToSign paymentToSign;

    public SignTask(TangemCard card, NfcReader reader, CardDataSubstitutionProvider cardDataSubstitutionProvider, PINsProvider pinsProvider, CardProtocol.Notifications notifications, PaymentToSign paymentToSign) {
        super(card, reader, cardDataSubstitutionProvider, pinsProvider, notifications);
        this.paymentToSign = paymentToSign;
    }

    @Override
    public void run_Task() throws Exception {
        protocol.run_VerifyCard();

        Log.i(TAG, "Manufacturer: " + protocol.getCard().getManufacturer().getOfficialName());

        mNotifications.onReadProgress(protocol, 30);
        if (isCancelled) return;

        if (mCard.getPauseBeforePIN2() > 0) {
            mNotifications.onReadWait(mCard.getPauseBeforePIN2());
        }

        if (!paymentToSign.isSigningMethodSupported(mCard.getSigningMethod())) {
            throw new CardProtocol.TangemException("Signing method isn't supported!");
        }

        TLVList signResult;
        switch (mCard.getSigningMethod()) {
            case Sign_Hash:
                signResult = protocol.run_SignHashes(pinsProvider.getPIN2(), paymentToSign.getHashesToSign(), null, null, null);
                break;
            case Sign_Hash_Validated_By_Issuer:
            case Sign_Hash_Validated_By_Issuer_And_WriteIssuerData:
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                byte[][] hashes = paymentToSign.getHashesToSign();
                if (hashes.length > 10) throw new CardProtocol.TangemException("To much hashes in one transaction!");
                for (int i = 0; i < hashes.length; i++) {
                    if (i != 0 && hashes[0].length != hashes[i].length)
                        throw new CardProtocol.TangemException("Hashes length must be identical!");
                    bs.write(hashes[i]);
                }
                signResult = protocol.run_SignHashes(pinsProvider.getPIN2(), hashes, paymentToSign.getIssuerTransactionSignature(bs.toByteArray()), null, null);
                break;
            case Sign_Raw:
                signResult = protocol.run_SignRaw(pinsProvider.getPIN2(), paymentToSign.getHashAlgToSign(), paymentToSign.getRawDataToSign(), null, null, null);
                break;
            case Sign_Raw_Validated_By_Issuer:
            case Sign_Raw_Validated_By_Issuer_And_WriteIssuerData:
                byte[] txOut = paymentToSign.getRawDataToSign();
                signResult = protocol.run_SignRaw(pinsProvider.getPIN2(), paymentToSign.getHashAlgToSign(), txOut, paymentToSign.getIssuerTransactionSignature(txOut), null, null);
                break;
            default:
                throw new CardProtocol.TangemException("Signing method isn't supported!");
        }

        paymentToSign.onSignCompleted(signResult.getTLV(TLV.Tag.TAG_Signature).Value);
        mNotifications.onReadProgress(protocol, 100);

    }
}
