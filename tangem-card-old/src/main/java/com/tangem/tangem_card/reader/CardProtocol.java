package com.tangem.tangem_card.reader;

import com.tangem.tangem_card.data.Issuer;
import com.tangem.tangem_card.data.Manufacturer;
import com.tangem.tangem_card.data.TangemCard;
import com.tangem.tangem_card.util.Log;
import com.tangem.tangem_card.util.Util;

import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.interfaces.ECPublicKey;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

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

    private NfcReader mIsoDep;

    public NfcReader getReader()
    {
        return mIsoDep;
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

    public static boolean isDefaultPIN(String pin) {
        return (DefaultPIN.equals(pin));
    }

    public static boolean isDefaultPIN2(String pin2) {
        return (DefaultPIN2.equals(pin2));
    }

    private byte[] terminalPublicKey;

    public void setTerminalPublicKey(byte[] terminalPubKey) {
        this.terminalPublicKey = terminalPubKey;
    }

    private TangemCard mCard;
    private Exception mError;

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
     * Constructor
     *
     * @param reader        - NFC Reader interface
     * @param card          - TangemCard object, stored data from previous reading or null for reading a card for the first time
     * @param notifications - UI notification callbacks
     */
    public CardProtocol(NfcReader reader, TangemCard card, Notifications notifications) {
        mIsoDep = reader;
        mNotifications = notifications;
        if (card != null) {
            mPIN = card.getPIN();
            mCard = card;
        } else {
            mPIN = null;
            mCard = new TangemCard(Util.byteArrayToHexString(mIsoDep.getId()));
        }
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
     * Base exception class for exceptions on card reading
     */
    public static class TangemException_TagLost extends Exception {
        public TangemException_TagLost() {
            super("Tag lost");
        }

        public TangemException_TagLost(String message) {
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
        if (mIsoDep == null) return null;
        return mIsoDep.getId();
    }

    /**
     * Return ISO 14443-3 tag reading timeout
     */
    public int getTimeout() {
        if (mIsoDep == null) return 60000;
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
                            throw new TangemException_TagLost();
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
                            throw new TangemException_TagLost();
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
     * @param cmdApdu          - APDU command to send
     * @param breakOnNeedPause - Specifies what to do when the card requests a security delay (interrupt transfer or wait till the end of the delay )
     * @return - response APDU
     * @throws Exception - if something went wrong
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
        if (ins == INS.Read) {
            addTerminalPublicKeyToApdu(Apdu);
        } else {
            Apdu.addTLV(TLV.Tag.TAG_CardID, mCard.getCID());
        }
        return Apdu;
    }

    private void addTerminalPublicKeyToApdu(CommandApdu apdu) {
        if (terminalPublicKey != null) {
            apdu.addTLV(TLV.Tag.TAG_Terminal_PublicKey, terminalPublicKey);
        } else if (mCard.getTerminalPublicKey() != null) {
            apdu.addTLV(TLV.Tag.TAG_Terminal_PublicKey, mCard.getTerminalPublicKey());
        }
    }

//    /**
//     * Run READ command and parse answer
//     * {@see run_Read(boolean parseResult) }
//     *
//     * @throws Exception if something went wrong
//     */
//    public void run_Read() throws Exception {
//        run_Read(true);
//    }

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
     * @throws Exception if something went wrong
     */
    public void run_Read() throws Exception {
        CommandApdu rqApdu = StartPrepareCommand(INS.Read);
        Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

        ResponseApdu rspApdu = SendAndReceive(rqApdu, false);

        if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {

            Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
            readResult = rspApdu.getTLVs();
        } else if (rspApdu.isStatus(SW_PIN_ERROR)) {
            throw new TangemException_InvalidPIN(String.format("FAILED: [%04X] - Possible PIN is invalid!\n", rspApdu.getSW1SW2()));
        } else {
            throw new TangemException(String.format("FAILED: [%04X]\n", rspApdu.getSW1SW2()));
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

    public TLVList getReadResult() {
        return readResult;
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
            throw new TangemException("Before run_VerifyCard execute run_Read card first!");
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
        if (readResult == null)
            throw new TangemException("Before run_VerifyCard execute run_Read card first!");
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
            throw new TangemException("Before run_VerifyCard execute run_Read card first!");
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

        TLV tlvCurveID = readResult.getTLV(TLV.Tag.TAG_CurveID);
        TLV tlvPublicKey = readResult.getTLV(TLV.Tag.TAG_Wallet_PublicKey);
        TLV tlvChallenge = checkResult.getTLV(TLV.Tag.TAG_Challenge);
        TLV tlvSalt = checkResult.getTLV(TLV.Tag.TAG_Salt);
        TLV tlvSignature = checkResult.getTLV(TLV.Tag.TAG_Signature);

        if (tlvCurveID == null || tlvPublicKey == null || tlvChallenge == null || tlvSalt == null || tlvSignature == null) {
            throw new TangemException("Not all data read, can't check signature!");
        }

        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        bs.write(tlvChallenge.Value);
        bs.write(tlvSalt.Value);
        byte[] dataArray = bs.toByteArray();

        if (CardCrypto.VerifySignature(tlvCurveID.getAsString(), tlvPublicKey.Value, dataArray, tlvSignature.Value)) {
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
     * SIGN command to sign hashes - SigningMethod=0,2,4 (see {@link TangemCard.SigningMethod})
     * See [1] 8.6
     *
     * @param PIN2                       - PIN2 code to confirm operation
     * @param hashes                     - array of digests to sign (max 10 digest at a time)
     * @param issuerTransactionSignature - signature of hashes, if card need issuer validation before sign (for SigningMethod=2)
     * @param issuerData                 - new issuerData to write on card (only for SigningMethod=4, null for other)
     * @param issuerDataSignature        - signature of issuerData, if issuerData specified(for SigningMethod=4)
     * @return TLVList with card answer contained wallet signatures of digests from hashes array (in case of success)
     * @throws Exception - if something went wrong
     */
    public TLVList run_SignHashes(String PIN2, byte[][] hashes, byte[] issuerTransactionSignature, byte[] issuerData, byte[] issuerDataSignature) throws Exception {
//        if (mCard.getSigningMethod() != TangemCard.SigningMethod.Sign_Hash_Validated_By_Issuer && mCard.getSigningMethod() != TangemCard.SigningMethod.Sign_Hash) {
//            throw new TangemException("Card don't support signing hashes!");
//        }

        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        if (hashes.length > 10) throw new TangemException("To much hashes in one transaction!");
        for (int i = 0; i < hashes.length; i++) {
            if (i != 0 && hashes[0].length != hashes[i].length)
                throw new TangemException("Hashes length must be identical!");
            bs.write(hashes[i]);
        }
        CommandApdu rqApdu = StartPrepareCommand(INS.Sign);
        rqApdu.addTLV(TLV.Tag.TAG_PIN2, Util.calculateSHA256(PIN2));
        rqApdu.addTLV_U8(TLV.Tag.TAG_TrOut_HashSize, hashes[0].length);
        byte[] hashesConcatenated = bs.toByteArray();
        rqApdu.addTLV(TLV.Tag.TAG_TrOut_Hash, hashesConcatenated);
        if (issuerData != null) {
            if (!mCard.allowedSigningMethod.contains(TangemCard.SigningMethod.Sign_Hash_Validated_By_Issuer_And_WriteIssuerData))
                throw new TangemException("Card don't support simultaneous sign with write issuer data!");

            if (issuerDataSignature == null)
                throw new TangemException("Card require issuer validation before write issuer data");
            bs.write(issuerData);
            rqApdu.addTLV(TLV.Tag.TAG_Issuer_Data, issuerData);
            rqApdu.addTLV(TLV.Tag.TAG_Issuer_Data_Signature, issuerDataSignature);
        }
        if (issuerTransactionSignature != null) {
            //byte[] issuerSignature = CardCrypto.Signature(issuer.getPrivateTransactionKey(), bs.toByteArray());
            rqApdu.addTLV(TLV.Tag.TAG_Issuer_Transaction_Signature, issuerTransactionSignature);
        } else if (!mCard.allowedSigningMethod.contains(TangemCard.SigningMethod.Sign_Hash)) {
            throw new TangemException("Card require issuer validation before sign the transaction!");
        }

        prepareDataForLinkingTerminal(rqApdu, hashesConcatenated);

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
     *
     * @param PIN2                       - PIN2 code to confirm operation
     * @param hashAlgID                  - name of hash alg, used for signature
     * @param bTxOutData                 - part of raw transaction to sign
     * @param issuerTransactionSignature - signature of hashes, if card need issuer validation before sign (for SigningMethod=2)
     * @param issuerData                 - new issuerData to write on card (only for SigningMethod=4, null for other)
     * @param issuerDataSignature        - signature of issuerData, if issuerData specified(for SigningMethod=4)
     * @return TLVList with card answer contained wallet signatures of bTxOutData(in case of success)
     * @throws Exception - if something went wrong
     */
    public TLVList run_SignRaw(String PIN2, String hashAlgID, byte[] bTxOutData, byte[] issuerTransactionSignature, byte[] issuerData, byte[] issuerDataSignature) throws Exception {

        CommandApdu rqApdu = StartPrepareCommand(INS.Sign);
        rqApdu.addTLV(TLV.Tag.TAG_PIN2, Util.calculateSHA256(PIN2));
        rqApdu.addTLV(TLV.Tag.TAG_TrOut_Raw, bTxOutData);
        rqApdu.addTLV(TLV.Tag.TAG_HashAlgID, hashAlgID.getBytes("US-ASCII"));
        Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        if (bTxOutData.length > 1024) throw new TangemException("Raw transaction size is to big!");
        bs.write(bTxOutData);

        if (issuerData != null) {
            if (!mCard.allowedSigningMethod.contains(TangemCard.SigningMethod.Sign_Raw_Validated_By_Issuer_And_WriteIssuerData))
                throw new TangemException("Card don't support simultaneous sign with write issuer data!");

            if (issuerDataSignature == null)
                throw new TangemException("Card require issuer validation before write issuer data");
            bs.write(issuerData);
            rqApdu.addTLV(TLV.Tag.TAG_Issuer_Data, issuerData);
            rqApdu.addTLV(TLV.Tag.TAG_Issuer_Data_Signature, issuerDataSignature);
        }
        if (issuerTransactionSignature != null) {
            rqApdu.addTLV(TLV.Tag.TAG_Issuer_Transaction_Signature, issuerTransactionSignature);
        } else if (!mCard.allowedSigningMethod.contains(TangemCard.SigningMethod.Sign_Raw)) {
            throw new TangemException("Card require issuer validation before sign the transaction!");
        }

        prepareDataForLinkingTerminal(rqApdu, bTxOutData);

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

    private void prepareDataForLinkingTerminal(CommandApdu apdu, byte[] data) throws Exception {
        byte[] transactionSignature = CardCrypto.Signature(mCard.getTerminalPrivateKey(), data);
        apdu.addTLV(TLV.Tag.TAG_Terminal_TransactionSignature, transactionSignature);
        addTerminalPublicKeyToApdu(apdu);
    }

    /**
     * VERIFY_CODE command
     * See [1] 8.8
     * This command challenges the card to prove integrity of COS binary code. For this purpose, the host application should have a special ‘hash library’ publicly provided
     * by Tangem. It may contains ~150.000 precalculated hashes of COS binary code segments.
     * VERIFY_CODE command internally reads a segment of COS binary code beginning at Code_Page_Address and having length of [64 x Code_Page_Count] bytes.
     * Then it appends Challenge to the code segment, calculates resulting hash and returns it in the response.
     * The application needs to ensure that returned hash coincides with the one stored in the hash library (see {@see Firmwares}).
     *
     * @param hashAlgID       - ‘sha-256’, ‘sha-1’, ‘sha-224’, ‘sha-384’, ‘sha-512’, ‘crc-16’
     * @param codePageAddress - Value from 0 to ~3000, take from {@see Firmwares}
     * @param codePageCount   - Number of 32-byte pages to read: from 1 to 5, take from {@see Firmwares}
     * @param challenge       - Additional challenge value from 1 to 10, take from {@see Firmwares}
     * @return digest bytes to compare with one stored in {@see Firmwares}
     * @throws Exception - if something went wrong
     */
    public byte[] run_VerifyCode(String hashAlgID, int codePageAddress, int codePageCount, byte[] challenge) throws Exception {
        if (readResult == null)
            throw new TangemException("Before run_VerifyCard execute run_Read card first!");
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
     *
     * @param PIN2 - PIN2 code to confirm operation
     * @throws Exception - if something went wrong
     */
    private void run_ValidateCard(String PIN2) throws Exception {
        if (readResult == null)
            throw new TangemException("Before run_VerifyCard execute run_Read card first!");
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
     *
     * @param issuerData      - new issuerData
     * @param issuerSignature - signature of issuerData with IssuerDataKey
     * @throws Exception - if something went wrong
     */
    public void run_WriteIssuerData(byte[] issuerData, byte[] issuerSignature) throws Exception {
        if (readResult == null)
            throw new TangemException("Before run_VerifyCard execute run_Read card first!");

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
     *
     * @return TLVList with issuerData (if success read and verify)
     * @throws Exception - if something went wrong
     */
    public TLVList run_GetIssuerData() throws Exception {
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

            if( issuerData.Value.length==0 && issuerDataSignature.Value.length==0 )
            {
                mCard.setIssuerData(null, null);
                return new TLVList();
            }

            ByteArrayOutputStream bsDataToVerify = new ByteArrayOutputStream();
            bsDataToVerify.write(mCard.getCID());
            bsDataToVerify.write(issuerData.Value);
            if (protectIssuerDataAgainstReplay) {
                bsDataToVerify.write(issuerDataCounter.Value);
            }
            try {
                if (CardCrypto.VerifySignature(mCard.getIssuerPublicDataKey(), bsDataToVerify.toByteArray(), issuerDataSignature.Value)) {
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
     * Series of GET_ISSUER_DATA command to read extra data and verify issuer signature of returned extra data
     * See [1] 3.3, 8.7
     * This command returns Issuer_Extra_Data data block and its issuer’s signature.
     *
     * @return TLVList with issuerData (if success read and verify)
     * @throws Exception - if something went wrong
     */
    public TLVList run_GetIssuerDataEx(Notifications notifications) throws Exception {
        boolean needReadIssuerDataCounter = (readResult.getTagAsInt(TLV.Tag.TAG_SettingsMask) & SettingsMask.ProtectIssuerDataAgainstReplay) != 0;
        TLV issuerDataSignature = null;
        TLV issuerDataCounter = null;
        TLV issuerDataSize = null;
        ByteArrayOutputStream bsIssuerDataEx = new ByteArrayOutputStream();
        do {

            CommandApdu rqApdu = StartPrepareCommand(INS.GetIssuerData);
            rqApdu.addTLV_U8(TLV.Tag.TAG_Mode, 0x01);
            rqApdu.addTLV_U16(TLV.Tag.TAG_Offset, bsIssuerDataEx.size());

            Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

            ResponseApdu rspApdu = SendAndReceive(rqApdu, false);

            if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {
                Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
                TLV issuerData = rspApdu.getTLVs().getTLV(TLV.Tag.TAG_Issuer_Data);
                if (issuerData != null && issuerData.Value != null)
                    bsIssuerDataEx.write(issuerData.Value);
                issuerDataSignature = rspApdu.getTLVs().getTLV(TLV.Tag.TAG_Issuer_Data_Signature);
                issuerDataCounter = rspApdu.getTLVs().getTLV(TLV.Tag.TAG_Issuer_Data_Counter);
                if (issuerDataSize == null)
                    issuerDataSize = rspApdu.getTLVs().getTLV(TLV.Tag.TAG_Size);
            } else {
                throw new TangemException(String.format("Failed: %04X", rspApdu.getSW1SW2()));
            }
            if (issuerDataSize != null && issuerDataSize.getAsInt() != 0) {
                if (notifications != null)
                    notifications.onReadProgress(this, (int) Math.round(bsIssuerDataEx.size() * 100.0 / issuerDataSize.getAsInt()));
                Log.i(logTag, String.format("Read %d/%d bytes", bsIssuerDataEx.size(), issuerDataSize.getAsInt()));
            }

        } while ((issuerDataSignature == null) || (needReadIssuerDataCounter && (issuerDataCounter == null)));
        boolean protectIssuerDataAgainstReplay = (readResult.getTagAsInt(TLV.Tag.TAG_SettingsMask) & SettingsMask.ProtectIssuerDataAgainstReplay) != 0;

        if (notifications != null) notifications.onReadProgress(this, 100);

        if (bsIssuerDataEx.size() > 0 && issuerDataSignature != null && issuerDataSignature.Value != null) {
            ByteArrayOutputStream bsDataToVerify = new ByteArrayOutputStream();
            bsDataToVerify.write(mCard.getCID());
            bsDataToVerify.write(bsIssuerDataEx.toByteArray());
            if (protectIssuerDataAgainstReplay) {
                bsDataToVerify.write(issuerDataCounter.Value);
            }
            try {
                if (CardCrypto.VerifySignature(mCard.getIssuerPublicDataKey(), bsDataToVerify.toByteArray(), issuerDataSignature.Value)) {
                    mCard.setIssuerDataEx(bsIssuerDataEx.toByteArray(), issuerDataSignature.Value, issuerDataCounter.getAsInt());
                    return TLVList.fromBytes(bsIssuerDataEx.toByteArray());
                } else {
                    throw new TangemException("Invalid issuer data read (signature verification failed)");
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new TangemException("Invalid issuer data read");
            }
        } else {
            mCard.setIssuerDataEx(null, null, issuerDataCounter.getAsInt());
            return null;
        }
    }

    /**
     * Series of WRITE_ISSUER_DATA command to write extra data:
     * 1. start write procedure
     * 2. series of writing data blocks
     * 3. finalize write procedure
     * This command make signatures with provided issuer privateDataKey
     *  - to start write procedure (sign CID|Counter|Size)
     *  - to finalize write procedure (sign CID|dataEx|Counter)
     * See [1] 3.3, 8.8
     *
     * @throws Exception - if something went wrong
     */
    public void run_WriteIssuerDataEx(Notifications notifications, byte[] dataEx, byte[] issuerPrivateDataKey) throws Exception {
        boolean needCounter = readResult.getTLV(TLV.Tag.TAG_SettingsMask) != null && (readResult.getTagAsInt(TLV.Tag.TAG_SettingsMask) & SettingsMask.ProtectIssuerDataAgainstReplay) != 0;

        Log.i(logTag, "WriteIssuerDataEx - Start");

        int issuerDataCounter = 0;
        ByteArrayOutputStream osDataToSign = new ByteArrayOutputStream();
        osDataToSign.write(mCard.getCID());
        if (needCounter) {
            issuerDataCounter = mCard.getIssuerDataExCounter() + 1;
            osDataToSign.write(Util.intToByteArray4(issuerDataCounter));
        }
        osDataToSign.write(Util.intToByteArray2(dataEx.length));

        byte[] issuerDataSignature = CardCrypto.Signature(issuerPrivateDataKey, osDataToSign.toByteArray());

        CommandApdu rqApdu = StartPrepareCommand(INS.WriteIssuerData);
        rqApdu.addTLV_U8(TLV.Tag.TAG_Mode, 0x01);
        if (needCounter) {
            rqApdu.addTLV_U32(TLV.Tag.TAG_Issuer_Data_Counter, issuerDataCounter);
        }
        rqApdu.addTLV_U16(TLV.Tag.TAG_Size, dataEx.length);
        rqApdu.addTLV(TLV.Tag.TAG_Issuer_Data_Signature, issuerDataSignature);

        Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

        ResponseApdu rspApdu = SendAndReceive(rqApdu, true);

        if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {
            Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
        } else {
            throw new TangemException(String.format("Failed: %04X", rspApdu.getSW1SW2()));
        }

        if (notifications != null)
            notifications.onReadProgress(this, 10);


        Log.i(logTag, "WriteIssuerDataEx - Send data");
        int offset = 0;
        int partSize = 1524;
        while (offset < dataEx.length) {

            if (dataEx.length - offset < partSize) {
                partSize = dataEx.length - offset;
            }
            byte[] part = Arrays.copyOfRange(dataEx, offset, offset + partSize);

            rqApdu = StartPrepareCommand(INS.WriteIssuerData);
            rqApdu.addTLV_U8(TLV.Tag.TAG_Mode, 0x02);
            rqApdu.addTLV_U16(TLV.Tag.TAG_Offset, offset);
            rqApdu.addTLV(TLV.Tag.TAG_Issuer_Data, part);

            Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

            rspApdu = SendAndReceive(rqApdu, true);

            if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {
                Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
            } else {
                throw new TangemException(String.format("Failed: %04X", rspApdu.getSW1SW2()));
            }

            offset += partSize;
            if (notifications != null)
                notifications.onReadProgress(this, 10+(int) Math.round(offset * 80.0 / dataEx.length));
        }

        Log.i(logTag, "WriteIssuerDataEx - Finalize");

        osDataToSign = new ByteArrayOutputStream();
        osDataToSign.write(mCard.getCID());
        osDataToSign.write(dataEx);
        if (needCounter) {
            osDataToSign.write(Util.intToByteArray4(issuerDataCounter));
        }
        issuerDataSignature = CardCrypto.Signature(issuerPrivateDataKey, osDataToSign.toByteArray());

        rqApdu = StartPrepareCommand(INS.WriteIssuerData);
        rqApdu.addTLV_U8(TLV.Tag.TAG_Mode, 0x03);
        rqApdu.addTLV(TLV.Tag.TAG_Issuer_Data_Signature, issuerDataSignature);

        Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

        rspApdu = SendAndReceive(rqApdu, true);

        if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {
            Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));
        } else {
            throw new TangemException(String.format("Failed: %04X", rspApdu.getSW1SW2()));
        }
        if (notifications != null)
            notifications.onReadProgress(this, 100);
    }


    /**
     * Execute consecutive READ commands with increasing encryption level from EncryptionMode.None to EncryptionMode.Strong, see {@link TangemCard.EncryptionMode}
     * If card requires stricter encryption level it returns SW.NEED_ENCRYPTION status word
     * Once READ is successfully executed - save answer to {@see readResult}, save current PIN and encryption mode to {@link TangemCard} and return
     *
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