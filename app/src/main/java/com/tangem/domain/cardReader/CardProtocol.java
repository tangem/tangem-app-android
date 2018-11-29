package com.tangem.domain.cardReader;

import android.content.Context;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.util.Log;

import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.CoinEngineFactory;
import com.tangem.domain.wallet.Issuer;
import com.tangem.data.db.LocalStorage;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.domain.wallet.Manufacturer;
import com.tangem.domain.wallet.TangemContext;
import com.tangem.util.Util;

import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.interfaces.ECPublicKey;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.math.ec.ECPoint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Calendar;

import javax.crypto.KeyAgreement;

/**
 * Implementation of the Tangem Card NFC Protocol
 * See "Tangem card and NFC protocol Technical manual" [1]
 *
 * @author dvol
 * 14.07.2017
 */
public class CardProtocol {
    private static final String logTag = "CardProtocol";

    /**
     * UI notifications
     */
    public interface Notifications {
        /**
         * call on start reading card
         *
         * @param cardProtocol - instance of protocol class
         */
        void onReadStart(CardProtocol cardProtocol);

        /**
         * called during reading thread progress - to show progress bar, etc.
         *
         * @param cardProtocol - instance of protocol class
         * @param progress     - progress in percents
         */
        void onReadProgress(CardProtocol cardProtocol, int progress);

        /**
         * called after the reading is over
         *
         * @param cardProtocol - instance of protocol class
         */
        void onReadFinish(CardProtocol cardProtocol);

        /**
         * called if reading thread was canceled (e.g. due to paused activity)
         */
        void onReadCancel();

        /**
         * called approx. every second while card security delay is pending
         * called with msec=0 after the delay is over and command has been executed
         *
         * @param msec - estimated time in ms before the delay is finished
         */
        void onReadWait(int msec);

        /**
         * called before sending command to a card
         *
         * @param timeout - maximum waiting time before the answer is received or timeout occurs
         */
        void onReadBeforeRequest(int timeout);

        /**
         * called when the command answer is received or error/timeout occurred
         */
        void onReadAfterRequest();
    }

    private Notifications mNotifications;


    /**
     * Return object containing data received from a card
     *
     * @return TangemCard
     * @see TangemCard
     */
    public TangemCard getCard() {
        return mCard;
    }

    private static final int SW_PIN_ERROR = SW.INVALID_PARAMS;

    private IsoDep mIsoDep;

    /**
     * @return Android NFC tag object
     */
    public Tag getTag() {
        return mIsoDep.getTag();
    }

    public static final String DefaultPIN = "000000";
    public static final String DefaultPIN2 = "000";

    private String mPIN;

    public void setPIN(String PIN) {
        mPIN = PIN;
        if (mCard != null) {
            mCard.setPIN(PIN);
        }
    }

    private TangemCard mCard;
    private Exception mError;
    private Context mContext;

    /**
     * Set occurred error
     *
     * @param error Exception object
     */
    public void setError(Exception error) {
        mError = error;
    }

    /**
     * Get occurred error
     *
     * @return Exception occurred during reading a card
     */
    public Exception getError() {
        return mError;
    }

    /**
     * Constructor for reading a card for the first time
     *
     * @param context       - Android context
     * @param isoDep        - Android NFC tag
     * @param notifications - UI notification callbacks
     */
    public CardProtocol(Context context, IsoDep isoDep, Notifications notifications) {
        mContext = context;
        mIsoDep = isoDep;
        mNotifications = notifications;
        mPIN = null;
        mCard = new TangemCard(Util.byteArrayToHexString(mIsoDep.getTag().getId()));
    }

    /**
     * Constructor for subsequent reading of the card
     *
     * @param context       - Android context
     * @param isoDep        - Android NFC tag
     * @param card          - TangemCard object, stored data from previous reading
     * @param notifications - UI notification callbacks
     */
    public CardProtocol(Context context, IsoDep isoDep, TangemCard card, Notifications notifications) {
        mContext = context;
        mIsoDep = isoDep;
        mNotifications = notifications;
        mPIN = card.getPIN();
        mCard = card;
    }

    /**
     * Base exception class for exceptions on card reading
     */
    public static class TangemException extends Exception {
        public TangemException(String message) {
            super(message);
        }
    }

    /**
     * Thrown on failed attempt of reading with possible wrong PIN
     */
    public static class TangemException_InvalidPIN extends TangemException {
        public TangemException_InvalidPIN(String message) {
            super(message);
        }
    }

    /**
     * Thrown when card enforces security delay
     */
    public static class TangemException_NeedPause extends TangemException {
        public TangemException_NeedPause(String message) {
            super(message);
        }
    }


    /**
     * Thrown if APDU commands with extended length is not supported by an Android NFC device. This issue can occur on some legacy devices.
     * This exception means that this particular action can't be executed on this device because it's impossible to transfer all needed data to/from a card.
     */
    public static class TangemException_ExtendedLengthNotSupported extends TangemException {
        public TangemException_ExtendedLengthNotSupported(String message) {
            super(message);
        }
    }

    public static class TangemException_WrongAmount extends TangemException {
        public TangemException_WrongAmount(String message) {
            super(message);
        }
    }

    /**
     * Return ISO 14443-3 tag UID - unique card identifier
     * The tag identifier is a low level number used for anti-collision
     * and identification. It has nothing to do with CID.
     * <p>
     * Used internally for protocol encryption
     * <p>
     * Can be used on subsequent reading to first fast check that
     * you read the same card
     *
     * @return UID byte array
     */
    public byte[] GetUID() {
        if (mIsoDep == null || mIsoDep.getTag() == null) return null;
        return mIsoDep.getTag().getId();
    }

    /**
     * Return ISO 14443-3 tag reading timeout
     */
    public int getTimeout() {
        if (mIsoDep == null || mIsoDep.getTag() == null) return 60000;
        return mIsoDep.getTimeout();
    }


    /**
     * protocolKey
     * a base value used to construct the communication encryption key derived from PIN and UID
     * See [1] 4.3, 4.4, 4.5
     * {@see run_OpenSession}
     */
    private byte[] protocolKey;

    public void resetProtocolKey() {
        protocolKey = null;
    }

    /**
     * Calculate protocolKey
     * See [1] 4.3, 4.4, 4.5
     *
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public void CreateProtocolKey() throws NoSuchAlgorithmException, InvalidKeyException {
        protocolKey = CardCrypto.pbkdf2(Util.calculateSHA256(mPIN), GetUID(), 50);
        //Log.e("Reader", String.format("PIN: %s, Protocol key: %s", mPIN, Util.bytesToHex(protocolKey)));
        if (sessionKey != null) {
            sessionKey = null;
        }
    }

    /**
     * current session communication encryption key
     * See [1] 4.1, 4.3, 4.4, 4.5
     */
    private byte[] sessionKey = null;


    /**
     * Execute open session command and calculate session key {@see CardProtocol.sessionKey}
     * During this command the card and the device exchange with random challenges (for Fast encryption mode) or
     * public keys (for Strong encryption mode) and calculate sessionKey based protocolKey as:
     * - sha256(challengeDevice|challengeCard|protocolKey) for Fast encryption
     * - sha256(ECDH shared secret|protocolKey) for Strong encryption
     * See [1] 4.1, 4.3, 4.4, 4.5
     *
     * @param encryptionMode - mode of encryption {@link TangemCard.EncryptionMode}
     * @throws Exception if something went wrong
     */
    private void run_OpenSession(TangemCard.EncryptionMode encryptionMode) throws Exception {
        sessionKey = null;
        try {
            CommandApdu cmdApdu = new CommandApdu(CommandApdu.ISO_CLA, INS.OpenSession.Code, 0, encryptionMode.getP());

            switch (encryptionMode) {
                case Fast: {
                    // See [1] 4.3
                    byte[] baMyChallenge = Util.generateRandomBytes(16);

                    cmdApdu.addTLV(TLV.Tag.TAG_Session_Key_A, baMyChallenge);
                    Log.i(logTag, cmdApdu.getCommandName());
                    ResponseApdu rspApdu = null;

                    try {
                        if (mIsoDep == null) {
                            throw new TagLostException();
                        }
                        byte[] cmdBytes = cmdApdu.toBytes();
                        String cmdStr = CommandApdu.toString(cmdBytes, cmdApdu.getLc());
                        Log.v("NFC", String.format("<< [%s]: %s", cmdApdu.getCommandName(), cmdStr));

                        byte[] rsp = mIsoDep.transceive(cmdBytes);
                        rspApdu = new ResponseApdu(rsp);

                        Log.v("NFC", String.format(">> [%s]: %s", cmdApdu.getCommandName(), Util.bytesToHex(rsp)));

                        if (rspApdu.isParsedWithError()) {
                            throw new Exception("Can't parse answer");
                        }
                    } catch (Exception E) {
                        sessionKey = null;
                        throw E;
                    }

                    if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {
                        Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
                        byte[] baTheirsChallenge = rspApdu.getTLVs().getTLV(TLV.Tag.TAG_Session_Key_B).Value;
                        if (protocolKey == null) CreateProtocolKey();
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        outputStream.write(baMyChallenge);
                        outputStream.write(baTheirsChallenge);
                        outputStream.write(protocolKey);
                        sessionKey = Util.calculateSHA256(outputStream.toByteArray());
                        //Log.i(logTag, String.format("Session key: %s", Util.bytesToHex(sessionKey)));
                    } else {
                        Log.e(logTag, String.format("Failed: %04X - %s", rspApdu.getSW1SW2(), rspApdu.getSW1SW2Description()));
                        throw new Exception(String.format("Can't open session: SW - %04X", rspApdu.getSW1SW2()));
                    }
                }
                break;
                case Strong: {
                    // See [1] 4.4
                    KeyPairGenerator kpgen = KeyPairGenerator.getInstance("ECDH", "SC");
                    kpgen.initialize(new ECGenParameterSpec("secp256k1"), new SecureRandom());
                    KeyPair KP = kpgen.generateKeyPair();
                    KeyAgreement ka = KeyAgreement.getInstance("ECDH", "SC");
                    ka.init(KP.getPrivate());

                    ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
                    //return spec.getG().multiply(new BigInteger((ECPrivateKey) )).getEncoded(false);
                    ECPublicKey eckey = (ECPublicKey) KP.getPublic();
                    byte[] baMyPublicKey = eckey.getQ().getEncoded(false);

                    cmdApdu.addTLV(TLV.Tag.TAG_Session_Key_A, baMyPublicKey);
                    Log.i(logTag, cmdApdu.getCommandName());
                    ResponseApdu rspApdu = null;

                    try {
                        if (mIsoDep == null) {
                            throw new TagLostException();
                        }
                        //mIsoDep.setTimeout(msTimeout);

                        byte[] cmdBytes = cmdApdu.toBytes();
                        String cmdStr = CommandApdu.toString(cmdBytes, cmdApdu.getLc());
                        Log.v("NFC", String.format("<< [%s]: %s", cmdApdu.getCommandName(), cmdStr));
                        byte[] rsp = mIsoDep.transceive(cmdBytes);
                        rspApdu = new ResponseApdu(rsp);
                        Log.v("NFC", String.format(">> [%s]: %s", cmdApdu.getCommandName(), Util.bytesToHex(rsp)));

                        if (rspApdu.isParsedWithError()) {
                            throw new Exception("Can't parse answer");
                        }
                    } catch (Exception E) {
                        sessionKey = null;
                        throw E;
                    }

                    if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {
                        Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
                        byte[] baTheirsPublicKey = rspApdu.getTLVs().getTLV(TLV.Tag.TAG_Session_Key_B).Value;
                        ka.doPhase(CardCrypto.LoadPublicKey(baTheirsPublicKey), true);
                        if (protocolKey == null) CreateProtocolKey();
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        outputStream.write(ka.generateSecret());
                        outputStream.write(protocolKey);
                        sessionKey = Util.calculateSHA256(outputStream.toByteArray());
//                        Log.i(logTag, String.format("Session key: %s", Util.bytesToHex(sessionKey)));
                    } else {
                        Log.i(logTag, String.format("Failed: %04X - %s", rspApdu.getSW1SW2(), rspApdu.getSW1SW2Description()));
                        throw new Exception(String.format("Can't open session: SW - %04X", rspApdu.getSW1SW2()));
                    }
                }
                break;
                default:
                    throw new Exception("Unknown encryption mode");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(logTag, String.format("Exception: %s", e.getMessage()));
            throw new Exception("Can't open session: " + e.getMessage());
        }
    }


    /**
     * Send the specified APDU command to a card and receive an answer
     * Should have a prior opened encryption session if encryption is used
     * See [1] 4
     *
     * @param cmdApdu           - APDU command to send
     * @param breakOnNeedPause  - Specifies what to do when the card requests a security delay (interrupt transfer or wait till the end of the delay )
     * @return                  - response APDU
     * @throws Exception        - if something went wrong
     */
    private ResponseApdu SendAndReceive(CommandApdu cmdApdu, boolean breakOnNeedPause) throws Exception {
        if (mCard.encryptionMode != TangemCard.EncryptionMode.None) {
            if (sessionKey == null) {
                run_OpenSession(mCard.encryptionMode);
            }
            cmdApdu.Crypt(sessionKey);
        }
        cmdApdu.setP1(mCard.encryptionMode.getP());
        byte[] cmdBytes = cmdApdu.toBytes();
        String cmdStr = CommandApdu.toString(cmdBytes, cmdApdu.getLc());
        Log.v("NFC", String.format("<< [%s]: %s", cmdApdu.getCommandName(), cmdStr));
        byte[] rsp;
        ResponseApdu rspApdu;
        try {
            do {
                try {
                    mNotifications.onReadBeforeRequest(mIsoDep.getTimeout());
                    try {
                        rsp = mIsoDep.transceive(cmdBytes);
                    } finally {
                        mNotifications.onReadAfterRequest();
                    }
                } catch (IOException e) {
                    if (e.getMessage().contains("length")) {
                        throw new TangemException_ExtendedLengthNotSupported(e.getMessage());
                    }
                    throw e;
                }
                if (mCard.encryptionMode != TangemCard.EncryptionMode.None && !ResponseApdu.isStatusWord(rsp, SW.NEED_PAUSE)) {
                    rspApdu = ResponseApdu.Decrypt(rsp, sessionKey);
                } else {
                    rspApdu = new ResponseApdu(rsp);
                }

                if (rspApdu.isParsedWithError()) {
                    Log.v("NFC", String.format(">> [%s]: %s", cmdApdu.getCommandName(), Util.bytesToHex(rsp)));
                    throw new TangemException(rspApdu.getParseErroMessage());
                } else if (rspApdu.getSW1SW2() == SW.NEED_PAUSE && mNotifications != null) {
                    int remainingPause = 60000;
                    TLV tlvPause = rspApdu.getTLVs().getTLV(TLV.Tag.TAG_Pause);
                    if (tlvPause != null) {
                        remainingPause = rspApdu.getTLVs().getTagAsInt(TLV.Tag.TAG_Pause) * 10;
                    }

                    Log.v("NFC", String.format(">> Security delay, remaining %f s", remainingPause / 1000.0));
                    if (breakOnNeedPause) {
                        break;
                    } else {
                        mNotifications.onReadWait(remainingPause);
                    }
                } else {
                    Log.v("NFC", String.format(">> [%s]: %s", cmdApdu.getCommandName(), Util.bytesToHex(rsp)));
                }
            } while (rspApdu.getSW1SW2() == SW.NEED_PAUSE);
        } finally {
            mNotifications.onReadWait(0);
        }

        return rspApdu;
    }

    /**
     * Helper for preparing APDU command - init CommandApdu object and add common TLV tags ([CID,] PIN)
     *
     * @param ins - instruction code {@link INS}
     * @return CommandAPDU
     * @throws NoSuchAlgorithmException - if no sha256 library found
     */
    private CommandApdu StartPrepareCommand(INS ins) throws NoSuchAlgorithmException {
        CommandApdu Apdu = new CommandApdu(ins);
        byte[] baPIN = Util.calculateSHA256(mPIN);
        Apdu.addTLV(TLV.Tag.TAG_PIN, baPIN);
        if (ins != INS.Read) {
            Apdu.addTLV(TLV.Tag.TAG_CardID, mCard.getCID());
        }
        return Apdu;
    }

    /**
     * Run READ command and parse answer
     * {@see run_Read(boolean parseResult) }
     *
     * @throws Exception if something went wrong
     */
    public void run_Read() throws Exception {
        run_Read(true);
    }

    /**
     * Run READ command and parse answer (if specified)
     * <p>
     * This command returns all card and wallet data, including unique card number (CID) that has to be submitted when further calling all other commands. Therefore,
     * READ_CARD should always be used in the beginning of communication session between NFC device and Tangem card
     * <p>
     * In order to obtain card’s data, the app should call READ_CARD command with correct PIN1 value as a parameter. The card will not respond if wrong PIN1
     * has been submitted
     * This command requires only PIN1 parameter while other commands also need CID
     * See [1] 8, 8.2
     *
     * @param parseResult - parse answer into TangemCard object or not
     * @throws Exception if something went wrong
     */
    public void run_Read(boolean parseResult) throws Exception {
        CommandApdu rqApdu = StartPrepareCommand(INS.Read);
        Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

        ResponseApdu rspApdu = SendAndReceive(rqApdu, false);

        if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {

            Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
            readResult = rspApdu.getTLVs();
            if (parseResult) {
                // After first successful read, data will be parsed into TangemCard object
                parseReadResult();
                //TODO: move issuer data out from here
                run_ReadWriteIssuerData();
            } else {
                //TODO: check that card is the same
            }

        } else if (rspApdu.isStatus(SW_PIN_ERROR)) {
            throw new TangemException_InvalidPIN(String.format("FAILED: [%04X] - Possible PIN is invalid!\n", rspApdu.getSW1SW2()));
        } else {
            throw new TangemException(String.format("FAILED: [%04X]\n", rspApdu.getSW1SW2()));
        }
    }


    /**
     * Executing GET_ISSUER_DATA or WRITE_ISSUER_DATA depending on state in TangemCard object {@see TangemCard.getNeedWriteIssuerData()}
     * See [1] 8.7
     *
     * @throws Exception if something went wrong
     */
    public void run_ReadWriteIssuerData() throws Exception {
        TLVList tlvIssuerData;
        if (mCard.getNeedWriteIssuerData()) {
            run_WriteIssuerData(mCard.getIssuerData(), mCard.getIssuerDataSignature());
            try {
                tlvIssuerData = TLVList.fromBytes(mCard.getIssuerData());
            } catch (TLVException e) {
                e.printStackTrace();
                tlvIssuerData = null;
            }
        } else {
            try {
                tlvIssuerData = run_GetIssuerData();
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
                mCard.setDenomination(tlvIssuerData.getTLV(TLV.Tag.TAG_Denomination).Value);
            }
        } else {
            mCard.clearDenomination();
        }
    }

    /**
     * A TLV list containing response of the last READ command
     */
    private TLVList readResult = null;

    public void clearReadResult() {
        sessionKey = null;
        readResult = null;
    }

    public boolean haveReadResult() {
        return readResult != null;
    }

    /**
     * Parse response of the last READ command into TangemCard object
     * See [1] 8.2, 5.3, 3.3
     *
     * @throws TangemException - if something went wrong
     */
    public void parseReadResult() throws TangemException {
        // These tags always present in the parsed response: TAG_Status, TAG_CID, TAG_Manufacture_ID, TAG_Health, TAG_Firmware
        TLV tlvStatus = readResult.getTLV(TLV.Tag.TAG_Status);
        mCard.setStatus(TangemCard.Status.fromCode(Util.byteArrayToInt(tlvStatus.Value)));
        TLV tlvCID = readResult.getTLV(TLV.Tag.TAG_CardID);
        mCard.setCID(tlvCID.Value);
        mCard.setManufacturer(Manufacturer.FindManufacturer(readResult.getTLV(TLV.Tag.TAG_Manufacture_ID).getAsString()), true);

        // Support of legacy Firmware
        if (readResult.getTLV(TLV.Tag.TAG_Firmware) != null) {
            mCard.setFirmwareVersion(readResult.getTLV(TLV.Tag.TAG_Firmware).getAsString());
        }
        mCard.setHealth(readResult.getTLV(TLV.Tag.TAG_Health).getAsInt());

        // If the card was previously personalized then parse personalized card data - card public key, blockchain data and other settings
        if (mCard.getStatus() != TangemCard.Status.NotPersonalized) {
            try {
                TLV tlvCardPubkicKey = readResult.getTLV(TLV.Tag.TAG_CardPublicKey);
                if (tlvCardPubkicKey == null)
                    throw new TangemException("Invalid answer format");

                mCard.setCardPublicKey(tlvCardPubkicKey.Value);

                TLVList tlvCardData = TLVList.fromBytes(readResult.getTLV(TLV.Tag.TAG_CardData).Value);

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
                    Log.e(logTag, "Cannot get firmware version");
                    mCard.setFirmwareVersion("0.00");
                }

                try {
                    if (mCard.getFirmwareVersion().compareTo("1.05") < 0) {
                        // In FW ver 1.05, the issuer has one key pair used as both DataKey and TransactionKey, see [1] 3.3.1 and [1] 3.3.2
                        mCard.setIssuer(tlvCardData.getTLV(TLV.Tag.TAG_Issuer_ID).getAsString(), readResult.getTLV(TLV.Tag.TAG_Issuer_Transaction_PublicKey).Value);
                    } else {
                        // In newer FW versions, the issuer has two different key pairs - DataKey and TransactionKey, see [1] 3.3.1 and [1] 3.3.2
                        mCard.setIssuer(tlvCardData.getTLV(TLV.Tag.TAG_Issuer_ID).getAsString(), readResult.getTLV(TLV.Tag.TAG_Issuer_Data_PublicKey).Value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(logTag, "Cannot get issuer, try a version for older cards");
                    try {
                        // for very very old cards Issuer key can be stored in different TLV tag
                        mCard.setIssuer(tlvCardData.getTLV(TLV.Tag.TAG_Issuer_ID).getAsString(), readResult.getTLV(TLV.Tag.TAG_Issuer_Transaction_PublicKey).Value);
                    } catch (Exception ee) {
                        ee.printStackTrace();
                        Log.e(logTag, "Cannot get issuer");
                        mCard.setIssuer(Issuer.Unknown().getID(), null);
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
                        LocalStorage localStorage = new LocalStorage(mContext);
                        localStorage.applySubstitution(mCard);
                    }
                } catch (Exception e) {
                    Log.e(logTag, "Can't apply card data substitution");
                    e.printStackTrace();
                }

                mCard.setBlockchainIDFromCard(tlvCardData.getTLV(TLV.Tag.TAG_Blockchain_ID).getAsString());

                try {
                    mCard.setSettingsMask(readResult.getTagAsInt(TLV.Tag.TAG_SettingsMask));
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(logTag, "Can't get settings mask");
                }

                if (readResult.getTLV(TLV.Tag.TAG_PauseBeforePIN2) != null) {
                    mCard.setPauseBeforePIN2(10 * readResult.getTagAsInt(TLV.Tag.TAG_PauseBeforePIN2));
                }

                try {
                    mCard.setSigningMethod(readResult.getTagAsInt(TLV.Tag.TAG_SigningMethod));
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(logTag, "Can't get signing method");
                    mCard.setSigningMethod(0);
                }

                try {
                    mCard.setMaxSignatures(readResult.getTagAsInt(TLV.Tag.TAG_MaxSignatures));
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(logTag, "Can't get max signatures");
                }

            } catch (Exception e) {
                e.printStackTrace();
                throw new TangemException("Can't parse card data");
            }
        }


        // Parse additional parameters for Loaded cards: wallet public key, remaining signatures, etc
        if (mCard.getStatus() == TangemCard.Status.Loaded) {

            TLV tlvPublicKey = readResult.getTLV(TLV.Tag.TAG_Wallet_PublicKey);

            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
            ECPoint p1 = spec.getCurve().decodePoint(tlvPublicKey.Value);

            byte pkUncompressed[] = p1.getEncoded(false);

            byte pkCompresses[] = p1.getEncoded(true);
            mCard.setWalletPublicKey(pkUncompressed);
            mCard.setWalletPublicKeyRar(pkCompresses);

            TangemContext ctx = new TangemContext(mCard);
            try {
                // TODO - store in mCard only public key, not wallet address, move wallet address to coinData
                CoinEngine engineCoin = CoinEngineFactory.INSTANCE.create(ctx);
                if (engineCoin == null) throw new Exception("Can't create CoinEngine!");
                String wallet = engineCoin.calculateAddress(pkUncompressed);
                mCard.setWallet(wallet);
            } catch (Exception e) {
                e.printStackTrace();
                throw new TangemException("Can't define wallet address");
            }
            //mCard.setWallet(Blockchain.calculateWalletAddress(mCard, pkUncompressed));

            mCard.setRemainingSignatures(readResult.getTagAsInt(TLV.Tag.TAG_RemainingSignatures));

            if (readResult != null && readResult.getTLV(TLV.Tag.TAG_SignedHashes) != null) {
                mCard.setSignedHashes(readResult.getTagAsInt(TLV.Tag.TAG_SignedHashes));
            }

        } else {
            mCard.setWallet("N/A");
        }
    }

    /**
     * VERIFY_CARD command and verify card's signature
     * By using standard challenge-response scheme, the card proves
     * possession of CARD_PRIVATE_KEY that corresponds to CARD_PUBLIC_KEY returned by READ_CARD command
     * See [1] 3.2, 8.9
     *
     * @return TLVList with response in case of success
     * @throws Exception - if something went wrong
     */
    public TLVList run_VerifyCard() throws Exception {
        if (mCard.getCardPublicKey() == null || readResult == null) {
            run_Read();
        }
        if (mCard.getStatus() == TangemCard.Status.NotPersonalized) {
            getCard().setManufacturer(Manufacturer.Unknown, false);
            return null;
        }

        if (mCard.getCardPublicKey() == null) {
            throw new TangemException("Not all data read, can't verify card!");
        }

        CommandApdu rqApdu = StartPrepareCommand(INS.VerifyCard);
        byte[] bChallenge = Util.generateRandomBytes(16);
        rqApdu.addTLV(TLV.Tag.TAG_Challenge, bChallenge);
        Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

        ResponseApdu rspApdu = SendAndReceive(rqApdu, false);

        if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {
            TLVList verifyResult = rspApdu.getTLVs();
            Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
            verifyResult.add(new TLV(TLV.Tag.TAG_Challenge, bChallenge));

            TLV tlvSalt = verifyResult.getTLV(TLV.Tag.TAG_Salt);
            TLV tlvCardSignature = verifyResult.getTLV(TLV.Tag.TAG_CardSignature);

            if (tlvSalt == null || tlvCardSignature == null) {
                throw new TangemException("Not all data read, can't verify card!");
            }

            try {
                ByteArrayOutputStream bs = new ByteArrayOutputStream();
                bs.write(bChallenge);
                bs.write(tlvSalt.Value);
                byte[] dataArray = bs.toByteArray();
                if (CardCrypto.VerifySignature(mCard.getCardPublicKey(), dataArray, tlvCardSignature.Value)) {
                    getCard().setCardPublicKeyValid(true);
                    Log.i(logTag, "Card signature verification OK");
                } else {
                    Log.e(logTag, "Card signature verification FAILED");
                    getCard().setCardPublicKeyValid(false);
                }

                getCard().setManufacturer(Manufacturer.FindManufacturer(readResult.getTLV(TLV.Tag.TAG_Manufacture_ID).getAsString()), true);
            } catch (Exception e) {
                e.printStackTrace();
                getCard().setManufacturer(Manufacturer.Unknown, false);
            }
            return verifyResult;
        } else if (rspApdu.isStatus(SW_PIN_ERROR)) {
            throw new TangemException_InvalidPIN(String.format("FAILED: [%04X] - Invalid PIN\n", rspApdu.getSW1SW2()));
        } else {
            getCard().setManufacturer(Manufacturer.Unknown, false);
            throw new TangemException(String.format("Failed: %04X", rspApdu.getSW1SW2()));
        }

    }

    /**
     * CREATE_WALLET command
     * This command will create a new wallet on the card having ‘Empty’ state. A key pair WALLET_PUBLIC_KEY / WALLET_PRIVATE_KEY is generated and securely stored in
     * the card.
     * See [1] 3.4, 8.3
     *
     * @param PIN2 - PIN2 code to confirm operation
     * @throws Exception - if something went wrong
     */
    public void run_CreateWallet(String PIN2) throws Exception {
        if (readResult == null) run_Read();
        CommandApdu rqApdu = StartPrepareCommand(INS.CreateWallet);
        rqApdu.addTLV(TLV.Tag.TAG_PIN2, Util.calculateSHA256(PIN2));
        Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

        ResponseApdu rspApdu = SendAndReceive(rqApdu, false);

        if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {
            Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
            if (PIN2.equals(DefaultPIN2)) {
                mCard.setUseDefaultPIN2(true);
            }
        } else if (rspApdu.isStatus(SW_PIN_ERROR)) {
            if (PIN2.equals(DefaultPIN2)) {
                mCard.setUseDefaultPIN2(false);
            }
            throw new TangemException_InvalidPIN(String.format("FAILED: [%04X] - Invalid PIN\n", rspApdu.getSW1SW2()));
        } else {
            throw new TangemException(String.format("Failed: %04X", rspApdu.getSW1SW2()));
        }
    }

    /**
     * CHECK_WALLET command without signature verification
     * Card will sign a challenge to prove that it possesses WALLET_PRIVATE_KEY corresponding to WALLET_PUBLIC_KEY. Standard challenge/response scheme is used.
     * See [1] 3.4, 8.4
     *
     * @return TLVList from answer in case of success
     * @throws Exception - if something went wrong
     */
    private TLVList run_CheckWallet() throws Exception {
        CommandApdu rqApdu = StartPrepareCommand(INS.CheckWallet);
        byte[] bChallenge = Util.generateRandomBytes(16);
        rqApdu.addTLV(TLV.Tag.TAG_Challenge, bChallenge);
        Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

        ResponseApdu rspApdu = SendAndReceive(rqApdu, false);

        if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {
            TLVList Result = rspApdu.getTLVs();
            Result.add(new TLV(TLV.Tag.TAG_Challenge, bChallenge));
            Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
            return Result;
        } else if (rspApdu.isStatus(SW_PIN_ERROR)) {
            throw new TangemException_InvalidPIN(String.format("FAILED: [%04X] - Invalid PIN\n", rspApdu.getSW1SW2()));
        } else {
            throw new TangemException(String.format("Failed: %04X", rspApdu.getSW1SW2()));
        }
    }

    /**
     * CHECK_WALLET command and verify wallet's signature
     * Card will sign a challenge to prove that it possesses WALLET_PRIVATE_KEY corresponding to WALLET_PUBLIC_KEY. Standard challenge/response scheme is used.
     * It will first run READ command if no reads where made before. Then execute CHECK_WALLET command and verify signature in the response.
     * Set WalletPublicKeyValid in {@link TangemCard}
     * See [1] 3.4, 8.4
     *
     * @throws Exception - if something went wrong
     */
    public void run_CheckWalletWithSignatureVerify() throws Exception {
        if (mCard.getCardPublicKey() == null || readResult == null) {
            run_Read();
        }
        if (mCard.getStatus() == TangemCard.Status.NotPersonalized) {
            getCard().setManufacturer(Manufacturer.Unknown, false);
            return;
        }

        if (readResult.getTagAsInt(TLV.Tag.TAG_Status) != TangemCard.Status.Loaded.getCode()) {
            throw new TangemException("Card must be loaded");
        }
        TLVList checkResult = run_CheckWallet();
        if (checkResult == null) return;

        TLV tlvPublicKey = readResult.getTLV(TLV.Tag.TAG_Wallet_PublicKey);
        TLV tlvChallenge = checkResult.getTLV(TLV.Tag.TAG_Challenge);
        TLV tlvSalt = checkResult.getTLV(TLV.Tag.TAG_Salt);
        TLV tlvSignature = checkResult.getTLV(TLV.Tag.TAG_Signature);

        if (tlvPublicKey == null || tlvChallenge == null || tlvSalt == null || tlvSignature == null) {
            throw new TangemException("Not all data read, can't check signature!");
        }

        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        bs.write(tlvChallenge.Value);
        bs.write(tlvSalt.Value);
        byte[] dataArray = bs.toByteArray();

        if (CardCrypto.VerifySignature(tlvPublicKey.Value, dataArray, tlvSignature.Value)) {
            Log.i(logTag, "Signature verification OK");
            mCard.setWalletPublicKeyValid(true);
        } else {
            mCard.setWalletPublicKeyValid(false);
        }
    }

    /**
     * PURGE_WALLET command
     * See [1] 3.4, 8.11
     * This command deletes all wallet data. If Is_Reusable flag is enabled during personalization, the card changes state to ‘Empty’ and a new wallet can be created by
     * CREATE_WALLET command. If Is_Reusable flag is disabled, the card switches to ‘Purged’ state. ‘Purged’ state is final, it makes the card useless.
     *
     * @param PIN2 - PIN2 code to confirm operation
     * @throws Exception - if something went wrong
     */
    public void run_PurgeWallet(String PIN2) throws Exception {
        CommandApdu rqApdu = StartPrepareCommand(INS.PurgeWallet);
        rqApdu.addTLV(TLV.Tag.TAG_PIN2, Util.calculateSHA256(PIN2));

        Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

        ResponseApdu rspApdu = SendAndReceive(rqApdu, false);

        if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {
            Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
            if (PIN2.equals(DefaultPIN2)) {
                mCard.setUseDefaultPIN2(true);
            }
        } else if (rspApdu.isStatus(SW_PIN_ERROR)) {
            if (PIN2.equals(DefaultPIN2)) {
                mCard.setUseDefaultPIN2(false);
            }
            throw new TangemException_InvalidPIN(String.format("FAILED: [%04X] - Invalid PIN\n", rspApdu.getSW1SW2()));
        } else {

            throw new TangemException(String.format("Failed: %04X", rspApdu.getSW1SW2()));
        }
    }

    /**
     * Execute SET_PIN command
     * This command changes PIN1 and PIN2 passwords if it is allowed by Allow_SET_PIN1 and Allow_SET_PIN2 flags in Settings_Mask. Host application can submit
     * unchanged passwords (New_PIN1 = PIN1 and New_PIN2 = PIN2) in order to check its correctness. Depending on the result, Status_Word in the command response will have
     * these values:
     * SW_PINS_NOT_CHANGED = 0x9000
     * SW_PIN1_CHANGED = 0x9001
     * SW_PIN2_CHANGED = 0x9002
     * SW_PINS_CHANGED = 0x9003
     *
     * @param PIN2             - PIN2 code to confirm operation
     * @param newPin           - new value of PIN code
     * @param newPin2          - new value of PIN2 code
     * @param breakOnNeedPause - flag that specify what
     * @throws Exception - if something went wrong
     */
    public void run_SetPIN(String PIN2, String newPin, String newPin2, boolean breakOnNeedPause) throws Exception {
        CommandApdu rqApdu = StartPrepareCommand(INS.SwapPIN);
        rqApdu.addTLV(TLV.Tag.TAG_PIN2, Util.calculateSHA256(PIN2));
        rqApdu.addTLV(TLV.Tag.TAG_NewPIN, Util.calculateSHA256(newPin));
        rqApdu.addTLV(TLV.Tag.TAG_NewPIN2, Util.calculateSHA256(newPin2));

        Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

        ResponseApdu rspApdu = SendAndReceive(rqApdu, breakOnNeedPause);

        if (rspApdu.isStatus(SW.PIN1_CHANGED) || rspApdu.isStatus(SW.PIN2_CHANGED) || rspApdu.isStatus(SW.PINS_CHANGED) || rspApdu.isStatus(SW.PINS_NOT_CHANGED)) {
            Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
            if (newPin2.equals(DefaultPIN2)) {
                mCard.setUseDefaultPIN2(true);
            } else {
                mCard.setUseDefaultPIN2(false);
            }
        } else if (rspApdu.isStatus(SW_PIN_ERROR)) {
            if (PIN2.equals(DefaultPIN2)) {
                mCard.setUseDefaultPIN2(false);
            }
            throw new TangemException_InvalidPIN(String.format("FAILED: [%04X] - Invalid PIN\n", rspApdu.getSW1SW2()));
        } else if (breakOnNeedPause && rspApdu.isStatus(SW.NEED_PAUSE)) {
            throw new TangemException_NeedPause(String.format("FAILED: [%04X] - Need pause\n", rspApdu.getSW1SW2()));
        } else {
            throw new TangemException(String.format("Failed: %04X", rspApdu.getSW1SW2()));
        }
    }

    /**
     * Verify if default PIN2 is set on the card and enable UseDefaultPIN2 flag {@link TangemCard}
     * Works in firmware 1.12 and later, for older version UseDefaultPIN2 is always null
     * This function NEVER triggers security delay
     *
     * @throws Exception - if something went wrong
     */
    public void run_CheckPIN2isDefault() throws Exception {
        // can obtain SetPIN(to default) answer without security delay - try check if PIN2 is default with card request
        if (mCard.isFirmwareNewer("1.19") || (mCard.isFirmwareNewer("1.12") && (mCard.getPauseBeforePIN2() == 0 || mCard.useSmartSecurityDelay()))) {
            try {
                run_SetPIN(DefaultPIN2, mPIN, DefaultPIN2, true);
                mCard.setUseDefaultPIN2(true);
            } catch (TangemException_NeedPause e) {
                mCard.setUseDefaultPIN2(null);
            } catch (TangemException_InvalidPIN e) {
                mCard.setUseDefaultPIN2(false);
            }
        } else {
            mCard.setUseDefaultPIN2(null);
        }
    }

    /**
     * SIGN command to sign hashes - SigningMethod=0,2,4 (see {@link TangemCard.SigningMethod})
     * See [1] 8.6
     * @param PIN2                - PIN2 code to confirm operation
     * @param hashes              - array of digests to sign (max 10 digest at a time)
     * @param UseIssuerValidation - if card need issuer validation before sign (true for SigningMethod=2,4)
     * @param issuerData          - new issuerData to write on card (only for SigningMethod=4, null for other)
     * @param issuer              - issuer (need only for SigningMethod=2,4)
     * @return TLVList with card answer contained wallet signatures of digests from hashes array (in case of success)
     * @throws Exception - if something went wrong
     */
    // TODO - change function to run_SignHashes(String PIN2, byte[][] hashes, byte[] issuerData, byte[] issuerTransactionSignature) because normally issuer signature can be obtained only by external server
    public TLVList run_SignHashes(String PIN2, byte[][] hashes, boolean UseIssuerValidation, byte[] issuerData, Issuer issuer) throws Exception {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        if (hashes.length > 10) throw new Exception("To much hashes in one transaction!");
        for (int i = 0; i < hashes.length; i++) {
            if (i != 0 && hashes[0].length != hashes[i].length)
                throw new Exception("Hashes length must be identical!");
            bs.write(hashes[i]);
        }
        CommandApdu rqApdu = StartPrepareCommand(INS.Sign);
        rqApdu.addTLV(TLV.Tag.TAG_PIN2, Util.calculateSHA256(PIN2));
        rqApdu.addTLV_U8(TLV.Tag.TAG_TrOut_HashSize, hashes[0].length);
        rqApdu.addTLV(TLV.Tag.TAG_TrOut_Hash, bs.toByteArray());
        if (UseIssuerValidation) {
            byte[] issuerSignature = CardCrypto.Signature(issuer.getPrivateTransactionKey(), bs.toByteArray());
            rqApdu.addTLV(TLV.Tag.TAG_Issuer_Transaction_Signature, issuerSignature);
        }
        if (issuerData != null) {
            if (issuer == null || issuer == Issuer.Unknown())
                throw new Exception("Need known Issuer to write issuer Data");
            rqApdu.addTLV(TLV.Tag.TAG_Issuer_Data, issuerData);
            byte[] issuerSignature = CardCrypto.Signature(issuer.getPrivateTransactionKey(), issuerData);
            rqApdu.addTLV(TLV.Tag.TAG_Issuer_Data_Signature, issuerSignature);
        }


        Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

        ResponseApdu rspApdu = SendAndReceive(rqApdu, false);

        if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {
            TLVList Result = rspApdu.getTLVs();
            Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
            if (PIN2.equals(DefaultPIN2)) {
                mCard.setUseDefaultPIN2(true);
            }
            return Result;
        } else if (rspApdu.isStatus(SW_PIN_ERROR)) {
            if (PIN2.equals(DefaultPIN2)) {
                mCard.setUseDefaultPIN2(false);
            }
            throw new TangemException_InvalidPIN(String.format("FAILED: [%04X] - Possible the PIN or PIN2 is invalid!\n", rspApdu.getSW1SW2()));
        } else {
            throw new TangemException(String.format("Failed: %04X", rspApdu.getSW1SW2()));
        }
    }

    /**
     * SIGN raw tx - SigningMethod=1 (see {@link TangemCard.SigningMethod})
     * See [1] 8.6
     * @param PIN2 - PIN2 code to confirm operation
     * @param bTxOutData - part of raw transaction to sign
     * @return TLVList with card answer contained wallet signatures of bTxOutData(in case of success)
     * @throws Exception - if something went wrong
     */
    // TODO - change function to run_SignRaw(String PIN2, String hashAlgID, byte[] bTxOutData, byte[] issuerData, byte[] issuerTransactionSignature) to support all signing methods
    public TLVList run_SignRaw(String PIN2, byte[] bTxOutData) throws Exception {

        CommandApdu rqApdu = StartPrepareCommand(INS.Sign);
        rqApdu.addTLV(TLV.Tag.TAG_PIN2, Util.calculateSHA256(PIN2));
        rqApdu.addTLV(TLV.Tag.TAG_TrOut_Raw, bTxOutData);
        rqApdu.addTLV(TLV.Tag.TAG_HashAlgID, "sha-256x2".getBytes("US-ASCII"));
        Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

        ResponseApdu rspApdu = SendAndReceive(rqApdu, false);

        if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {
            TLVList Result = rspApdu.getTLVs();
            Result.add(new TLV(TLV.Tag.TAG_TrOut_Raw, bTxOutData));
            Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
            if (PIN2.equals(DefaultPIN2)) {
                mCard.setUseDefaultPIN2(true);
            }
            return Result;
        } else if (rspApdu.isStatus(SW_PIN_ERROR)) {
            if (PIN2.equals(DefaultPIN2)) {
                mCard.setUseDefaultPIN2(false);
            }
            throw new TangemException_InvalidPIN(String.format("FAILED: [%04X] - Possible the PIN or PIN2 is invalid!\n", rspApdu.getSW1SW2()));
        } else {
            throw new TangemException(String.format("Failed: %04X", rspApdu.getSW1SW2()));
        }
    }

    /**
     * VERIFY_CODE command
     * See [1] 8.8
     * This command challenges the card to prove integrity of COS binary code. For this purpose, the host application should have a special ‘hash library’ publicly provided
     * by Tangem. It may contains ~150.000 precalculated hashes of COS binary code segments.
     * VERIFY_CODE command internally reads a segment of COS binary code beginning at Code_Page_Address and having length of [64 x Code_Page_Count] bytes.
     * Then it appends Challenge to the code segment, calculates resulting hash and returns it in the response.
     * The application needs to ensure that returned hash coincides with the one stored in the hash library (see {@link Firmwares}).
     * @param hashAlgID - ‘sha-256’, ‘sha-1’, ‘sha-224’, ‘sha-384’, ‘sha-512’, ‘crc-16’
     * @param codePageAddress - Value from 0 to ~3000, take from {@link Firmwares}
     * @param codePageCount - Number of 32-byte pages to read: from 1 to 5, take from {@link Firmwares}
     * @param challenge - Additional challenge value from 1 to 10, take from {@link Firmwares}
     * @return digest bytes to compare with one stored in {@link Firmwares}
     * @throws Exception - if something went wrong
     */
    public byte[] run_VerifyCode(String hashAlgID, int codePageAddress, int codePageCount, byte[] challenge) throws Exception {
        if (readResult == null) run_Read();
        CommandApdu rqApdu = StartPrepareCommand(INS.VerifyCode);
        rqApdu.addTLV(TLV.Tag.TAG_HashAlgID, hashAlgID.getBytes("US-ASCII"));
        rqApdu.addTLV_U32(TLV.Tag.TAG_CodePageAddress, codePageAddress);
        rqApdu.addTLV_U16(TLV.Tag.TAG_CodePageCount, codePageCount);
        rqApdu.addTLV(TLV.Tag.TAG_Challenge, challenge);

        Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

        ResponseApdu rspApdu = SendAndReceive(rqApdu, false);

        if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {
            Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
            return rspApdu.getTLVs().getTLV(TLV.Tag.TAG_CodeHash).Value;
        } else {
            throw new TangemException(String.format("Failed: %04X", rspApdu.getSW1SW2()));
        }
    }

    /**
     * VALIDATE_CARD command
     * See [1] 3.2, 8.10
     * This is an optional command that the issuer can support if there is a real risk of mass counterfeiting by making multiple clones of a single card. This can be the case for
     * transferrable Tangem cards that are almost never redeemed by users.
     * The issuer has to have a back-end service storing and updating a counter value (Card_Validation_Counter) for each card (CID). This function should also be supported
     * by the issuer’s application.
     * The application may occasionally call VALIDATE_CARD command to ensure that there’s only one card having this CID is circulating out there. VALIDATE_CARD
     * will increase COS internal Card_Validation_Counter by 1 and sign the new value with CARD_PRIVATE_KEY. Then the application should submit increased
     * Card_Validation_Counter and its signature to issuer’s card validation back-end (server). The server should verify the signature and update Card_Validation_Counter value if
     * previous value is less than the new one. If the server reveals that submitted Card_Validation_Counter value is less than previous value, then the card having this CID is
     * deemed compromised and should not be accepted by the application.
     * @param PIN2 - PIN2 code to confirm operation
     * @throws Exception - if something went wrong
     */
    private void run_ValidateCard(String PIN2) throws Exception {
        if (readResult == null) run_Read();
        CommandApdu rqApdu = StartPrepareCommand(INS.ValidateCard);
        rqApdu.addTLV(TLV.Tag.TAG_PIN2, Util.calculateSHA256(PIN2));

        Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

        ResponseApdu rspApdu = SendAndReceive(rqApdu, false);

        if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {
            Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
            if (PIN2.equals(DefaultPIN2)) {
                mCard.setUseDefaultPIN2(true);
            }
        } else if (rspApdu.isStatus(SW_PIN_ERROR)) {
            if (PIN2.equals(DefaultPIN2)) {
                mCard.setUseDefaultPIN2(false);
            }
            throw new TangemException_InvalidPIN(String.format("FAILED: [%04X] - Possible the PIN or PIN2 is invalid!\n", rspApdu.getSW1SW2()));
        } else {
            throw new TangemException(String.format("Failed: %04X", rspApdu.getSW1SW2()));
        }
    }

    /**
     * WRITE_ISSUER_DATA command
     * This command re-writes Issuer_Data data block (max 512 bytes) and its issuer’s signature.
     * Issuer_Data is never changed or parsed from within the Tangem COS. The issuer defines purpose of use, format and payload of Issuer_Data.
     * For example, this field may contain information about wallet balance signed by the issuer or additional issuer’s attestation data
     * @param issuerData - new issuerData
     * @param issuerSignature - signature of issuerData with IssuerDataKey
     * @throws Exception - if something went wrong
     */
    private void run_WriteIssuerData(byte[] issuerData, byte[] issuerSignature) throws Exception {
        run_Read();

        CommandApdu rqApdu = StartPrepareCommand(INS.WriteIssuerData);
        rqApdu.addTLV(TLV.Tag.TAG_Issuer_Data, issuerData);
        rqApdu.addTLV(TLV.Tag.TAG_Issuer_Data_Signature, issuerSignature);

        Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

        ResponseApdu rspApdu = SendAndReceive(rqApdu, false);

        if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {
            Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
        } else {
            throw new TangemException(String.format("Failed: %04X", rspApdu.getSW1SW2()));
        }
    }

    /**
     * GET_ISSUER_DATA command and verify verify issuer signature of returned data
     * See [1] 3.3, 8.7
     * This command returns Issuer_Data data block and its issuer’s signature.
     * @return TLVList with issuerData (if success read and verify)
     * @throws Exception - if something went wrong
     */
    private TLVList run_GetIssuerData() throws Exception {
        CommandApdu rqApdu = StartPrepareCommand(INS.GetIssuerData);

        Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

        ResponseApdu rspApdu = SendAndReceive(rqApdu, false);

        if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {
            Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
            TLV issuerData = rspApdu.getTLVs().getTLV(TLV.Tag.TAG_Issuer_Data);
            TLV issuerDataSignature = rspApdu.getTLVs().getTLV(TLV.Tag.TAG_Issuer_Data_Signature);
            TLV issuerDataCounter = rspApdu.getTLVs().getTLV(TLV.Tag.TAG_Issuer_Data_Counter);

            boolean protectIssuerDataAgainstReplay = (readResult.getTagAsInt(TLV.Tag.TAG_SettingsMask) & SettingsMask.ProtectIssuerDataAgainstReplay) != 0;

            if (issuerData == null || issuerDataSignature == null)
                throw new TangemException("Invalid answer format (GetIssuerData)");

            ByteArrayOutputStream bsDataToVerify = new ByteArrayOutputStream();
            bsDataToVerify.write(mCard.getCID());
            bsDataToVerify.write(issuerData.Value);
            if (protectIssuerDataAgainstReplay) {
                bsDataToVerify.write(issuerDataCounter.Value);
            }
            try {
                if (CardCrypto.VerifySignature(mCard.getIssuer().getPublicDataKey(), bsDataToVerify.toByteArray(), issuerDataSignature.Value)) {
                    mCard.setIssuerData(issuerData.Value, issuerDataSignature.Value);
                    return TLVList.fromBytes(issuerData.Value);
                } else {
                    throw new TangemException("Invalid issuer data read (signature verification failed)");
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new TangemException("Invalid issuer data read");
            }
        } else {
            throw new TangemException(String.format("Failed: %04X", rspApdu.getSW1SW2()));
        }
    }

    /**
     * Execute consecutive READ commands with increasing encryption level from EncryptionMode.None to EncryptionMode.Strong, see {@link TangemCard.EncryptionMode}
     * If card requires stricter encryption level it returns SW.NEED_ENCRYPTION status word
     * Once READ is successfully executed - save answer to {@see readResult}, save current PIN and encryption mode to {@link TangemCard} and return
     * @throws Exception - if something went wrong
     */
    public void run_GetSupportedEncryption() throws Exception {
        mCard.encryptionMode = TangemCard.EncryptionMode.None;
        do {
            CommandApdu rqApdu = StartPrepareCommand(INS.Read);
            Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

            ResponseApdu rspApdu = SendAndReceive(rqApdu, false);

            if (rspApdu.isStatus(SW.NEED_ENCRYPTION)) {
                if (mCard.encryptionMode == TangemCard.EncryptionMode.None) {
                    mCard.encryptionMode = TangemCard.EncryptionMode.Fast;
                } else if (mCard.encryptionMode == TangemCard.EncryptionMode.Fast) {
                    mCard.encryptionMode = TangemCard.EncryptionMode.Strong;
                } else {
                    throw new Exception("Can't get supported encryption methods");
                }
            } else if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {
                Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
                readResult = rspApdu.getTLVs();
                mCard.setPIN(mPIN);
                break;
            } else {
                break;
            }
        } while (true);
    }

}