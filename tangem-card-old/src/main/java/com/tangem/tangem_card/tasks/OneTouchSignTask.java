package com.tangem.tangem_card.tasks;

import com.tangem.tangem_card.data.TangemCard;
import com.tangem.tangem_card.data.external.CardDataSubstitutionProvider;
import com.tangem.tangem_card.data.external.PINsProvider;
import com.tangem.tangem_card.reader.CardProtocol;
import com.tangem.tangem_card.reader.NfcReader;
import com.tangem.tangem_card.reader.TLV;
import com.tangem.tangem_card.reader.TLVList;
import com.tangem.tangem_card.util.Log;

import java.io.ByteArrayOutputStream;

public class OneTouchSignTask extends ReadCardInfoTask {
    public static final String TAG = OneTouchSignTask.class.getSimpleName();

    /**
     * Transaction Engine request/notifications during sign process
     */
    public interface TransactionToSign {
        boolean isSigningOnCardSupported(TangemCard card);

        boolean isIssuerCanSignData(TangemCard card);

        boolean isIssuerCanSignTransaction(TangemCard card);

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

        if (!transactionToSign.isSigningOnCardSupported(mCard)) {
            throw new CardProtocol.TangemException("Signing method isn't supported!");
        }

        TLVList signResult;

        if (transactionToSign.isIssuerCanSignTransaction(mCard) &&
                (
                        mCard.allowedSigningMethod.contains(TangemCard.SigningMethod.Sign_Hash_Validated_By_Issuer) ||
                                (
                                        transactionToSign.isIssuerCanSignData(mCard) &&
                                                mCard.allowedSigningMethod.contains(TangemCard.SigningMethod.Sign_Hash_Validated_By_Issuer_And_WriteIssuerData)
                                )
                )
        ) {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            byte[][] hashes = transactionToSign.getHashesToSign(mCard);
            if (hashes.length > 10) throw new CardProtocol.TangemException("To much hashes in one transaction!");
            for (int i = 0; i < hashes.length; i++) {
                if (i != 0 && hashes[0].length != hashes[i].length)
                    throw new CardProtocol.TangemException("Hashes length must be identical!");
                bs.write(hashes[i]);
            }
            signResult = protocol.run_SignHashes(pinsProvider.getPIN2(), hashes, transactionToSign.getIssuerTransactionSignature(mCard, bs.toByteArray()), null, null);
        } else if (
                transactionToSign.isIssuerCanSignTransaction(mCard) &&
                        (
                                mCard.allowedSigningMethod.contains(TangemCard.SigningMethod.Sign_Raw_Validated_By_Issuer) ||
                                        (
                                                transactionToSign.isIssuerCanSignData(mCard) &&
                                                        mCard.allowedSigningMethod.contains(TangemCard.SigningMethod.Sign_Raw_Validated_By_Issuer_And_WriteIssuerData)
                                        )
                        )
        ) {
            byte[] txOut = transactionToSign.getRawDataToSign(mCard);
            signResult = protocol.run_SignRaw(pinsProvider.getPIN2(), transactionToSign.getHashAlgToSign(mCard), txOut, transactionToSign.getIssuerTransactionSignature(mCard, txOut), null, null);
        } else if (mCard.allowedSigningMethod.contains(TangemCard.SigningMethod.Sign_Hash)) {
            signResult = protocol.run_SignHashes(pinsProvider.getPIN2(), transactionToSign.getHashesToSign(mCard), null, null, null);
        } else if (mCard.allowedSigningMethod.contains(TangemCard.SigningMethod.Sign_Raw)) {
            signResult = protocol.run_SignRaw(pinsProvider.getPIN2(), transactionToSign.getHashAlgToSign(mCard), transactionToSign.getRawDataToSign(mCard), null, null, null);
        } else {
            throw new CardProtocol.TangemException("Signing method isn't supported!");
        }

        transactionToSign.onSignCompleted(mCard, signResult.getTLV(TLV.Tag.TAG_Signature).Value);
        mNotifications.onReadProgress(protocol, 100);
    }
}
