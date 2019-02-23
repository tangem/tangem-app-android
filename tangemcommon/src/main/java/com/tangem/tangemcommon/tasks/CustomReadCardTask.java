package com.tangem.tangemcommon.tasks;

import com.tangem.tangemcommon.data.external.CardDataSubstitutionProvider;
import com.tangem.tangemcommon.data.Manufacturer;
import com.tangem.tangemcommon.data.external.PINsProvider;
import com.tangem.tangemcommon.data.TangemCard;
import com.tangem.tangemcommon.reader.CardCrypto;
import com.tangem.tangemcommon.reader.CardProtocol;
import com.tangem.tangemcommon.reader.NfcReader;
import com.tangem.tangemcommon.reader.TLV;
import com.tangem.tangemcommon.reader.TLVException;
import com.tangem.tangemcommon.reader.TLVList;
import com.tangem.tangemcommon.util.Log;
import com.tangem.tangemcommon.util.Util;

import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.math.ec.ECPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

/**
 * Base class for card communication task
 */
public class CustomReadCardTask extends Thread {
    public static final String TAG = CustomReadCardTask.class.getSimpleName();

    protected NfcReader mIsoDep;
    protected CardProtocol.Notifications mNotifications;
    protected boolean isCancelled = false;
    protected TangemCard mCard;
    CardDataSubstitutionProvider localStorage;
    PINsProvider pinsProvider;
    CardProtocol protocol;

    // this fields are static to optimize process when need enter pin and scan card again
    private static ArrayList<String> lastRead_UnsuccessfullPINs = new ArrayList<>();
    private static TangemCard.EncryptionMode lastRead_Encryption = null;
    private static String lastRead_UID;

    public static void resetLastReadInfo() {
        lastRead_UID = "";
        lastRead_Encryption = null;
        lastRead_UnsuccessfullPINs.clear();
    }

    public CustomReadCardTask(TangemCard card, NfcReader reader, CardDataSubstitutionProvider cardDataSubstitutionProvider, PINsProvider pinsProvider, CardProtocol.Notifications notifications) {
        mIsoDep = reader;
        mNotifications = notifications;
        localStorage = cardDataSubstitutionProvider;
        this.pinsProvider = pinsProvider;
        mCard = card;
    }

    /**
     * Executing GET_ISSUER_DATA or WRITE_ISSUER_DATA depending on state in TangemCard object {@see TangemCard.getNeedWriteIssuerData()}
     * See [1] 8.7
     * Usually this function must be called if run_Task() of descendants
     *
     * @throws Exception if something went wrong
     */
    protected void run_ReadOrWriteIssuerData() throws Exception {
        TLVList tlvIssuerData;
        if (mCard.getNeedWriteIssuerData()) {
            protocol.run_WriteIssuerData(mCard.getIssuerData(), mCard.getIssuerDataSignature());
            try {
                tlvIssuerData = TLVList.fromBytes(mCard.getIssuerData());
            } catch (TLVException e) {
                e.printStackTrace();
                tlvIssuerData = null;
            }
        } else {
            try {
                tlvIssuerData = protocol.run_GetIssuerData();
            } catch (Exception e) {
                e.printStackTrace();
                tlvIssuerData = null;
            }
        }

        // try read offline balance data
        if (mCard.getStatus() == TangemCard.Status.Loaded) {
            if (tlvIssuerData != null && tlvIssuerData.getTLV(TLV.Tag.TAG_ValidatedBalance) != null && mCard.getMaxSignatures() == mCard.getRemainingSignatures()) {
                mCard.setOfflineBalance(tlvIssuerData.getTLV(TLV.Tag.TAG_ValidatedBalance).Value);
            } else {
                mCard.clearOfflineBalance();
            }
        } else {
            mCard.clearOfflineBalance();
        }

        // try read denomination
        if (tlvIssuerData != null && tlvIssuerData.getTLV(TLV.Tag.TAG_Denomination) != null) {
            if (tlvIssuerData.getTLV(TLV.Tag.TAG_DenominationText) != null) {
                mCard.setDenomination(tlvIssuerData.getTLV(TLV.Tag.TAG_Denomination).Value, tlvIssuerData.getTLV(TLV.Tag.TAG_DenominationText).getAsString());
            } else {
                mCard.setDenomination(tlvIssuerData.getTLV(TLV.Tag.TAG_Denomination).Value, null);
            }
        } else {
            mCard.clearDenomination();
        }
    }

    /**
     * Parse response of the last READ command into TangemCard object
     * See [1] 8.2, 5.3, 3.3
     *
     * @throws CardProtocol.TangemException - if something went wrong
     */
    public void parseReadResult() throws CardProtocol.TangemException {
        if (mCard == null) mCard = protocol.getCard();
        // These tags always present in the parsed response: TAG_Status, TAG_CID, TAG_Manufacture_ID, TAG_Health, TAG_Firmware
        TLV tlvStatus = protocol.getReadResult().getTLV(TLV.Tag.TAG_Status);
        mCard.setStatus(TangemCard.Status.fromCode(Util.byteArrayToInt(tlvStatus.Value)));
        TLV tlvCID = protocol.getReadResult().getTLV(TLV.Tag.TAG_CardID);
        mCard.setCID(tlvCID.Value);
        mCard.setManufacturer(Manufacturer.FindManufacturer(protocol.getReadResult().getTLV(TLV.Tag.TAG_Manufacture_ID).getAsString()), true);

        // Support of legacy Firmware
        if (protocol.getReadResult().getTLV(TLV.Tag.TAG_Firmware) != null) {
            mCard.setFirmwareVersion(protocol.getReadResult().getTLV(TLV.Tag.TAG_Firmware).getAsString());
        }
        mCard.setHealth(protocol.getReadResult().getTLV(TLV.Tag.TAG_Health).getAsInt());

        // If the card was previously personalized then parse personalized card data - card public key, blockchain data and other settings
        if (mCard.getStatus() != TangemCard.Status.NotPersonalized) {
            try {
                TLV tlvCardPubkicKey = protocol.getReadResult().getTLV(TLV.Tag.TAG_CardPublicKey);
                if (tlvCardPubkicKey == null)
                    throw new CardProtocol.TangemException("Invalid answer format");

                mCard.setCardPublicKey(tlvCardPubkicKey.Value);

                TLVList tlvCardData = TLVList.fromBytes(protocol.getReadResult().getTLV(TLV.Tag.TAG_CardData).Value);

                mCard.setBatch(tlvCardData.getTLV(TLV.Tag.TAG_Batch).getAsHexString());

                TLV tokenSymbol = tlvCardData.getTLV(TLV.Tag.TAG_Token_Symbol);
                TLV contractAddress = tlvCardData.getTLV(TLV.Tag.TAG_Token_Contract_Address);
                TLV tokens_decimal = tlvCardData.getTLV(TLV.Tag.TAG_Token_Decimal);

                if (tokenSymbol != null)
                    mCard.setTokenSymbol(tokenSymbol.getAsString());

                if (contractAddress != null)
                    mCard.setContractAddress(contractAddress.getAsString());

                if (tokens_decimal != null)
                    mCard.setTokensDecimal(tokens_decimal.getAsInt());

                byte[] tlvPersonalizationDT = tlvCardData.getTLV(TLV.Tag.TAG_ManufactureDateTime).Value;
                int year = (tlvPersonalizationDT[0] & 0xFF) << 8 | (tlvPersonalizationDT[1] & 0xFF);
                int month = tlvPersonalizationDT[2] - 1;
                int day = tlvPersonalizationDT[3];
                Calendar cd = Calendar.getInstance();
                cd.set(year, month, day, 0, 0, 0);
                mCard.setPersonalizationDateTime(cd.getTime());
                try {
                    // Support of legacy Firmware
                    if (mCard.getFirmwareVersion() == null) {
                        mCard.setFirmwareVersion(tlvCardData.getTLV(TLV.Tag.TAG_Firmware).getAsString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Cannot get firmware version");
                    mCard.setFirmwareVersion("0.00");
                }

                try {
                    if (mCard.getFirmwareVersion().compareTo("1.05") < 0) {
                        // In FW ver 1.05, the issuer has one key pair used as both DataKey and TransactionKey, see [1] 3.3.1 and [1] 3.3.2
                        //mCard.setIssuer(tlvCardData.getTLV(TLV.Tag.TAG_Issuer_ID).getAsString(), protocol.getReadResult().getTLV(TLV.Tag.TAG_Issuer_Transaction_PublicKey).Value);
                        mCard.setIssuer(tlvCardData.getTLV(TLV.Tag.TAG_Issuer_ID).getAsString(), protocol.getReadResult().getTLV(TLV.Tag.TAG_Issuer_Transaction_PublicKey).Value);
                    } else {
                        // In newer FW versions, the issuer has two different key pairs - DataKey and TransactionKey, see [1] 3.3.1 and [1] 3.3.2
                        mCard.setIssuer(tlvCardData.getTLV(TLV.Tag.TAG_Issuer_ID).getAsString(), protocol.getReadResult().getTLV(TLV.Tag.TAG_Issuer_Data_PublicKey).Value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Cannot get issuer, try a version for older cards");
                    try {
                        // for very very old cards Issuer key can be stored in different TLV tag
                        mCard.setIssuer(tlvCardData.getTLV(TLV.Tag.TAG_Issuer_ID).getAsString(), protocol.getReadResult().getTLV(TLV.Tag.TAG_Issuer_Transaction_PublicKey).Value);
                    } catch (Exception ee) {
                        ee.printStackTrace();
                        Log.e(TAG, "Cannot get issuer");
                        mCard.setIssuer("Unknown", null);
                    }
                }

                // Overriding of missing card data, e.g. for cards with unknown ERC20 contract data
                // This method must be called after the issuer data is defined because newly written data is verified by issuer data key
                try {
                    // Hardcoded for some known batches, or, for other batches, received from Tangem server and stored in local storage
                    if (mCard.getBatch().equals("0017")) {
                        mCard.setContractAddress("0x9Eef75bA8e81340da9D8d1fd06B2f313DB88839c");
                    } else if (mCard.getBatch().equals("0019")) {
                        mCard.setContractAddress("0x0c056b0cda0763cc14b8b2d6c02465c91e33ec72");
                    } else {
                        //CardDataSubstitutionProvider localStorage = new CardDataSubstitutionProvider(mContext);
                        if (localStorage != null) localStorage.applySubstitution(mCard);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Can't apply card data substitution");
                    e.printStackTrace();
                }

                mCard.setBlockchainID(tlvCardData.getTLV(TLV.Tag.TAG_Blockchain_ID).getAsString());

                try {
                    mCard.setSettingsMask(protocol.getReadResult().getTagAsInt(TLV.Tag.TAG_SettingsMask));
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Can't get settings mask");
                }

                if (protocol.getReadResult().getTLV(TLV.Tag.TAG_PauseBeforePIN2) != null) {
                    mCard.setPauseBeforePIN2(10 * protocol.getReadResult().getTagAsInt(TLV.Tag.TAG_PauseBeforePIN2));
                }

                try {
                    mCard.setSigningMethod(protocol.getReadResult().getTagAsInt(TLV.Tag.TAG_SigningMethod));
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Can't get signing method");
                    mCard.setSigningMethod(0);
                }

                try {
                    mCard.setMaxSignatures(protocol.getReadResult().getTagAsInt(TLV.Tag.TAG_MaxSignatures));
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Can't get max signatures");
                }

            } catch (Exception e) {
                e.printStackTrace();
                throw new CardProtocol.TangemException("Can't parse card data");
            }
        }


        // Parse additional parameters for Loaded cards: wallet public key, remaining signatures, etc
        if (mCard.getStatus() == TangemCard.Status.Loaded) {

            TLV tlvPublicKey = protocol.getReadResult().getTLV(TLV.Tag.TAG_Wallet_PublicKey);
            String curveID = protocol.getReadResult().getTLV(TLV.Tag.TAG_CurveID).getAsString();

            CardCrypto.Curve curve = CardCrypto.Curve.valueOf(curveID);
            switch (curve) {
                case secp256k1:
                    ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
                    ECPoint p1 = spec.getCurve().decodePoint(tlvPublicKey.Value);

                    byte pkUncompressed[] = p1.getEncoded(false);

                    byte pkCompresses[] = p1.getEncoded(true);
                    mCard.setWalletPublicKey(pkUncompressed);
                    mCard.setWalletPublicKeyRar(pkCompresses);
                    break;
                case ed25519:
                    mCard.setWalletPublicKey(tlvPublicKey.Value);
                    mCard.setWalletPublicKeyRar(tlvPublicKey.Value);
                    break;

            }

            mCard.setRemainingSignatures(protocol.getReadResult().getTagAsInt(TLV.Tag.TAG_RemainingSignatures));

            if (protocol.getReadResult() != null && protocol.getReadResult().getTLV(TLV.Tag.TAG_SignedHashes) != null) {
                mCard.setSignedHashes(protocol.getReadResult().getTagAsInt(TLV.Tag.TAG_SignedHashes));
            }

        } else {
//            mCard.setWallet("N/A");
        }
    }


    /**
     * On first time card reading run READ command, parse answer and Executing GET_ISSUER_DATA or WRITE_ISSUER_DATA depending on state in TangemCard object {@see TangemCard.getNeedWriteIssuerData()}
     * See [1] 8.7
     *
     * @throws Exception if something went wrong
     */
    public void run_FirstTimeRead() throws Exception {


        byte[] UID = mIsoDep.getId();
        String sUID = Util.byteArrayToHexString(UID);
        if (!lastRead_UID.equals(sUID)) {
            resetLastReadInfo();
        }

        Log.i(TAG, "[-- Start read card info --]");

        if (isCancelled) return;

        protocol.setPIN(CardProtocol.DefaultPIN);
        protocol.clearReadResult();

        if (lastRead_Encryption == null) {
            Log.i(TAG, "Try get supported encryption mode");
            protocol.run_GetSupportedEncryption();
        } else {
            Log.i(TAG, "Use already defined encryption mode: " + lastRead_Encryption.name());
            protocol.getCard().encryptionMode = lastRead_Encryption;
        }

        if (protocol.haveReadResult()) {
            //already have read result (obtained while get supported encryption), only read issuer data and define offline balance
            parseReadResult();
            pinsProvider.setLastUsedPIN(protocol.getCard().getPIN());
        } else {
            //don't have read result - may be don't get supported encryption on this try, need encryption or need another PIN
            if (lastRead_Encryption == null) {
                // we try get supported encryption on this time
                lastRead_Encryption = protocol.getCard().encryptionMode;
                if (protocol.getCard().encryptionMode == TangemCard.EncryptionMode.None) {
                    // default pin not accepted
                    lastRead_UnsuccessfullPINs.add(CardProtocol.DefaultPIN);
                }
            }

            boolean pinFound = false;
            for (String PIN : pinsProvider.getPINs()) {
                Log.e(TAG, "PIN: " + PIN);

                boolean skipPin = false;
                for (int i = 0; i < lastRead_UnsuccessfullPINs.size(); i++) {
                    if (lastRead_UnsuccessfullPINs.get(i).equals(PIN)) {
                        skipPin = true;
                        break;
                    }
                }

                if (skipPin) {
                    Log.e(TAG, "Skip PIN - already checked before");
                    continue;
                }

                try {
                    protocol.setPIN(PIN);
                    if (protocol.getCard().encryptionMode != TangemCard.EncryptionMode.None) {
                        protocol.CreateProtocolKey();
                    }
                    protocol.run_Read();
                    // After first successful read, data will be parsed into TangemCard object
                    parseReadResult();

                    pinsProvider.setLastUsedPIN(PIN);
                    pinFound = true;
                    protocol.getCard().setPIN(PIN);
                    break;
                } catch (CardProtocol.TangemException_InvalidPIN e) {
                    Log.e(TAG, e.getMessage());
                    lastRead_UnsuccessfullPINs.add(PIN);
                }
            }
            if (!pinFound) {
                throw new CardProtocol.TangemException_InvalidPIN("No valid PIN found!");
            }
        }
    }

    /**
     * Run read and check that the card is the same
     *
     * @throws Exception if something went wrong
     */
    public void run_SecondTimeRead() throws Exception {
        String PIN = mCard.getPIN();
        protocol.setPIN(PIN);
        protocol.run_Read();
        pinsProvider.setLastUsedPIN(PIN);
        if (!Arrays.equals(mCard.getCID(), protocol.getReadResult().getTLV(TLV.Tag.TAG_CardID).Value)) {
            throw new CardProtocol.TangemException("Card must be the same. Reading attempt on different card!");
        }
    }

    /**
     * This function must be override in descendants to arrive the task goal - sign, verify and etc
     * @throws Exception if something went wrong
     */
    public void run_Task() throws Exception {

    }

    @Override
    public void run() {
        if (mIsoDep == null) {
            return;
        }
        try {
            // for Samsung's bugs -
            // Workaround for the Samsung Galaxy S5 (since the
            // first connection always hangs on transceive).

            mIsoDep.connect();
            try {
                protocol = new CardProtocol(mIsoDep, mCard, mNotifications);
                mNotifications.onReadStart(protocol);
                try {
                    Log.i(TAG, String.format("[-- Start task -- %s --]", getClass().getSimpleName()));
                    mNotifications.onReadProgress(protocol, 5);
                    if (mCard == null) {
                        // first time reading
                        run_FirstTimeRead();
                    } else {
                        run_SecondTimeRead();
                    }
                    run_Task();
                    mNotifications.onReadProgress(protocol, 100);
                } catch (Exception e) {
                    e.printStackTrace();
                    protocol.setError(e);

                } finally {
                    Log.i(TAG, String.format("[-- Finish task -- %s --]", getClass().getSimpleName()));
                    mNotifications.onReadFinish(protocol);
                }
            } finally {
                mIsoDep.ignoreTag();
            }
        } catch (Exception e) {
            e.printStackTrace();
            mIsoDep.notifyReadResult(false);
        }
    }

    public void cancel(Boolean AllowInterrupt) {
        try {
            if (isAlive()) {
                isCancelled = true;
                join(500);
            }
            if (isAlive() && AllowInterrupt) {
                interrupt();
                mNotifications.onReadCancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
