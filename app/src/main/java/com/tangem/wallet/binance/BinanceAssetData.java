package com.tangem.wallet.binance;

import android.os.Bundle;
import android.util.Log;

import com.tangem.wallet.CoinEngine;

public class BinanceAssetData extends BinanceData {
    private String assetBalance;
    private String assetSymbol;

    @Override
    public void loadFromBundle(Bundle B) {
        super.loadFromBundle(B);

        if (B.containsKey("AssetBalance")) assetBalance = B.getString("AssetBalance");
        else assetBalance = null;
        if (B.containsKey("AssetSymbol")) assetSymbol = B.getString("AssetSymbol");
        else assetSymbol = null;
    }

    @Override
    public void saveToBundle(Bundle B) {
        super.saveToBundle(B);
        try {
            if (assetBalance != null) B.putString("AssetBalance", assetBalance);
            if (assetSymbol != null) B.putString("AssetSymbol", assetSymbol);
        } catch (Exception e) {
            Log.e("Can't save to bundle ", e.getMessage());
        }
    }

    @Override
    public void clearInfo() {
        super.clearInfo();
        assetBalance = null;
    }

    @Override
    public boolean hasBalanceInfo() {
        return super.hasBalanceInfo() || assetBalance != null;
    }

    public CoinEngine.Amount getAssetBalance() {
        return assetBalance == null ? null : new CoinEngine.Amount(assetBalance, assetSymbol);
    }

    public void setAssetBalance(String assetBalance) {
        this.assetBalance = assetBalance;
    }

    public void setAssetSymbol(String assetSymbol) {
        this.assetSymbol = assetSymbol;
    }
}
