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

public class SignTask extends CustomReadCardTask {
    public static final String TAG = SignTask.class.getSimpleName();

    private static final int MAX_HASHES_TO_SIGN = 10;

    /**
     * Transaction Engine request/notifications during sign process
     */
    public interface TransactionToSign {
        boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod);

        byte[][] getHashesToSign() throws Exception;

        byte[] getRawDataToSign() throws Exception;

        String getHashAlgToSign() throws Exception;

        byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception;

        byte[] onSignCompleted(byte[] signature) throws Exception;
    }

    private TransactionToSign transactionToSign;

    public SignTask(TangemCard card, NfcReader reader, CardDataSubstitutionProvider cardDataSubstitutionProvider, PINsProvider pinsProvider, CardProtocol.Notifications notifications, TransactionToSign transactionToSign) {
        super(card, reader, cardDataSubstitutionProvider, pinsProvider, notifications);
        this.transactionToSign = transactionToSign;
    }

    @Override
    public void run_Task() throws Exception {
        protocol.run_VerifyCard();

        Log.i(TAG, "Manufacturer: " + protocol.getCard().getManufacturer().getOfficialName());

        mNotifications.onReadProgress(protocol, 30);
        if (isCancelled) return;

        if (mCard.getPauseBeforePIN2() > 0 && !mCard.getTerminalIsLinked()) {
            mNotifications.onReadWait(mCard.getPauseBeforePIN2());
        }

        if (!transactionToSign.isSigningMethodSupported(mCard.getSigningMethod())) {
            throw new CardProtocol.TangemException("Signing method isn't supported!");
        }

        TLVList signResult;
        switch (mCard.getSigningMethod()) {
            case Sign_Hash:
                byte[][] remainingHashes = transactionToSign.getHashesToSign();
                ByteArrayOutputStream signatures = new ByteArrayOutputStream();
                while (remainingHashes.length > 0) {
                    byte[][] hashesToSign;
                    if (remainingHashes.length <= MAX_HASHES_TO_SIGN) {
                        hashesToSign = remainingHashes.clone();
                        remainingHashes = new byte[0][];
                    } else {
                        hashesToSign = new byte[MAX_HASHES_TO_SIGN][];
                        System.arraycopy(remainingHashes, 0, hashesToSign, 0, MAX_HASHES_TO_SIGN);
                        remainingHashes = removeFirstTenEntries(remainingHashes);
                    }

                    signResult = protocol.run_SignHashes(pinsProvider.getPIN2(), hashesToSign, null, null, null);
                    signatures.write(signResult.getTLV(TLV.Tag.TAG_Signature).Value);
                }
                mNotifications.onReadProgress(protocol, 100);
                transactionToSign.onSignCompleted(signatures.toByteArray());
                return;

            case Sign_Hash_Validated_By_Issuer:
            case Sign_Hash_Validated_By_Issuer_And_WriteIssuerData:
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                byte[][] hashes = transactionToSign.getHashesToSign();
                if (hashes.length > MAX_HASHES_TO_SIGN) throw new CardProtocol.TangemException("To much hashes in one transaction!");
                for (int i = 0; i < hashes.length; i++) {
                    if (i != 0 && hashes[0].length != hashes[i].length)
                        throw new CardProtocol.TangemException("Hashes length must be identical!");
                    bs.write(hashes[i]);
                }
                signResult = protocol.run_SignHashes(pinsProvider.getPIN2(), hashes, transactionToSign.getIssuerTransactionSignature(bs.toByteArray()), null, null);
                break;
            case Sign_Raw:
                signResult = protocol.run_SignRaw(pinsProvider.getPIN2(), transactionToSign.getHashAlgToSign(), transactionToSign.getRawDataToSign(), null, null, null);
                break;
            case Sign_Raw_Validated_By_Issuer:
            case Sign_Raw_Validated_By_Issuer_And_WriteIssuerData:
                byte[] txOut = transactionToSign.getRawDataToSign();
                signResult = protocol.run_SignRaw(pinsProvider.getPIN2(), transactionToSign.getHashAlgToSign(), txOut, transactionToSign.getIssuerTransactionSignature(txOut), null, null);
                break;
            default:
                throw new CardProtocol.TangemException("Signing method isn't supported!");
        }

        transactionToSign.onSignCompleted(signResult.getTLV(TLV.Tag.TAG_Signature).Value);
        //TODO: maybe we should move notifyOnNeedSendTransaction(txForSend) here?
        mNotifications.onReadProgress(protocol, 100);

    }

    private byte[][] removeFirstTenEntries(byte[][] array) {
        byte[][] temp = new byte[array.length - MAX_HASHES_TO_SIGN][];
        System.arraycopy(array, MAX_HASHES_TO_SIGN, temp, 0, temp.length);
        return temp;
    }
}
