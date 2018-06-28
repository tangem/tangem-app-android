package com.tangem.domain.wallet;

import android.os.Bundle;

import com.google.common.base.Strings;
import com.tangem.domain.cardReader.SettingsMask;
import com.tangem.util.BTCUtils;
import com.tangem.util.FormatUtil;
import com.tangem.util.Util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
    private String error;
    private String message;
    private String balanceDecimal;
    private String balanceDecimalAlter = "";
    private Long balanceConfirmed, balanceUnconfirmed;
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
    private float rate = 0;
    private float rateAlter = 0;

//    public static final int SettingsMask_IsReusable = 0x0001;
//    public static final int SettingsMask_UseRecovery = 0x0002;
//    public static final int SettingsMask_UseBlock = 0x0004;
//
//    public static final int SettingsMask_AllowSwapPIN = 0x0010;
//    public static final int SettingsMask_AllowSwapPIN2 = 0x0020;
//    public static final int SettingsMask_UseCVC = 0x0040;
//
//    public static final int SettingsMask_UseOneCommandAtTime = 0x100;
//    public static final int SettingsMask_UseNDEF = 0x200;
//    public static final int SettingsMask_UseDynamicNDEF = 0x400;

    private BigInteger countConfirmTX = null;

    public BigInteger GetConfirmTXCount() {
        if (countConfirmTX == null) {
            countConfirmTX = BigInteger.valueOf(0);
        }
        return countConfirmTX;
    }

    public void SetConfirmTXCount(BigInteger count) {
        countConfirmTX = count;
    }

    public float getRate() {
        return rate;
    }

    public float getRateAlter() {
        return rateAlter;
    }

    public void setRate(float rate) {
        this.rate = rate;
    }

    public void setRateAlter(float rate) {
        this.rateAlter = rate;
    }

    public String getBlockchainID() {
        return blockchainID;
    }

    public Blockchain getBlockchain() {
        //TODO: hard fork only for test
        //return Blockchain.Ethereum;
        return Blockchain.fromId(blockchainID);
    }

    public void setBlockchainID(String blockchainID) {


        this.blockchainID = blockchainID;
    }

    public void addTokenToBlockchainName() {
        String token = getTokenSymbol();
        if (Strings.isNullOrEmpty(token))
            return;
        String oldName = getBlockchain().getOfficialName();
        String newName = String.format("%s (%s)", oldName, token);
        setBlockchainName(newName);
    }

    private String blochchainName = "";

    public void setBlockchainName(String name) {
        blochchainName = name;
    }


    public String getBlockchainName() {
        if (Strings.isNullOrEmpty(blochchainName))
            return getBlockchain().getOfficialName();
        return blochchainName;
    }

    public void setBlockchainIDFromCard(String blockchainID) {

        //TODO: ONLY FOR TEST

//        if(Blockchain.fromId(blockchainID) == Blockchain.Bitcoin)
//        {
//            this.blockchainID = "BCH";
//            return;
//        }
//
//        if(Blockchain.fromId(blockchainID) == Blockchain.BitcoinTestNet)
//        {
//            this.blockchainID = "BCH/test";
//            return;
//        }


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

    public Double AmountFromInternalUnits(Long internalAmount) {
        return ((double) internalAmount) / getBlockchain().getMultiplier();
    }

    private Double AmountFromInternalUnits(Integer internalAmount) {
        return ((double) internalAmount) / getBlockchain().getMultiplier();
    }

    // TODO разобраться с балансами, привести к единому интерфейсу
    public Long InternalUnitsFromString(String caption) {
        try {
            return FormatUtil.ConvertStringToLong(caption);
        } catch (Exception e) {
            return null;
        }
    }

    public String getAmountDescription(Double amount) {
        if (amount < 10) {
            amount *= 1000.0; //todo: really?
            //String pattern = "#0.000"; // If you like 4 zeros
            //DecimalFormat myFormatter = new DecimalFormat(pattern);
            //String output = myFormatter.format(amount);

            String output = FormatUtil.DoubleToString(amount);

            return output + " m" + getBlockchain().getCurrency();

            //return String.format("%.3f m%s", amount, getBlockchain().getCurrency());
        } else {
            //return String.format("%.3f %s", amount, getBlockchain().getCurrency());
            String output = FormatUtil.DoubleToString(amount);
            return output + " " + getBlockchain().getCurrency();

        }
    }

    public String getAmountEquivalentDescription(Double amount) {
        if (getBlockchain() == Blockchain.Ethereum || getBlockchain() == Blockchain.EthereumTestNet || getBlockchain() == Blockchain.Token) {
            return EthEngine.getAmountEquivalentDescriptionETH(amount, rate);
        }

        return BtcEngine.getAmountEquivalentDescriptionBTC(amount, rate);
    }

    public boolean getAmountEquivalentDescriptionAvailable() {
        return rate > 0;
    }

    public String getAmountInGwei(String amount) {
        BigInteger d = new BigInteger(amount, 10);
        BigInteger m = d.divide(BigInteger.valueOf(1000000000L));
        return m.toString(10);
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

    public int getRemainingSignatures() {
        return remainingSignatures;
    }

    public void setRemainingSignatures(int remainingSignatures) {
        this.remainingSignatures = remainingSignatures;
    }

    public String getTransactionList() {
        try {
            StringBuffer result = new StringBuffer();
            if (hasUnspentInfo()) {
                if (mUnspentTransactions.size() > 0) {

                    for (int i = 0; i < mUnspentTransactions.size(); ++i) {
                        if (i != 0) {
                            result.append(" \n");
                        }
                        result.append(String.valueOf(i + 1));
                        result.append(": ");
                        long unix_time = GetTimestamp(mUnspentTransactions.get(i).Height);

                        if (unix_time < UNIX_TIME_2017_01_01)
                            return "-- -- --";
                        unix_time = unix_time * 1000L;

                        Date dt = new Date(unix_time);
                        //java.util.Calendar cd = Calendar.getInstance();
                        //cd.setTimeInMillis(unix_time);

                        //result.append(String.format("%02d/%02d/%04d", cd.get(Calendar.DAY_OF_MONTH), cd.get(Calendar.MONTH) + 1, cd.get(Calendar.YEAR)));

                        result.append(Util.formatDate(dt));


                        result.append(" - Amount: ");
                        result.append(getAmountDescription(AmountFromInternalUnits(mUnspentTransactions.get(i).Amount)));
                        //result.append(String.valueOf((double) mUnspentTransactions.get(i).Amount / 100000));
                        //result.append(",\nTx: "); This is only for developers
                        //result.append(String.valueOf(mUnspentTransactions.get(i).txID));

                    }

//                if (hasHistoryInfo()) {
//                    for (int i = 0; i < mHistoryTransactions.size(); ++i) {
//                        result.append(" \n");
//                        result.append("O");
//                        result.append(String.valueOf(i + 1));
//                        result.append(": ");
//                        long unix_time = GetTimestamp(mHistoryTransactions.get(i).Height);
//                        unix_time = unix_time * 1000L;
//
//                        java.util.Calendar cd = Calendar.getInstance();
//                        cd.setTimeInMillis(unix_time);
//
//                        result.append(String.format("%02d/%02d/%04d", cd.get(Calendar.DAY_OF_MONTH), cd.get(Calendar.MONTH) + 1, cd.get(Calendar.YEAR)));
//
//
//                    }
//                }
                } else {
                    result.append("-- -- --");
                }
            } else {
                result.append("-- -- --");
            }
            if (balanceUnconfirmed != 0) {
                result.append("awaiting confirmation...");
            }
            return result.toString();
        } catch (Exception e) {
            return "?";
        }
    }

    public String getInputsDescription() {

        try {
            StringBuffer result = new StringBuffer();
            if (hasUnspentInfo()) {
                if (mUnspentTransactions.size() == 1) {
                    //
                    if (balanceUnconfirmed != 0) {
                        result.append(getAmountDescription(AmountFromInternalUnits(mUnspentTransactions.get(0).Amount)));
                    } else {
                        result.append("1 unspent");
                    }
                    long unix_time = GetTimestamp(mUnspentTransactions.get(0).Height);

                    if (unix_time < UNIX_TIME_2017_01_01) {

                    } else {
                        result.append(", ");
                        unix_time = unix_time * 1000L;
                        result.append(Util.formatDateTime(new Date(unix_time)));
                    }


//                    java.util.Calendar cd = Calendar.getInstance();
//                    cd.setTimeInMillis(unix_time);

                    //result.append(String.format(" at %02d/%02d/%04d %02d:%02d:%02d", cd.get(Calendar.DAY_OF_MONTH), cd.get(Calendar.MONTH) + 1, cd.get(Calendar.YEAR), cd.get(Calendar.HOUR), cd.get(Calendar.MINUTE), cd.get(Calendar.SECOND)));


                    if (balanceUnconfirmed != 0) {
                        result.append("\nawaiting confirmation...");
                    }
                } else if (mUnspentTransactions.size() > 0) {
                    long minDate = GetTimestamp(mUnspentTransactions.get(0).Height) * 1000L;
                    long maxDate = minDate;
                    int unspentAmount = mUnspentTransactions.get(0).Amount;

                    for (int i = 1; i < mUnspentTransactions.size(); ++i) {
                        long unix_time = GetTimestamp(mUnspentTransactions.get(i).Height);
                        unix_time = unix_time * 1000L;

                        if (unix_time < minDate) minDate = unix_time;
                        if (unix_time > maxDate) maxDate = unix_time;
                        unspentAmount += mUnspentTransactions.get(i).Amount;
                    }

                    Calendar cd = Calendar.getInstance();
                    cd.setTimeInMillis(minDate);
                    if (balanceUnconfirmed != 0) {
                        result.append(getAmountDescription(AmountFromInternalUnits(unspentAmount)));
                        result.append(" in ");
                    }
                    result.append(String.valueOf(mUnspentTransactions.size()));
                    result.append(" unspents\n");

                    result.append(String.format("between %s\n", Util.formatDateTime(new Date(minDate))));

                    cd.setTimeInMillis(maxDate);
                    result.append(String.format("and %s", Util.formatDateTime(new Date(maxDate))));

                    if (balanceUnconfirmed != 0) {
                        result.append("\nawaiting confirmation...");
                    }
                } else if (balanceUnconfirmed == 0) {
                    result.append("-- -- --");
                } else if (balanceUnconfirmed != 0) {
                    result.append("awaiting confirmation...");
                }
            } else {
                result.append("-- -- --");
            }
            return result.toString();
        } catch (Exception e) {
            return "?";
        }
    }

    public int getCountOutputs() {
        int count = 0;
        for (int i = 0; i < mHistoryTransactions.size(); ++i) {
            if (mHistoryTransactions.get(i).isInput)
                continue;
            ++count;
        }
        return count;
    }

    public String getOutputsDescription() {
        if (hasUnspentInfo() && hasHistoryInfo() && mHistoryTransactions.size() - mUnspentTransactions.size() > 0) {
            return String.valueOf(mHistoryTransactions.size() - mUnspentTransactions.size());
        } else if (hasUnspentInfo() && hasHistoryInfo()) {
            return "-- -- --";
        } else {
            return "-- -- --";
        }
    }

    static int UNIX_TIME_2017_01_01 = 1483228800;

    public String getLastInputDescription() {

        if (hasUnspentInfo()) {
            if (mUnspentTransactions.size() > 0) {

                long maxUDT = 0;
                int UnspentTransactionsAmount = 0;

                for (int i = 0; i < mUnspentTransactions.size(); ++i) {
                    UnspentTransactionsAmount += mUnspentTransactions.get(i).Amount;
                    long unix_time = GetTimestamp(mUnspentTransactions.get(i).Height);

                    if (unix_time < UNIX_TIME_2017_01_01) {
                        return "-- -- --";
                    }
                    unix_time = unix_time * 1000L;
                    if (unix_time > maxUDT) maxUDT = unix_time;
                }

                Calendar cd = Calendar.getInstance();
                cd.setTimeInMillis(maxUDT);

                StringBuilder sb = new StringBuilder();
                //sb.append(String.format("%02d/%02d/%04d %02d:%02d:%02d", cd.get(Calendar.DAY_OF_MONTH), cd.get(Calendar.MONTH) + 1, cd.get(Calendar.YEAR), cd.get(Calendar.HOUR), cd.get(Calendar.MINUTE), cd.get(Calendar.SECOND)));
                sb.append(Util.formatDateTime(new Date(maxUDT)));

                if (balanceUnconfirmed != null && balanceUnconfirmed != 0) {
                    return "awaiting confirmation...";
                }
                return sb.toString();

            } else {
                if (balanceUnconfirmed != null) {
                    if (balanceUnconfirmed != 0) {
                        return "awaiting confirmation...";
                    } else {
                        return "-- -- --";
                    }
                }
            }
        }
        return "-- -- --";

    }

    public String getLastOutputDescription() {
        if (hasUnspentInfo() && hasHistoryInfo()) {
            if (mHistoryTransactions.size() - mUnspentTransactions.size() > 0) {
                long maxUDT = 0;

                for (int i = 0; i < mHistoryTransactions.size(); ++i) {
                    if (!mHistoryTransactions.get(i).isInput)
                        continue;
                    if (indexfOfUnspentTransaction(mHistoryTransactions.get(i).txID) != -1)
                        continue;
                    long unix_time = GetTimestamp(mHistoryTransactions.get(i).Height);
                    if (unix_time == 1296688602L)
                        unix_time = 0;
                    if (unix_time < UNIX_TIME_2017_01_01)
                        return "-- -- --";
                    unix_time = unix_time * 1000L;
                    if (unix_time > maxUDT) maxUDT = unix_time;
                }

                if (maxUDT == 0) {
                    return "-- -- --";
                } else {
                    return Util.formatDateTime(new Date(maxUDT));
                }

            } else {
                return "-- -- --";
            }
        } else {
            return "-- -- --";
        }
    }

    private int indexfOfUnspentTransaction(String txID) {
        if (hasUnspentInfo()) {
            for (int i = 0; i < mUnspentTransactions.size(); i++) {
                if (mUnspentTransactions.get(i).txID.equals(txID)) return i;
            }
        }
        return -1;
    }

    public String getLastSignedDescription() {
        Date dt = LastSignStorage.getLastSignDate(getWallet());
        if (dt != null) {
            return Util.formatDateTime(dt);

        } else {
            return "Not supported";
        }
    }

    public String getPIN() {
        return PIN;
    }

    public void setPIN(String PIN) {
        this.PIN = PIN;
    }

    public void clearInfo() {
        balanceConfirmed = null;
        balanceUnconfirmed = null;
        mUnspentTransactions = null;
        mHistoryTransactions = null;
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

    private Issuer issuer = Issuer.Unknown;

    public void setIssuer(Issuer issuer) {
        this.issuer = issuer;
    }

    String contractAddress = "";

    public void setContractAddress(String address) {
        contractAddress = address;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    String tokenSymbol = "";

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

    public Issuer getIssuer() {
        return issuer;
    }

    public String getIssuerDescription() {
        return issuer.getOfficialName();
    }

    byte[] issuerData;
    byte[] issuerDataSignature;

    public byte[] getIssuerData() {
        return issuerData;
    }

    public byte[] getIssuerDataSignature() {
        return issuerDataSignature;
    }

    public void setIssuerData(byte[] value, byte[] signature) {
        issuerData = value;
    }

    boolean needWriteIssuerData = false;

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
        return (settingsMask.intValue() & SettingsMask.IsReusable) != 0;
    }

    public Boolean allowSwapPIN() {
        if (settingsMask == null) return null;
        return (settingsMask.intValue() & SettingsMask.AllowSwapPIN) != 0;
    }

    public Boolean allowSwapPIN2() {
        if (settingsMask == null) return null;
        return (settingsMask.intValue() & SettingsMask.AllowSwapPIN2) != 0;
    }

    public Boolean needCVC() {
        if (settingsMask == null) return null;
        return (settingsMask.intValue() & SettingsMask.UseCVC) != 0;
    }

    public Boolean useDefaultPIN1() {
        return PINStorage.isDefaultPIN(getPIN());
    }

    public Boolean useSmartSecurityDelay() {
        if (settingsMask == null) return null;
        return (settingsMask.intValue() & SettingsMask.SmartSecurityDelay) != 0;
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
        return (settingsMask.intValue() & SettingsMask.UseNDEF) != 0;
    }

    public Boolean supportOnlyOneCommandAtTime() {
        if (settingsMask == null) return null;
        return supportNDEF() && ((settingsMask.intValue() & SettingsMask.UseOneCommandAtTime) != 0);
    }

    public Boolean supportDynamicNDEF() {
        if (settingsMask == null) return null;
        return supportNDEF() && ((settingsMask.intValue() & SettingsMask.UseDynamicNDEF) != 0);
    }

    public Boolean supportBlock() {
        if (settingsMask == null) return null;
        return (settingsMask.intValue() & SettingsMask.UseBlock) != 0;
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

    public static String getFirmwareVersionNumber(String version) throws Exception {
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

    public static int[] getFirmwareVersionNumbers(String version) throws Exception {
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

    private String validationNodeDescription = "";

    public String getValidationNodeDescription() {
        return validationNodeDescription;
    }

    public void setValidationNodeDescription(String validationNodeDescription) {
        this.validationNodeDescription = validationNodeDescription;
    }

    public enum SigningMethod {
        Sign_Hash(0, "Sign hash"),
        Sign_Raw(1, "Sign raw tx"),
        Sign_Hash_Validated_By_Issuer(2, "Sign hash validated by issuer"),
        Sign_Raw_Validated_By_Issuer(3, "Sign raw tx validated by issuer");

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

    public static class HeaderInfo {
        public HeaderInfo() {

        }

        public HeaderInfo(Integer height, Integer timestamp) {
            Height = height;
            TmStamp = timestamp;
        }

        public Integer TmStamp;
        public Integer Height;

        public Bundle getAsBundle() {
            Bundle B = new Bundle();
            B.putInt("TmStamp", TmStamp);
            B.putInt("Height", Height);
            return B;
        }

        public void LoadFromBundle(Bundle B) {
            TmStamp = B.getInt("TmStamp");
            Height = B.getInt("Height");
        }
    }

    public static class UnspentTransaction {
        public String txID;
        public Integer Amount;
        public Integer Height;
        public String Raw = "";

        public Bundle getAsBundle() {
            Bundle B = new Bundle();
            B.putString("txID", txID);
            B.putInt("Amount", Amount);
            B.putInt("Height", Height);
            B.putString("Raw", Raw);
            return B;
        }

        public void LoadFromBundle(Bundle B) {
            txID = B.getString("txID");
            Amount = B.getInt("Amount");
            Height = B.getInt("Height");
            Raw = B.getString("Raw");

        }
    }

    public static int CountOurTx(List<HistoryTransaction> listHTx) {
        int count = 0;
        for (HistoryTransaction tx : listHTx) {

            if (tx.Raw == null)
                continue;

            String raw = tx.Raw;
            //Log.e("H", tx.txID + " - ?");
            try {
                ArrayList<byte[]> prevHashes = BTCUtils.getPrevTX(raw);

                boolean isOur = false;
                for (byte[] hash : prevHashes) {
                    String checkID = BTCUtils.toHex(hash);

                    for (HistoryTransaction txForCheck : listHTx) {
                        String id = txForCheck.txID;
                        //Log.e("H:", checkID + " - " + id);
                        if (id.equals(checkID)) {
                            isOur = true;
                            //Log.e("H", tx.txID + " - Complete");
                            break;
                        }
                    }
                    if (isOur) {
                        ++count;
                        break;
                    }
                }

                tx.isInput = !isOur;
            } catch (BitcoinException e) {
                e.printStackTrace();
            }
        }
        return count;
    }

    public static class HistoryTransaction {
        public String txID;
        public Integer Height;
        public String Raw = "";
        public boolean isInput = false;

        public Bundle getAsBundle() {
            Bundle B = new Bundle();
            B.putString("txID", txID);
            B.putInt("Height", Height);
            B.putString("Raw", Raw);
            B.putBoolean("Input", isInput);
            return B;
        }

        public void LoadFromBundle(Bundle B) {
            txID = B.getString("txID");
            Height = B.getInt("Height");
            Raw = B.getString("Raw");
            isInput = B.getBoolean("Input");
        }
    }

    private List<UnspentTransaction> mUnspentTransactions = null;
    private List<HistoryTransaction> mHistoryTransactions = null;
    private List<HeaderInfo> mHeaders = null;

    public List<UnspentTransaction> getUnspentTransactions() {
        if (mUnspentTransactions == null) mUnspentTransactions = new ArrayList<>();
        return mUnspentTransactions;
    }

    public boolean hasUnspentInfo() {
        return mUnspentTransactions != null;
    }

    public List<HistoryTransaction> getHistoryTransactions() {
        if (mHistoryTransactions == null) mHistoryTransactions = new ArrayList<>();
        return mHistoryTransactions;
    }

    public List<HistoryTransaction> getOutputTransactions() {
        if (mHistoryTransactions == null) mHistoryTransactions = new ArrayList<>();
        return mHistoryTransactions;
    }

    public boolean hasHistoryInfo() {
        return mHistoryTransactions != null;
    }

    public List<HeaderInfo> getHaedersInfo() {
        if (mHeaders == null) mHeaders = new ArrayList<>();
        return mHeaders;
    }

    public void UpdateHeaderInfo(HeaderInfo info) {
        boolean newItem = true;
        for (int i = 0; i < mHeaders.size(); ++i) {
            if (mHeaders.get(i).Height == info.Height) {
                newItem = false;
                break;
            }
        }

        if (newItem) {
            mHeaders.add(info);
        }
    }

    public int GetTimestamp(int height) {
        if (mHeaders == null)
            return 0;
        for (int i = 0; i < mHeaders.size(); ++i) {
            if (mHeaders.get(i).Height == height) {
                return mHeaders.get(i).TmStamp;
            }
        }

        return 0;
    }

    public boolean hasHeaderInfo() {
        return mHeaders != null;
    }

    public TangemCard(String UID) {
        this.UID = UID;
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

    public Long getBalance() {
        return balanceConfirmed + balanceUnconfirmed;
    }

    public Long getBalanceUnconfirmed() {
        return balanceUnconfirmed;
    }

    public void setBalanceConfirmed(Long balance) {
        this.balanceConfirmed = balance;
    }

    public void setBalanceUnconfirmed(Long balance) {
        this.balanceUnconfirmed = balance;
    }

    public boolean hasBalanceInfo() {
        return balanceConfirmed != null && balanceUnconfirmed != null;
    }

    public String getDecimalBalance() {
        return balanceDecimal;
    }

    public String getDecimalBalanceAlter() {
        return balanceDecimalAlter;
    }

    public void setDecimalBalance(String value) {
        balanceDecimal = value;
    }

    public void setDecimalBalanceAlter(String value) {
        balanceDecimalAlter = value;
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

    public void setDenomination(byte[] denomination) {
        this.Denomination = denomination;
    }

    public byte[] getDenomination() {
        return Denomination;
    }

    public void clearDenomination() {
        Denomination = null;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public void setMessage(String value) {
        this.message = value;
    }

    public String getMessage() {
        return message;
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

    public Bundle getAsBundle() {
        Bundle B = new Bundle();
        SaveToBundle(B);
        return B;
    }

    public void SaveToBundle(Bundle B) {
        B.putString("UID", UID);
        B.putByteArray("CID", CID);
        B.putString("PIN", PIN);
        B.putString("PIN2", PIN2.name());
        B.putString("Status", status.name());
        B.putString("Blockchain", blockchainID);
        B.putString("BlockchainName", blochchainName);
        B.putInt("TokensDecimal", tokensDecimal);
        B.putString("TokenSymbol", tokenSymbol);
        B.putString("ContractAddress", contractAddress);
        B.putString("validationNodeDescription", validationNodeDescription);
        if (dtPersonalization != null) B.putLong("dtPersonalization", dtPersonalization.getTime());
        B.putInt("RemainingSignatures", remainingSignatures);
        B.putInt("MaxSignatures", maxSignatures);
        B.putInt("Health", health);
        if (settingsMask != null) B.putInt("settingsMask", settingsMask);
        B.putInt("pauseBeforePIN2", pauseBeforePIN2);
        if (signingMethod != null) B.putString("signingMethod", signingMethod.name());
        if (manufacturer != null) B.putString("Manufacturer", manufacturer.name());
        if (encryptionMode != null) B.putString("EncryptionMode", encryptionMode.name());
        if (issuer != null) B.putString("Issuer", issuer.name());
        if (firmwareVersion != null) B.putString("FirmwareVersion", firmwareVersion);
        B.putString("BalanceDecimal", balanceDecimal);
        B.putString("BalanceDecimalAlter", balanceDecimalAlter);
        B.putBoolean("ManufacturerConfirmed", manufacturerConfirmed);
        B.putBoolean("CardPublicKeyValid", isCardPublicKeyValid());
        B.putByteArray("CardPublicKey", getCardPublicKey());

        B.putString("Wallet", wallet);
        B.putString("Error", error);
        B.putBoolean("WalletPublicKeyValid", isWalletPublicKeyValid());
        if (pbWalletKey != null)
            B.putByteArray("PublicKey", pbWalletKey);
        if (pbWalletKeyRar != null)
            B.putByteArray("PublicKeyRar", pbWalletKeyRar);
        if (balanceConfirmed != null) B.putLong("BalanceConfirmed", balanceConfirmed);
        if (balanceUnconfirmed != null) B.putLong("BalanceUnconfirmed", balanceUnconfirmed);

        if (getOfflineBalance() != null) B.putByteArray("OfflineBalance", getOfflineBalance());

        if (getDenomination() != null) B.putByteArray("Denomination", getDenomination());

        if (getIssuerData() != null && getIssuerDataSignature() != null) {
            B.putByteArray("IssuerData", getIssuerData());
            B.putByteArray("IssuerDataSignature", getIssuerDataSignature());
            B.putBoolean("NeedWriteIssuerData", getNeedWriteIssuerData());
        }

        if (mUnspentTransactions != null) {
            Bundle BB = new Bundle();
            for (Integer i = 0; i < mUnspentTransactions.size(); i++) {
                BB.putBundle(i.toString(), mUnspentTransactions.get(i).getAsBundle());
            }
            B.putBundle("UnspentTransactions", BB);
        }
        if (mHeaders != null) {
            Bundle BB = new Bundle();
            for (Integer i = 0; i < mHeaders.size(); i++) {
                BB.putBundle(i.toString(), mHeaders.get(i).getAsBundle());
            }
            B.putBundle("Headers", BB);
        }
        if (mHistoryTransactions != null) {
            Bundle BB = new Bundle();
            for (Integer i = 0; i < mHistoryTransactions.size(); i++) {
                BB.putBundle(i.toString(), mHistoryTransactions.get(i).getAsBundle());
            }
            B.putBundle("HistoryTransactions", BB);
        }
        B.putFloat("rate", rate);
        B.putFloat("rateAlter", rateAlter);
        B.putString("confirmTx", GetConfirmTXCount().toString(16));
    }

    public void LoadFromBundle(Bundle B) {
        UID = B.getString("UID");
        CID = B.getByteArray("CID");
        PIN = B.getString("PIN");
        PIN2 = PIN2_Mode.valueOf(B.getString("PIN2"));
        status = Status.valueOf(B.getString("Status"));
        wallet = B.getString("Wallet");
        error = B.getString("Error");
        blockchainID = B.getString("Blockchain");
        tokensDecimal = B.getInt("TokensDecimal", 18);
        tokenSymbol = B.getString("TokenSymbol", "");
        contractAddress = B.getString("ContractAddress", "");
        if (B.containsKey("BlockchainName"))
            blochchainName = B.getString("BlockchainName", "");
        validationNodeDescription = B.getString("validationNodeDescription");
        if (B.containsKey("dtPersonalization")) {
            dtPersonalization = new Date(B.getLong("dtPersonalization"));
        }
        balanceDecimal = B.getString("BalanceDecimal");
        balanceDecimalAlter = B.getString("BalanceDecimalAlter");
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

        if (B.containsKey("Issuer")) issuer = Issuer.valueOf(B.getString("Issuer"));
        if (B.containsKey("FirmwareVersion")) firmwareVersion = B.getString("FirmwareVersion");

        cardPublicKeyValid = B.getBoolean("CardPublicKeyValid");
        if (B.containsKey("CardPublicKey")) setCardPublicKey(B.getByteArray("CardPublicKey"));

        if (B.containsKey("BalanceConfirmed")) balanceConfirmed = B.getLong("BalanceConfirmed");
        else balanceConfirmed = null;
        if (B.containsKey("BalanceUnconfirmed"))
            balanceUnconfirmed = B.getLong("BalanceUnconfirmed");
        else balanceUnconfirmed = null;

        if (B.containsKey("OfflineBalance")) setOfflineBalance(B.getByteArray("OfflineBalance"));
        else clearOfflineBalance();

        if (B.containsKey("Denomination")) setDenomination(B.getByteArray("Denomination"));
        else clearDenomination();

        if (B.containsKey("IssuerData"))
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

        if (B.containsKey("UnspentTransactions")) {
            mUnspentTransactions = new ArrayList<>();
            Bundle BB = B.getBundle("UnspentTransactions");
            Integer i = 0;
            while (BB.containsKey(i.toString())) {
                UnspentTransaction t = new UnspentTransaction();
                t.LoadFromBundle(BB.getBundle(i.toString()));
                mUnspentTransactions.add(t);
                i++;
            }
        }
        if (B.containsKey("Headers")) {
            mHeaders = new ArrayList<>();
            Bundle BB = B.getBundle("Headers");
            Integer i = 0;
            while (BB.containsKey(i.toString())) {
                HeaderInfo t = new HeaderInfo();
                t.LoadFromBundle(BB.getBundle(i.toString()));
                mHeaders.add(t);
                i++;
            }
        }
        if (B.containsKey("HistoryTransactions")) {
            mHistoryTransactions = new ArrayList<>();
            Bundle BB = B.getBundle("HistoryTransactions");
            Integer i = 0;
            while (BB.containsKey(i.toString())) {
                HistoryTransaction t = new HistoryTransaction();
                t.LoadFromBundle(BB.getBundle(i.toString()));
                mHistoryTransactions.add(t);
                i++;
            }
        }
        if (B.containsKey("rate"))
            rate = B.getFloat("rate");
        if (B.containsKey("rateAlter"))
            rateAlter = B.getFloat("rateAlter");
        if (B.containsKey("confirmTx"))
            countConfirmTX = new BigInteger(B.getString("confirmTx"), 16);
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