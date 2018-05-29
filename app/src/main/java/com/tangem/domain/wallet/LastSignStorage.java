package com.tangem.domain.wallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.ArrayMap;
import android.util.ArraySet;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Created by dvol on 30.10.2017.
 */

public class LastSignStorage {

    private static SharedPreferences sharedPreferences=null;

    private static Set<String> cards = new ArraySet<>();
    private static Map<String, Date> dates = new ArrayMap<>();
    private static Map<String, String> txCol = new ArrayMap<>();
    private static Map<String, String> txCompleteCol = new ArrayMap<>();

    public static void Init(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        cards=sharedPreferences.getStringSet("LastSign_Cards", cards);
        for (int i = 0; i < cards.size(); i++) {
            String wallet = cards.toArray()[i].toString();
            Date dt = new Date();
            dt.setTime(sharedPreferences.getLong("LastSign_" + wallet, 0));
            dates.put(wallet, dt);
        }
    }

    public static boolean needInit() {
        return sharedPreferences==null;
    }

    static class CompleteTx
    {
        public String TX;
        public boolean isComplete;
    }
    public static Map<String, CompleteTx> GetTxList()
    {
        Set<String > wallets=sharedPreferences.getStringSet("LastSign_Cards", cards);
        Map<String, CompleteTx> txList = new ArrayMap<>();

        for (int i = 0; i < wallets.size(); i++) {
            String wallet = wallets.toArray()[i].toString();
            String tx = sharedPreferences.getString("LastSignTX_" + wallet, "");
            boolean complete = sharedPreferences.getBoolean("LastSignTXComplete_" + wallet, false);
            CompleteTx txComplete = new CompleteTx();
            txComplete.isComplete = complete;
            txComplete.TX = tx;
            txList.put(wallet, txComplete);
        }

        return txList;
    }
    public static Date getLastSignDate(String wallet) {
        if (dates.containsKey(wallet)) return dates.get(wallet);
        return null;
    }

    public static void setLastSignDate(String wallet, Date date) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (!cards.contains(wallet)) {
            cards.add(wallet);
            editor.putStringSet("LastSign_Cards", cards);
        }
        dates.put(wallet, date);
        editor.putLong("LastSign_" + wallet, date.getTime());
        editor.apply();
    }

    public static void setLastTX(String wallet, String tx) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (!cards.contains(wallet)) {
            cards.add(wallet);
            editor.putStringSet("LastSign_Cards", cards);
        }

        editor.putString("LastSignTX_" + wallet, tx);
        editor.putBoolean("LastSignTXComplete_" + wallet, false);
        editor.apply();
    }

    public static void setLastMessage(String wallet, String message) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (!cards.contains(wallet)) {
            cards.add(wallet);
            editor.putStringSet("LastSign_Cards", cards);
        }

        editor.putString("LastSignMessage_" + wallet, message);
        editor.apply();
    }

    public static String getLastMessage(String wallet)
    {
        try {
            String msg = sharedPreferences.getString("LastSignMessage_" + wallet, "");
            return msg;
        }
        catch(Exception e)
        {
            return "";
        }
    }

    public static void setTxWasSend(String wallet)
    {

        Map<String, CompleteTx> txList = GetTxList();
        if(txList.containsKey(wallet))
        {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("LastSignTXComplete_" + wallet, true);
            editor.apply();
        }
    }
    public static boolean getNeedTxSend(String wallet)
    {
        Map<String, CompleteTx> txList = GetTxList();
        if(txList.containsKey(wallet))
        {
            boolean complete = txList.get(wallet).isComplete;
            return !complete;
        }
        return false;
    }

    public static String getTxForSend(String wallet)
    {
        Map<String, CompleteTx> txList = GetTxList();
        if(txList.containsKey(wallet))
        {
            return txList.get(wallet).TX;
        }
        return "";
    }
}
