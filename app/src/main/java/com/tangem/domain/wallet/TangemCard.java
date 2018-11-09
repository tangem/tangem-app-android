package com.tangem.domain.wallet;

import android.os.Bundle;
import android.util.Log;

import com.google.common.base.Strings;
import com.tangem.data.db.PINStorage;
import com.tangem.domain.cardReader.SettingsMask;
import com.tangem.util.Util;

import java.util.Date;

/**
 * Created by dvol on 16.07.2017.
 */

public class TangemCard {

    public static final String EXTRA_CARD = "Card";
    public static final String EXTRA_UID = "UID";

    private byte[] CID;
    private Status status;
    private String wallet;
    private String UID;
    private String blockchainID;
    private Manufacturer manufacturer = Manufacturer.Unknown;
    private boolean manufacturerConfirmed = false;
    private int maxSignatures;
    private int remainingSignatures;
    private String PIN;
    private byte[] pbWalletKey = null;
    private byte[] pbCardKey = null;
    private byte[] pbWalletKeyRar = null;
    private Date dtPersonalization = null;

    public String getBlockchainID() {
        return blockchainID;
    }

    public Blockchain getBlockchain() {
        return Blockchain.fromId(blockchainID);
    }

    public void setBlockchain(Blockchain blockchain) {
        blockchainID=blockchain.getID();
    }

    public void setBlockchainID(String blockchainID) {
        this.blockchainID = blockchainID;
    }

    public void addTokenToBlockchainName() {
        String token = getTokenSymbol();
        if (Strings.isNullOrEmpty(token))
            return;
        String oldName = getBlockchain().getOfficialName();
        //String newName = String.format("%s - %s ERC20 token", token, oldName);
        String newName = token + " <br><small><small> " + oldName + " ERC20 token</small></small>";
        blockchainName=newName;

    }

    private String blockchainName = "";

    public String getBlockchainName() {
        if (Strings.isNullOrEmpty(blockchainName))
            return getBlockchain().getOfficialName();
        return blockchainName;
    }

    public void setBlockchainIDFromCard(String blockchainID) {

        if (Blockchain.fromId(blockchainID) != Blockchain.Ethereum && Blockchain.fromId(blockchainID) != Blockchain.EthereumTestNet)
            this.blockchainID = blockchainID;

        if (isToken()) {
            this.blockchainID = Blockchain.Token.getID();
            addTokenToBlockchainName();
        } else {
            this.blockchainID = blockchainID;
        }
    }

    public void setWalletPublicKey(byte[] publicKey) {
        pbWalletKey = publicKey;
    }

    public void setWalletPublicKeyRar(byte[] publicKey) {
        pbWalletKeyRar = publicKey;
    }

    public byte[] getWalletPublicKey() {
        return pbWalletKey;
    }

    public byte[] getWalletPublicKeyRar() {
        return pbWalletKeyRar;
    }

    private boolean walletPublicKeyValid = false;

    public void setWalletPublicKeyValid(boolean walletPublicKeyValid) {
        this.walletPublicKeyValid = walletPublicKeyValid;
    }

    public boolean isWalletPublicKeyValid() {
        return walletPublicKeyValid;
    }

    public void setCardPublicKey(byte[] publicKey) {
        pbCardKey = publicKey;
    }

    public byte[] getCardPublicKey() {
        return pbCardKey;
    }

    private boolean cardPublicKeyValid = false;

    public void setCardPublicKeyValid(boolean cardPublicKeyValid) {
        this.cardPublicKeyValid = cardPublicKeyValid;
    }

    public boolean isCardPublicKeyValid() {
        return cardPublicKeyValid;
    }


    public Manufacturer getManufacturer() {
        return manufacturer;
    }

    public boolean isManufacturerConfirmed() {
        return manufacturerConfirmed;
    }

    public void setManufacturer(Manufacturer manufacturer, boolean verified) {
        if (this.manufacturer == manufacturer) {
            this.manufacturerConfirmed |= verified;
        } else {
            this.manufacturer = manufacturer;
            this.manufacturerConfirmed = verified;
        }
    }

    private Boolean codeConfirmed;

    public void setCodeConfirmed(Boolean codeConfirmed) {
        this.codeConfirmed = codeConfirmed;
    }

    public Boolean isCodeConfirmed() {
        return codeConfirmed;
    }

    private Boolean onlineVerified;

    public void setOnlineVerified(Boolean verified) {
        this.onlineVerified = verified;
    }

    public Boolean isOnlineVerified() {
        return onlineVerified;
    }

    private Boolean onlineValidated;

    public void setOnlineValidated(Boolean validated) {
        this.onlineValidated = validated;
    }

    public Boolean isOnlineValidated() {
        return onlineValidated;
    }

    public int getRemainingSignatures() {
        return remainingSignatures;
    }

    public void setRemainingSignatures(int remainingSignatures) {
        this.remainingSignatures = remainingSignatures;
    }

    public String getPIN() {
        return PIN;
    }

    public void setPIN(String PIN) {
        this.PIN = PIN;
    }

    public void switchToInitialBlockchain() {
        if (tokenSymbol.length() > 1)
            blockchainID = Blockchain.Token.getID(); // Reset blockchain to Token from ETH for token cards with zero token balance on it
    }

    public Date getPersonalizationDateTime() {
        return dtPersonalization;
    }

    public void setPersonalizationDateTime(Date dtPersonalization) {
        this.dtPersonalization = dtPersonalization;
    }

    public String getPersonalizationDateTimeDescription() {
        return Util.formatDate(dtPersonalization);
    }

    private int health = 0;

    public void setHealth(int health) {
        if (health > this.health) this.health = health;
    }

    public boolean isHealthOK() {
        return health == 0;
    }

    private Issuer issuer = Issuer.Unknown();
    private byte[] issuerPublicDataKey = null;

//    public void setIssuer(Issuer issuer) {
//        this.issuer = issuer;
//    }

    public void setIssuer(String issuerID, byte[] issuerPublicDataKey) {
        this.issuerPublicDataKey = issuerPublicDataKey;
        this.issuer = Issuer.FindIssuer(issuerID, issuerPublicDataKey);
    }

    public Issuer getIssuer() {
        return issuer;
    }

    public byte[] getIssuerPublicDataKey() {
        return issuerPublicDataKey;
    }

    public String getIssuerDescription() {
        return issuer.getOfficialName();
    }

    String contractAddress = "";

    public void setContractAddress(String address) {
        contractAddress = address;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public String tokenSymbol = "";

    public void setTokenSymbol(String symbol) {
        tokenSymbol = symbol;
    }

    public String getTokenSymbol() {
        return tokenSymbol;
    }

    public boolean isToken() {
        return !Strings.isNullOrEmpty(tokenSymbol);
    }

    int tokensDecimal = 18;

    public void setTokensDecimal(int tokensDecimal) {
        this.tokensDecimal = tokensDecimal;
    }

    public int getTokensDecimal() {
        return tokensDecimal;
    }


    private byte[] issuerData;
    private byte[] issuerDataSignature;

    public byte[] getIssuerData() {
        return issuerData;
    }

    public byte[] getIssuerDataSignature() {
        return issuerDataSignature;
    }

    public void setIssuerData(byte[] value, byte[] signature) {
        issuerData = value;
        issuerDataSignature = signature;
    }

    private boolean needWriteIssuerData = false;

    public boolean getNeedWriteIssuerData() {
        return needWriteIssuerData;
    }

    public void setNeedWriteIssuerData(boolean value) {
        needWriteIssuerData = value;
    }

    public String getIssuerDataDescription() {
        return "";
    }

    private int pauseBeforePIN2 = 0;

    public void setPauseBeforePIN2(int value) {
        this.pauseBeforePIN2 = value;
    }

    public int getPauseBeforePIN2() {
        return pauseBeforePIN2;
    }

    private Integer settingsMask = null;

    public void setSettingsMask(int settingsMask) {
        this.settingsMask = settingsMask;
    }

    public Boolean isReusable() {
        if (settingsMask == null) return null;
        return (settingsMask & SettingsMask.IsReusable) != 0;
    }

    public Boolean allowSwapPIN() {
        if (settingsMask == null) return null;
        return (settingsMask & SettingsMask.AllowSwapPIN) != 0;
    }

    public Boolean allowSwapPIN2() {
        if (settingsMask == null) return null;
        return (settingsMask & SettingsMask.AllowSwapPIN2) != 0;
    }

    public Boolean needCVC() {
        if (settingsMask == null) return null;
        return (settingsMask & SettingsMask.UseCVC) != 0;
    }

    public Boolean useDefaultPIN1() {
        return PINStorage.isDefaultPIN(getPIN());
    }

    public Boolean useSmartSecurityDelay() {
        if (settingsMask == null) return null;
        return (settingsMask & SettingsMask.SmartSecurityDelay) != 0;
    }

    public enum PIN2_Mode {Unchecked, DefaultPIN2, CustomPIN2}

    public PIN2_Mode PIN2 = PIN2_Mode.Unchecked;

    public Boolean useDefaultPIN2() {
        if (PIN2 == PIN2_Mode.DefaultPIN2 || (PIN2 == PIN2_Mode.Unchecked && (needCVC() || (getPauseBeforePIN2() > 0)))) {
            // define that we use default PIN2 if we try it or not try and security delay or CVC is used
            return true;
        } else {
            return false;
        }
    }

    public void setUseDefaultPIN2(Boolean value) {
        if (value != null) {
            PIN2 = value ? PIN2_Mode.DefaultPIN2 : PIN2_Mode.CustomPIN2;
        } else {
            PIN2 = PIN2_Mode.Unchecked;
        }
    }

    public Boolean supportNDEF() {
        if (settingsMask == null) return null;
        return (settingsMask & SettingsMask.UseNDEF) != 0;
    }

    public Boolean supportOnlyOneCommandAtTime() {
        if (settingsMask == null) return null;
        return supportNDEF() && ((settingsMask & SettingsMask.UseOneCommandAtTime) != 0);
    }

    public Boolean supportDynamicNDEF() {
        if (settingsMask == null) return null;
        return supportNDEF() && ((settingsMask & SettingsMask.UseDynamicNDEF) != 0);
    }

    public Boolean supportBlock() {
        if (settingsMask == null) return null;
        return (settingsMask & SettingsMask.UseBlock) != 0;
    }

    public int getMaxSignatures() {
        return maxSignatures;
    }

    public void setMaxSignatures(int value) {
        maxSignatures = value;
    }

    private String firmwareVersion;

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public Boolean useDevelopersFirmware() {
        return getFirmwareVersion().endsWith("d") || getFirmwareVersion().endsWith("SDK");
    }

    private static String getFirmwareVersionNumber(String version) throws Exception {
        if (version == null || version.length() < 4) {
            throw new Exception("Firmware version has unsupported format!");
        }
        if (version.endsWith("d SDK")) {
            return version.substring(0, version.length() - 5);
        } else if (version.endsWith("r")) {
            return version.substring(0, version.length() - 1);
        } else {
            return version;
        }
    }

    private static int[] getFirmwareVersionNumbers(String version) throws Exception {
        String fwNumber = getFirmwareVersionNumber(version);
        String[] strNumbers = fwNumber.split("\\.");

        if (strNumbers.length != 2) throw new Exception("Firmware version has unsupported format!");
        try {
            int major = Integer.parseInt(strNumbers[0]), minor = Integer.parseInt(strNumbers[1]);
            return new int[]{major, minor};
        } catch (NumberFormatException e) {
            e.printStackTrace();
            throw new Exception("Firmware version has unsupported format!");
        }
    }

    public Boolean isFirmwareOlder(String version) throws Exception {
        int[] numbers1 = getFirmwareVersionNumbers(firmwareVersion), numbers2 = getFirmwareVersionNumbers(version);
        return numbers1[0] < numbers2[0] || (numbers1[0] == numbers2[0] && numbers1[1] < numbers2[1]);
    }

    public Boolean isFirmwareNewer(String version) throws Exception {
        int[] numbers1 = getFirmwareVersionNumbers(firmwareVersion), numbers2 = getFirmwareVersionNumbers(version);
        return numbers1[0] > numbers2[0] || (numbers1[0] == numbers2[0] && numbers1[1] > numbers2[1]);
    }

    private String batch;

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public String getBatch() {
        return batch;
    }

    public enum SigningMethod {
        Sign_Hash(0, "sign hash"),
        Sign_Raw(1, "sign raw tx"),
        Sign_Hash_Validated_By_Issuer(2, "sign hash validated by issuer"),
        Sign_Raw_Validated_By_Issuer(3, "sign raw tx validated by issuer");

        int ID;
        String mDescription;

        SigningMethod(int ID, String description) {
            this.ID = ID;
            mDescription = description;
        }

        static SigningMethod FindByID(int ID) {
            SigningMethod[] methods = values();
            for (SigningMethod m : methods) {
                if (m.ID == ID) return m;
            }
            return SigningMethod.Sign_Hash;
        }

        public String getDescription() {
            return mDescription;
        }
    }

    private SigningMethod signingMethod;

    public void setSigningMethod(int signingMethodID) {
        this.signingMethod = SigningMethod.FindByID(signingMethodID);
    }

    public SigningMethod getSigningMethod() {
        return signingMethod;
    }

    public TangemCard(String UID) {
        this.UID = UID;
    }


    public enum Status {
        NotPersonalized(0), Empty(1), Loaded(2), Purged(3);

        Status(int Code) {
            mCode = Code;
        }

        private int mCode;

        public int getCode() {
            return mCode;
        }

        public static Status fromCode(int code) {
            for (Status s : values()) {
                if (s.getCode() == code) return s;
            }
            return null;
        }
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public byte[] getCID() {
        return CID;
    }

    public void setCID(byte[] value) {
        this.CID = value;
    }

    public String getCIDDescription() {
        String strCID = Util.bytesToHex(CID);
        try {
            return strCID.substring(0, 4) + " " + strCID.substring(4, 8) + " " + strCID.substring(8, 12) + " " + strCID.substring(12, 16);
        } catch (Exception e) {
            return strCID;
        }
    }

    public void setWallet(String wallet) {
        this.wallet = wallet;
    }

    public String getWallet() {
        return wallet;
    }

    public String getShortWalletString() {
        if (wallet.length() < 22) {
            return wallet;
        } else {
            return wallet.substring(0, 10) + "......" + wallet.substring(wallet.length() - 10, wallet.length());
        }
    }

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }


    private byte[] offlineBalance;

    public void setOfflineBalance(byte[] offlineBalance) {
        this.offlineBalance = offlineBalance;
    }

    public byte[] getOfflineBalance() {
        return offlineBalance;
    }

    public void clearOfflineBalance() {
        offlineBalance = null;
    }

    private byte[] Denomination;
    private String DenominationText;

    public void setDenomination(byte[] denomination, String denominationText) {
        this.Denomination = denomination;
        this.DenominationText = denominationText;
    }

    public void setDenomination(byte[] denomination) {
        this.Denomination = denomination;
        try {
            CoinEngine engine= CoinEngineFactory.INSTANCE.create(getBlockchain());
            CoinEngine.InternalAmount internalAmount=engine.convertToInternalAmount(denomination);
            CoinEngine.Amount amount=engine.convertToAmount(internalAmount);
            this.DenominationText = amount.toString();
        } catch (Exception e) {
            e.printStackTrace();
            this.DenominationText = "N/A";
        }
    }

    public byte[] getDenomination() {
        return Denomination;
    }

    public int SignedHashes = -1; // Will remain -1 if tag was not found on card (= not safe to accept)

    public void setSignedHashes(int SignedHashes) {
        this.SignedHashes = SignedHashes;
    }

    public int getSignedHashes() {
        return SignedHashes;
    }


    public String getDenominationText() {
        return DenominationText;
    }

    public void setDenominationText(String denominationText) {
        DenominationText = denominationText;
    }

    public void clearDenomination() {
        Denomination = null;
        DenominationText = null;
    }

    public Bundle getAsBundle() {
        Bundle B = new Bundle();
        saveToBundle(B);
        return B;
    }

    public void saveToBundle(Bundle B) {
        try {
            B.putString("UID", UID);
            B.putByteArray("CID", CID);
            B.putString("PIN", PIN);
            B.putString("PIN2", PIN2.name());
            B.putString("Status", status.name());
            B.putString("Blockchain", blockchainID);
            B.putString("BlockchainName", blockchainName);
            B.putInt("TokensDecimal", tokensDecimal);
            B.putString("TokenSymbol", tokenSymbol);
            B.putString("ContractAddress", contractAddress);
            if (dtPersonalization != null) B.putLong("dtPersonalization", dtPersonalization.getTime());
            B.putInt("RemainingSignatures", remainingSignatures);
            B.putInt("MaxSignatures", maxSignatures);
            B.putInt("Health", health);
            if (settingsMask != null) B.putInt("settingsMask", settingsMask);
            B.putInt("pauseBeforePIN2", pauseBeforePIN2);
            if (signingMethod != null) B.putString("signingMethod", signingMethod.name());
            if (manufacturer != null) B.putString("Manufacturer", manufacturer.name());
            if (encryptionMode != null) B.putString("EncryptionMode", encryptionMode.name());
            if (issuer != null) B.putString("Issuer", issuer.getID());
            if (issuerPublicDataKey != null) B.putByteArray("IssuerPublicDataKey", issuerPublicDataKey);
            if (firmwareVersion != null) B.putString("FirmwareVersion", firmwareVersion);
            if (batch != null) B.putString("Batch", batch);
            B.putBoolean("ManufacturerConfirmed", manufacturerConfirmed);
            B.putBoolean("CardPublicKeyValid", isCardPublicKeyValid());
            B.putByteArray("CardPublicKey", getCardPublicKey());

            B.putInt("SignedHashes", getSignedHashes());
            B.putString("Wallet", wallet);
            B.putBoolean("WalletPublicKeyValid", isWalletPublicKeyValid());
            if (pbWalletKey != null)
                B.putByteArray("PublicKey", pbWalletKey);
            if (pbWalletKeyRar != null)
                B.putByteArray("PublicKeyRar", pbWalletKeyRar);

            if (getOfflineBalance() != null) B.putByteArray("OfflineBalance", getOfflineBalance());

            if (getDenomination() != null) B.putByteArray("Denomination", getDenomination());

            if (getIssuerData() != null && getIssuerDataSignature() != null) {
                B.putByteArray("IssuerData", getIssuerData());
                B.putByteArray("IssuerDataSignature", getIssuerDataSignature());
                B.putBoolean("NeedWriteIssuerData", getNeedWriteIssuerData());
            }

            if (codeConfirmed != null)
                B.putBoolean("codeConfirmed", codeConfirmed);

            if (codeConfirmed != null)
                B.putBoolean("codeConfirmed", codeConfirmed);

            if (onlineVerified != null)
                B.putBoolean("onlineVerified", onlineVerified);

            if (onlineValidated != null)
                B.putBoolean("onlineValidated", onlineValidated);

            if (codeConfirmed != null)
                B.putBoolean("codeConfirmed", codeConfirmed);

            if (codeConfirmed != null)
                B.putBoolean("codeConfirmed", codeConfirmed);

            if (onlineVerified != null)
                B.putBoolean("onlineVerified", onlineVerified);

            if (onlineValidated != null)
                B.putBoolean("onlineValidated", onlineValidated);
        } catch (Exception e) {
            Log.e("Can't save to bundle ", e.getMessage());
        }

    }

    public void loadFromBundle(Bundle B) {
        UID = B.getString("UID");
        CID = B.getByteArray("CID");
        PIN = B.getString("PIN");
        PIN2 = PIN2_Mode.valueOf(B.getString("PIN2"));
        status = Status.valueOf(B.getString("Status"));
        wallet = B.getString("Wallet");
        blockchainID = B.getString("Blockchain");
        tokensDecimal = B.getInt("TokensDecimal", 18);
        tokenSymbol = B.getString("TokenSymbol", "");
        contractAddress = B.getString("ContractAddress", "");
        if (B.containsKey("BlockchainName"))
            blockchainName = B.getString("BlockchainName", "");
        if (B.containsKey("dtPersonalization")) {
            dtPersonalization = new Date(B.getLong("dtPersonalization"));
        }
        remainingSignatures = B.getInt("RemainingSignatures");
        maxSignatures = B.getInt("MaxSignatures");
        health = B.getInt("health");
        if (B.containsKey("settingsMask")) settingsMask = B.getInt("settingsMask");
        pauseBeforePIN2 = B.getInt("pauseBeforePIN2");
        if (B.containsKey("signingMethod"))
            signingMethod = SigningMethod.valueOf(B.getString("signingMethod"));
        if (B.containsKey("Manufacturer"))
            manufacturer = Manufacturer.valueOf(B.getString("Manufacturer"));
        manufacturerConfirmed = B.getBoolean("ManufacturerConfirmed");
        if (B.containsKey("EncryptionMode"))
            encryptionMode = EncryptionMode.valueOf(B.getString("EncryptionMode"));
        else
            encryptionMode = null;

        if (B.containsKey("SignedHashes")) setSignedHashes(B.getInt("SignedHashes"));

        if (B.containsKey("Issuer")) issuer = Issuer.FindIssuer(B.getString("Issuer"));
        if (B.containsKey("IssuerPublicDataKey"))
            issuerPublicDataKey = B.getByteArray("IssuerPublicDataKey");

        if (B.containsKey("FirmwareVersion")) firmwareVersion = B.getString("FirmwareVersion");
        if (B.containsKey("Batch")) batch = B.getString("Batch");

        cardPublicKeyValid = B.getBoolean("CardPublicKeyValid");
        if (B.containsKey("CardPublicKey")) setCardPublicKey(B.getByteArray("CardPublicKey"));

        if (B.containsKey("OfflineBalance")) setOfflineBalance(B.getByteArray("OfflineBalance"));
        else clearOfflineBalance();

        if (B.containsKey("Denomination")) setDenomination(B.getByteArray("Denomination"));
        else clearDenomination();

        if (B.containsKey("IssuerData") && B.containsKey("IssuerDataSignature"))
            setIssuerData(B.getByteArray("IssuerData"), B.getByteArray("IssuerDataSignature"));
        else setIssuerData(null, null);

        if (B.containsKey("NeedWriteIssuerData"))
            setNeedWriteIssuerData(B.getBoolean("NeedWriteIssuerData"));

        walletPublicKeyValid = B.getBoolean("WalletPublicKeyValid");
        if (B.containsKey("PublicKey")) {
            pbWalletKey = B.getByteArray("PublicKey");
        }
        if (B.containsKey("PublicKeyRar")) {
            pbWalletKeyRar = B.getByteArray("PublicKeyRar");
        }

        if (B.containsKey("codeConfirmed"))
            codeConfirmed = B.getBoolean("codeConfirmed");

        if (B.containsKey("onlineVerified"))
            onlineVerified = B.getBoolean("onlineVerified");

        if (B.containsKey("onlineValidated"))
            onlineValidated = B.getBoolean("onlineValidated");
    }

    public enum EncryptionMode {
        None((byte) 0x0), Fast((byte) 0x1), Strong((byte) 0x2);

        private byte P;

        EncryptionMode(byte P) {
            this.P = P;
        }

        public int getP() {
            return P;
        }
    }

    public EncryptionMode encryptionMode = EncryptionMode.None;
}