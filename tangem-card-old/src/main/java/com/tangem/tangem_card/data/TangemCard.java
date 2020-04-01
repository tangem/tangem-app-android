package com.tangem.tangem_card.data;

import com.tangem.tangem_card.reader.CardProtocol;
import com.tangem.tangem_card.reader.ProductMask;
import com.tangem.tangem_card.reader.SettingsMask;
import com.tangem.tangem_card.reader.TLV;
import com.tangem.tangem_card.reader.TLVList;
import com.tangem.tangem_card.util.Util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.EnumSet;

/**
 * Created by dvol on 16.07.2017.
 */

public class TangemCard {

    private byte[] CID;
    private Status status;
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
    private byte[] terminalPrivateKey;
    private byte[] terminalPublicKey;
    private boolean terminalIsLinked = false;
    private byte[] tagSignature = null;
    private byte[] idHash = null;

    public String getBlockchainID() {
        return blockchainID;
    }

    public void setBlockchainID(String blockchainID) {
        this.blockchainID = blockchainID;
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

//    public void switchToInitialBlockchain() {
//        if (tokenSymbol.length() > 1)
//            blockchainID = Blockchain.Token.getID(); // Reset blockchain to Token from ETH for token cards with zero token balance on it
//    }

    public Date getPersonalizationDateTime() {
        return dtPersonalization;
    }

    public void setPersonalizationDateTime(Date dtPersonalization) {
        this.dtPersonalization = dtPersonalization;
    }

    public byte[] getTerminalPrivateKey() {
        return terminalPrivateKey;
    }

    public void setTerminalPrivateKey(byte[] terminalPrivateKey) {
        this.terminalPrivateKey = terminalPrivateKey;
    }

    public byte[] getTerminalPublicKey() {
        return terminalPublicKey;
    }

    public void setTerminalPublicKey(byte[] terminalPublicKey) {
        this.terminalPublicKey = terminalPublicKey;
    }

    public boolean getTerminalIsLinked() {
        return terminalIsLinked;
    }

    public void setTerminalIsLinked(boolean terminalIsLinked) {
        this.terminalIsLinked = terminalIsLinked;
    }

    public byte[] getIdHash() {
        return idHash;
    }

    public void setIdHash(byte[] idHash) {
        this.idHash = idHash;
    }

    private int health = 0;

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        if (health > this.health) this.health = health;
    }

    public boolean isHealthOK() {
        return health == 0;
    }

    private byte[] issuerPublicDataKey = null;
    private String issuerID = null;


    public byte[] getIssuerPublicDataKey() {
        return issuerPublicDataKey;
    }

    private Issuer issuer = Issuer.Unknown();

    public void setIssuer(String issuerID, byte[] issuerPublicDataKey) {
        this.issuerPublicDataKey = issuerPublicDataKey;
        this.issuerID = issuerID;
        this.issuer = Issuer.FindIssuer(issuerID, issuerPublicDataKey);
    }

    public Issuer getIssuer() {
        return issuer;
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
        return !(tokenSymbol == null || tokenSymbol.isEmpty());
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

    private byte[] issuerDataEx;
    private byte[] issuerDataExSignature;
    private int issuerDataExCounter;

    public byte[] getIssuerDataEx() {
        return issuerDataEx;
    }

    public byte[] getIssuerDataExSignature() {
        return issuerDataExSignature;
    }

    public int getIssuerDataExCounter() {
        return issuerDataExCounter;
    }

    public void setIssuerDataEx(byte[] value, byte[] signature, int counter) {
        issuerDataEx = value;
        issuerDataExSignature = signature;
        issuerDataExCounter = counter;
    }

    private int pauseBeforePIN2 = 0;

    public void setPauseBeforePIN2(int value) {
        this.pauseBeforePIN2 = value;
    }

    public int getPauseBeforePIN2() {
        return pauseBeforePIN2;
    }

    private Integer settingsMask = null;

    public Integer getSettingsMask() {
        return settingsMask;
    }

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

    public Boolean forbidPurgeWallet() {
        if (settingsMask == null) return null;
        return (settingsMask & SettingsMask.ForbidPurgeWallet) != 0;
    }

    public Boolean needCVC() {
        if (settingsMask == null) return null;
        return (settingsMask & SettingsMask.UseCVC) != 0;
    }

    public Boolean useSmartSecurityDelay() {
        if (settingsMask == null) return null;
        return (settingsMask & SettingsMask.SmartSecurityDelay) != 0;
    }

    public Boolean useDefaultPIN1() {
        return CardProtocol.isDefaultPIN(getPIN());
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

    public Boolean supportLinkingTerminal() {
        if (settingsMask == null) return null;
        return (settingsMask & SettingsMask.SkipSecurityDelayIfValidatedByLinkedTerminal) != 0;
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
        Sign_Raw_Validated_By_Issuer(3, "sign raw tx validated by issuer"),
        Sign_Hash_Validated_By_Issuer_And_WriteIssuerData(4, "sign hash validated by issuer and write issuer data"),
        Sign_Raw_Validated_By_Issuer_And_WriteIssuerData(5, "sign raw tx validated by issuer and write issuer data");

        public int ID;
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

    public EnumSet<SigningMethod> allowedSigningMethod = EnumSet.noneOf(SigningMethod.class);

    public void setSigningMethod(int signingMethodID) {
        allowedSigningMethod.clear();
        this.signingMethod = null;
        if ((signingMethodID & 0x80) != 0) {
            this.signingMethod = null;
            for (int i = 0; i < 6; i++) {
                if ((signingMethodID & (0x01 << i)) != 0) {
                    allowedSigningMethod.add(SigningMethod.FindByID(i));
                }
            }
            if (allowedSigningMethod.contains(SigningMethod.Sign_Hash)) {
                signingMethod = SigningMethod.Sign_Hash;
            } else if (allowedSigningMethod.contains(SigningMethod.Sign_Raw)) {
                signingMethod = SigningMethod.Sign_Raw;
            } else if (allowedSigningMethod.contains(SigningMethod.Sign_Hash_Validated_By_Issuer)) {
                signingMethod = SigningMethod.Sign_Hash_Validated_By_Issuer;
            } else if (allowedSigningMethod.contains(SigningMethod.Sign_Raw_Validated_By_Issuer)) {
                signingMethod = SigningMethod.Sign_Raw_Validated_By_Issuer;
            } else if (allowedSigningMethod.contains(SigningMethod.Sign_Hash_Validated_By_Issuer_And_WriteIssuerData)) {
                signingMethod = SigningMethod.Sign_Hash_Validated_By_Issuer_And_WriteIssuerData;
            } else if (allowedSigningMethod.contains(SigningMethod.Sign_Raw_Validated_By_Issuer_And_WriteIssuerData)) {
                signingMethod = SigningMethod.Sign_Raw_Validated_By_Issuer_And_WriteIssuerData;
            } else {
                signingMethod = SigningMethod.Sign_Hash;
            }
        } else {
            allowedSigningMethod.clear();
            allowedSigningMethod.add(SigningMethod.FindByID(signingMethodID));
            this.signingMethod = SigningMethod.FindByID(signingMethodID);
        }
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

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public int productMask = ProductMask.Note;

    public void setProductMask(int productMask) {
        this.productMask = productMask;
    }

    public boolean isIDCard() {
        try {
            return (productMask & ProductMask.IdCard) != 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public TLVList tlvIDCardData = null;

    public void setTlvIdCardData(TLVList tlvIDCardData) {
        this.tlvIDCardData = tlvIDCardData;
    }

    public void setTlvIDCardData(
            String fullName,
            String birthday,
            String gender,
            byte[] photo,
            String issueDate,
            String expireDate,
            String trustedAddress) {

        IDCardData data = new IDCardData();
        data.fullName = fullName;
        data.birthday = birthday;
        data.gender = gender;
        data.photo = photo;
        data.issueDate = issueDate;
        data.expireDate = expireDate;
        data.trustedAddress = trustedAddress;

        this.tlvIDCardData = data.toTLVList();
    }

    public boolean hasIDCardData() {
        if (tlvIDCardData == null) {
            if (issuerDataEx == null) return false;
            try {
                tlvIDCardData = TLVList.fromBytes(issuerDataEx);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        return tlvIDCardData.hasTag(TLV.Tag.TAG_FullName) && tlvIDCardData.hasTag(TLV.Tag.TAG_Birthday) && tlvIDCardData.hasTag(TLV.Tag.TAG_Gender) && tlvIDCardData.hasTag(TLV.Tag.TAG_Photo)
                && tlvIDCardData.hasTag(TLV.Tag.TAG_IssueDate) && tlvIDCardData.hasTag(TLV.Tag.TAG_ExpireDate) && tlvIDCardData.hasTag(TLV.Tag.TAG_TrustedAddress);
    }

    public static class IDCardData {
        public String fullName;
        public String birthday;
        public String gender;
        public byte[] photo;
        public String issueDate;
        public String expireDate;
        public String trustedAddress;

        public TLVList toTLVList() {
            TLVList tlvIDCardData = new TLVList();
            tlvIDCardData.add(new TLV(TLV.Tag.TAG_FullName, fullName.getBytes(StandardCharsets.UTF_8)));
            tlvIDCardData.add(new TLV(TLV.Tag.TAG_Birthday, birthday.getBytes(StandardCharsets.UTF_8)));
            tlvIDCardData.add(new TLV(TLV.Tag.TAG_Gender, gender.getBytes(StandardCharsets.UTF_8)));
            tlvIDCardData.add(new TLV(TLV.Tag.TAG_Photo, photo));
            tlvIDCardData.add(new TLV(TLV.Tag.TAG_IssueDate, issueDate.getBytes(StandardCharsets.UTF_8)));
            tlvIDCardData.add(new TLV(TLV.Tag.TAG_ExpireDate, expireDate.getBytes(StandardCharsets.UTF_8)));
            tlvIDCardData.add(new TLV(TLV.Tag.TAG_TrustedAddress, trustedAddress.getBytes(StandardCharsets.UTF_8)));
            return tlvIDCardData;
        }

        public static IDCardData fromTLVList(TLVList tlvIDCardData) {
            IDCardData result = new IDCardData();
            result.fullName = tlvIDCardData.getTLV(TLV.Tag.TAG_FullName).getAsString();
            result.birthday = tlvIDCardData.getTLV(TLV.Tag.TAG_Birthday).getAsString();
            result.gender = tlvIDCardData.getTLV(TLV.Tag.TAG_Gender).getAsString();
            result.photo = tlvIDCardData.getTLV(TLV.Tag.TAG_Photo).Value;
            result.issueDate = tlvIDCardData.getTLV(TLV.Tag.TAG_IssueDate).getAsString();
            result.expireDate = tlvIDCardData.getTLV(TLV.Tag.TAG_ExpireDate).getAsString();
            result.trustedAddress = tlvIDCardData.getTLV(TLV.Tag.TAG_TrustedAddress).getAsString();
            return result;
        }
    }

    public IDCardData getIDCardData() {
        if (!hasIDCardData()) return null;
        return IDCardData.fromTLVList(tlvIDCardData);
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

    public byte[] getTagSignature() {
        return tagSignature;
    }

    public void setTagSignature(byte[] tagSignature) {
        this.tagSignature = tagSignature;
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