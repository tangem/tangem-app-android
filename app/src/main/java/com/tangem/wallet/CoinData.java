package com.tangem.wallet;

import android.os.Bundle;
import android.util.Log;

import com.tangem.data.Blockchain;

public abstract class CoinData {

    public CoinData() {
    }

    private String wallet;

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

    public boolean isBalanceReceived() {
        return balanceReceived;
    }

    private boolean balanceReceived = false;

    public void setBalanceReceived(boolean value) {
        balanceReceived = value;
    }

    public void loadFromBundle(Bundle B) {
        wallet = B.getString("Wallet");

        if (B.containsKey("balanceReceived")) setBalanceReceived(B.getBoolean("balanceReceived"));

        validationNodeDescription = B.getString("validationNodeDescription");

//        if (B.containsKey("FailedBalance"))
//            failedBalanceRequestCounter = new AtomicInteger(B.getInt("FailedBalance"));

        if (B.containsKey("isBalanceEqual")) setIsBalanceEqual(B.getBoolean("isBalanceEqual"));

        if (B.containsKey("rate"))
            rate = B.getFloat("rate");
        if (B.containsKey("rateAlter"))
            rateAlter = B.getFloat("rateAlter");

        if (B.containsKey("sentTransactionsCount")) {
            sentTransactionsCount = B.getInt("sentTransactionsCount");
        }
    }

    public void saveToBundle(Bundle B) {
        try {
            B.putString("Wallet", wallet);

            if (balanceEqual != null) B.putBoolean("isBalanceEqual", balanceEqual);

//            if (failedBalanceRequestCounter != null)
//                B.putInt("FailedBalance", failedBalanceRequestCounter.get());

            B.putFloat("rate", rate);
            B.putFloat("rateAlter", rateAlter);

            B.putBoolean("balanceReceived", balanceReceived);
            B.putString("validationNodeDescription", validationNodeDescription);

            B.putInt("sentTransactionsCount", sentTransactionsCount);
        } catch (Exception e) {
            Log.e("Can't save to bundle ", e.getMessage());
        }

    }

    public Bundle asBundle() {
        Bundle bundle = new Bundle();
        saveToBundle(bundle);
        return bundle;
    }

    public static CoinData fromBundle(Blockchain blockchain, Bundle bundle) {
        CoinEngine engine = CoinEngineFactory.INSTANCE.create(blockchain);
        if (engine == null) return null;
        CoinData result = engine.createCoinData();
        result.loadFromBundle(bundle);
        return result;
    }

    //TODO - move all to special engines
    private float rate = 0;
    private float rateAlter = 0;

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

//        public Double amountFromInternalUnits(Long internalAmount) {
//            // TODO java.lang.NullPointerException: Attempt to invoke virtual method 'long java.lang.Long.longValue()' on a null object reference
//            return ((double) internalAmount) /
//                    getBlockchain().getMultiplier();
//        }
//
//        public Double amountFromInternalUnits(Integer internalAmount) {
//            return ((double) internalAmount) / getBlockchain().getMultiplier();
//        }
//
//        // TODO разобраться с балансами, привести к единому интерфейсу
//        public Long internalUnitsFromString(String caption) {
//            try {
//                return FormatUtil.ConvertStringToLong(caption);
//            } catch (Exception e) {
//                return null;
//            }
//        }
//
//        public String getAmountDescription(Double amount) {
//            String output = FormatUtil.DoubleToString(amount);
//            return output + " " + getBlockchain().getCurrency();
//        }

    public boolean getAmountEquivalentDescriptionAvailable() {
        return rate > 0;
    }


    public void clearInfo() {
        setIsBalanceEqual(false);
        setBalanceReceived(false);
        setValidationNodeDescription("");
        minFee = null;
        maxFee = null;
        normalFee = null;
        rate = 0f;
        rateAlter = 0f;
        sentTransactionsCount = 0;
    }

//    private AtomicInteger failedBalanceRequestCounter;
//
//    public int incFailedBalanceRequestCounter() {
//        if (failedBalanceRequestCounter == null)
//            failedBalanceRequestCounter = new AtomicInteger(0);
//        return failedBalanceRequestCounter.incrementAndGet();
//    }
//
//    public void resetFailedBalanceRequestCounter() {
//        failedBalanceRequestCounter = new AtomicInteger(0);
//    }
//
//    public int getFailedBalanceRequestCounter() {
//        if (failedBalanceRequestCounter == null)
//            return 0;
//        return failedBalanceRequestCounter.get();
//    }

    private Boolean balanceEqual;

    public Boolean isBalanceEqual() {
        return balanceEqual;
    }

    public void setIsBalanceEqual(boolean isEqual) {
        balanceEqual = isEqual;
    }

    private String validationNodeDescription = "";

    public String getValidationNodeDescription() {
        return validationNodeDescription;
    }

    public void setValidationNodeDescription(String validationNodeDescription) {
        this.validationNodeDescription = validationNodeDescription;
    }

    public CoinEngine.Amount minFee = null;
    public CoinEngine.Amount normalFee = null;
    public CoinEngine.Amount maxFee = null;

    private int sentTransactionsCount = 0;

    public int getSentTransactionsCount() {
        return sentTransactionsCount;
    }

    public void setSentTransactionsCount(int sentTransactionsCount) {
        this.sentTransactionsCount = sentTransactionsCount;
    }

    public void incSentTransactionsCount() {
        sentTransactionsCount++;
    }
}
