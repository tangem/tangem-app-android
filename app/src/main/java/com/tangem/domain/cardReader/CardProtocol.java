package com.tangem.domain.cardReader;

import android.content.Context;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.IsoDep;
import android.util.Log;

import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.CoinEngineFactory;
import com.tangem.domain.wallet.Issuer;
import com.tangem.domain.wallet.LocalStorage;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.domain.wallet.Manufacturer;
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
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;

import javax.crypto.KeyAgreement;

/**
 * Created by dvol on 14.07.2017.
 */

public class CardProtocol {

    public interface Notifications {
        void onReadStart(CardProtocol cardProtocol);

        void onReadProgress(CardProtocol cardProtocol, int progress);

        void onReadFinish(CardProtocol cardProtocol);

        void onReadCancel();

        void onReadWait(int msec);

        void onReadBeforeRequest(int timeout);

        void onReadAfterRequest();
    }

    public void setError(Exception error) {
        mError = error;
    }

    public TangemCard getCard() {
        return mCard;
    }

    public Exception getError() {
        return mError;
    }

    private static final String logTag = "CardProtocol";
    public static final int SW_PIN_ERROR = SW.INVALID_PARAMS;

    public static final String DefaultPIN = "000000";
    public static final String DefaultPIN2 = "000";

    protected IsoDep mIsoDep;

    public Tag getTag() {
        return mIsoDep.getTag();
    }

    protected String mPIN;
    protected Notifications mNotifications;

    public void setPIN(String PIN) {
        mPIN = PIN;
        if (mCard != null) {
            mCard.setPIN(PIN);
        }
    }

    protected TangemCard mCard;
    protected Exception mError;
    protected Context mContext;

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public CardProtocol(Context context, IsoDep isoDep, Notifications notifications) {
        mContext = context;
        mIsoDep = isoDep;
        mNotifications = notifications;
        mPIN = null;
        mCard = new TangemCard(Util.byteArrayToHexString(mIsoDep.getTag().getId()));
    }

    public CardProtocol(Context context, IsoDep isoDep, TangemCard card, Notifications notifications) {
        mContext = context;
        mIsoDep = isoDep;
        mNotifications = notifications;
        mPIN = card.getPIN();
        mCard = card;
    }

    public static class TangemException extends Exception {
        public TangemException(String message) {
            super(message);
        }
    }

    public static class TangemException_InvalidPIN extends TangemException {
        public TangemException_InvalidPIN(String message) {
            super(message);
        }
    }

    public static class TangemException_NeedPause extends TangemException {
        public TangemException_NeedPause(String message) {
            super(message);
        }
    }

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

    public byte[] GetUID() {
        if (mIsoDep == null || mIsoDep.getTag() == null) return null;
        return mIsoDep.getTag().getId();
    }

    public int getTimeout() {
        if (mIsoDep == null || mIsoDep.getTag() == null) return 60000;
        return mIsoDep.getTimeout();
    }


    protected byte[] protocolKey;

    public void resetProtocolKey() {
        protocolKey = null;
    }

    public void CreateProtocolKey() throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        protocolKey = CardCrypto.pbkdf2(Util.calculateSHA256(mPIN), mIsoDep.getTag().getId(), 50);
        //Log.e("Reader", String.format("PIN: %s, Protocol key: %s", mPIN, Util.bytesToHex(protocolKey)));
        if (sessionKey != null) {
            sessionKey = null;
        }
    }

    byte[] sessionKey = null;

    public void run_OpenSession(TangemCard.EncryptionMode encryptionMode) throws Exception {
        sessionKey = null;
        try {
            CommandApdu cmdApdu = new CommandApdu(CommandApdu.ISO_CLA, INS.OpenSession.Code, 0, encryptionMode.getP());

            switch (encryptionMode) {
                case Fast: {
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

    private CommandApdu StartPrepareCommand(INS ins) throws NoSuchAlgorithmException {
        CommandApdu Apdu = new CommandApdu(ins);
        byte[] baPIN = Util.calculateSHA256(mPIN);
        Apdu.addTLV(TLV.Tag.TAG_PIN, baPIN);
        if (ins != INS.Read) {
            Apdu.addTLV(TLV.Tag.TAG_CardID, mCard.getCID());
        }
        return Apdu;
    }

    public void run_Read() throws Exception {
        run_Read(true);
    }

    public void run_Read(boolean parseResult) throws Exception {
        CommandApdu rqApdu = StartPrepareCommand(INS.Read);
        Log.i(logTag, String.format("[%s]\n%s", rqApdu.getCommandName(), rqApdu.getTLVs().getParsedTLVs("  ")));

        ResponseApdu rspApdu = SendAndReceive(rqApdu, false);

        if (rspApdu.isStatus(SW.PROCESS_COMPLETED)) {


            Log.i(logTag, String.format("OK: [%04X]\n%s", rspApdu.getSW1SW2(), rspApdu.getTLVs().getParsedTLVs("  ")));


            readResult = rspApdu.getTLVs();

            if (parseResult) {
                parseReadResult();
                run_ReadWriteIssuerData();
            } else {
                //TODO: проверить что карта та же
            }

        } else if (rspApdu.isStatus(SW_PIN_ERROR)) {
            throw new TangemException_InvalidPIN(String.format("FAILED: [%04X] - Possible PIN is invalid!\n", rspApdu.getSW1SW2()));
        } else {
            throw new TangemException(String.format("FAILED: [%04X]\n", rspApdu.getSW1SW2()));
        }
    }

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
                tlvIssuerData = run_ReadIssuerData();
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

    private TLVList readResult = null;

    public void clearReadResult() {
        sessionKey = null;
        readResult = null;
    }

    public boolean haveReadResult() {
        return readResult != null;
    }

    public void parseReadResult() throws TangemException, NoSuchProviderException, NoSuchAlgorithmException {
        TLV tlvStatus = readResult.getTLV(TLV.Tag.TAG_Status);
        mCard.setStatus(TangemCard.Status.fromCode(Util.byteArrayToInt(tlvStatus.Value)));
        TLV tlvCID = readResult.getTLV(TLV.Tag.TAG_CardID);
        mCard.setCID(tlvCID.Value);
        mCard.setManufacturer(Manufacturer.FindManufacturer(readResult.getTLV(TLV.Tag.TAG_Manufacture_ID).getAsString()), true);
        mCard.setHealth(readResult.getTLV(TLV.Tag.TAG_Health).getAsInt());

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

                // Hardcoded smart contracts for the cards manufactured before registration of smart-contract in ETH


                if (contractAddress != null)
                {
                    mCard.setContractAddress(contractAddress.getAsString());
                }

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
                    if (readResult.getTLV(TLV.Tag.TAG_Firmware) != null) {
                        mCard.setFirmwareVersion(readResult.getTLV(TLV.Tag.TAG_Firmware).getAsString());
                    } else {
                        mCard.setFirmwareVersion(tlvCardData.getTLV(TLV.Tag.TAG_Firmware).getAsString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(logTag, "Cannot get firmware version");
                    mCard.setFirmwareVersion("0.00");
                }

                try {
                    if (mCard.getFirmwareVersion().compareTo("1.05") < 0) {
                        mCard.setIssuer(tlvCardData.getTLV(TLV.Tag.TAG_Issuer_ID).getAsString(), readResult.getTLV(TLV.Tag.TAG_Issuer_Transaction_PublicKey).Value);
                    } else {
                        mCard.setIssuer(tlvCardData.getTLV(TLV.Tag.TAG_Issuer_ID).getAsString(), readResult.getTLV(TLV.Tag.TAG_Issuer_Data_PublicKey).Value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(logTag, "Cannot get issuer, try a version for older cards");
                    try {
                        mCard.setIssuer(tlvCardData.getTLV(TLV.Tag.TAG_Issuer_ID).getAsString(), readResult.getTLV(TLV.Tag.TAG_Issuer_Transaction_PublicKey).Value);
                    } catch (Exception ee) {
                        ee.printStackTrace();
                        Log.e(logTag, "Cannot get issuer");
                        mCard.setIssuer(Issuer.Unknown().getID(), null);
                    }
                }

                // substitutions for card, that was produced with wrong blockchain data (for example token contract was unknown)
                // this method must be called after set issuer because substitution is verified by issuer data key
                try {
                    if( mCard.getBatch().equals("0017") )
                    {
                        mCard.setContractAddress("0x9Eef75bA8e81340da9D8d1fd06B2f313DB88839c");
                    }
                    else if( mCard.getBatch().equals("0019") )
                    {
                        mCard.setContractAddress("0x0c056b0cda0763cc14b8b2d6c02465c91e33ec72");
                    } else {
                        LocalStorage localStorage = new LocalStorage(mContext);
                        localStorage.applySubstitution(mCard);
                    }
                }catch(Exception e)
                {
                    Log.e(logTag, "Can't apply card data substitution");
                    e.printStackTrace();
                }

                // this method has reflection TokenSymbol
                // you mast call setBlockchainIDFromCard after calling setTokensXXX and make substitutions
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


        if (mCard.getStatus() == TangemCard.Status.Loaded) {

            TLV tlvPublicKey = readResult.getTLV(TLV.Tag.TAG_Wallet_PublicKey);

            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
            ECPoint p1 = spec.getCurve().decodePoint(tlvPublicKey.Value);

            byte pkUncompressed[] = p1.getEncoded(false);

            byte pkCompresses[] = p1.getEncoded(true);
            mCard.setWalletPublicKey(pkUncompressed);
            mCard.setWalletPublicKeyRar(pkCompresses);

            CoinEngine engineCoin = CoinEngineFactory.create(mCard.getBlockchain());
            String wallet = engineCoin.calculateAddress(mCard, pkUncompressed);
            mCard.setWallet(wallet);
            //mCard.setWallet(Blockchain.calculateWalletAddress(mCard, pkUncompressed));

            mCard.setRemainingSignatures(readResult.getTagAsInt(TLV.Tag.TAG_RemainingSignatures));

            if (readResult != null && readResult.getTLV(TLV.Tag.TAG_SignedHashes) != null) {
                mCard.setSignedHashes(readResult.getTagAsInt(TLV.Tag.TAG_SignedHashes));
            }

        } else {
            mCard.setWallet("N/A");
        }
    }

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
//            TLV tlvManufacturerSignature = verifyResult.getTLV(TLV.Tag.TAG_Manufacturer_Signature);

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

                //getCard().setManufacturer(Manufacturer.FindManufacturer(readResult.getTLV(TLV.Tag.TAG_Manufacture_ID).getAsString(), bChallenge, tlvSalt.Value, tlvManufacturerSignature.Value), true);
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

    public TLVList run_CheckWallet() throws Exception {
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

    public void run_SwapPIN(String PIN2, String newPin, String newPin2, boolean breakOnNeedPause) throws Exception {
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

    public void run_CheckPIN2isDefault() throws Exception {
        if (mCard.isFirmwareNewer("1.19") || (mCard.isFirmwareNewer("1.12") && (mCard.getPauseBeforePIN2() == 0 || mCard.useSmartSecurityDelay()))) {
            // can obtain SwapPIN(to default) answer without security delay - try check if PIN2 is default with card request
            try {
                run_SwapPIN(DefaultPIN2, mPIN, DefaultPIN2, true);
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

    private TLVList run_ReadIssuerData() throws Exception {
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