package com.tangem.wallet;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;

import com.tangem.cardReader.CardProtocol;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;

/**
 * Created by dvol on 12.09.2017.
 * Global PIN Storage
 */

public class PINStorage {
    private static String mSavedPIN, mUserPIN, mLastUsedPIN, mEncryptedPIN, mPIN2;
    private static SharedPreferences sharedPreferences=null;

    public static void Init(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mSavedPIN = sharedPreferences.getString("SavedPIN", null);
        mUserPIN = null;
        mLastUsedPIN = null;
        mEncryptedPIN = null;
        mPIN2 = null;
    }


    static List<String> getPINs() {
        ArrayList<String> result = new ArrayList<>();
        if (mLastUsedPIN != null) result.add(mLastUsedPIN);
        if (mEncryptedPIN != null && !result.contains(mEncryptedPIN)) result.add(mEncryptedPIN);
        if (mUserPIN != null && !result.contains(mUserPIN)) result.add(mUserPIN);
        if (mSavedPIN != null && !result.contains(mSavedPIN)) result.add(mSavedPIN);
        if (!result.contains(CardProtocol.DefaultPIN)) result.add(CardProtocol.DefaultPIN);
        return result;
    }

    static void setLastUsedPIN(String PIN) {
        mLastUsedPIN = PIN;
    }

    public static void setUserPIN(String PIN) {
        mUserPIN = PIN;
    }

    public static void setPIN2(String PIN) {
        mPIN2 = PIN;
    }

    public static void savePIN(String PIN) {
        mSavedPIN = PIN;
        if (mSavedPIN != null && !mSavedPIN.isEmpty()) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("SavedPIN", mSavedPIN);
            editor.apply();
        } else {
            deletePIN();
        }
    }

    public static void deletePIN() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (mSavedPIN != null && mLastUsedPIN != null && mSavedPIN.equals(mLastUsedPIN)) {
            mLastUsedPIN = null;
        }
        mSavedPIN = null;
        editor.remove("SavedPIN");
        editor.apply();
    }

    public static void saveEncryptedPIN(Cipher cipher, String PIN) {
        try {
            byte[] iv = cipher.getIV();
            byte[] bytes = cipher.doFinal(PIN.getBytes());
            String encryptedPIN = Base64.encodeToString(bytes, Base64.NO_WRAP);
            String sIV = Base64.encodeToString(iv, Base64.NO_WRAP);

//            Log.d("PINStorage", String.format("saveEncryptedPIN: %s, encrypted: %s, iv: %s",PIN,encryptedPIN,sIV));
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("EncryptedPIN", encryptedPIN);
            editor.putString("EncryptedIV", sIV);
            editor.apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] loadEncryptedIV() {
        String sIV = sharedPreferences.getString("EncryptedIV", "");
//        Log.d("PINStorage", String.format("loadEncryptedIV: %s",sIV));

        return Base64.decode(sIV, Base64.NO_WRAP);
    }

    public static String loadEncryptedPIN(Cipher cipher) {
        String encryptedPIN = sharedPreferences.getString("EncryptedPIN", null);

        try {
            byte[] bytes = Base64.decode(encryptedPIN, Base64.NO_WRAP);
            mEncryptedPIN = new String(cipher.doFinal(bytes));
//            Log.d("PINStorage", String.format("loadEncryptedPIN: %s (encrypted: %s)",mEncryptedPIN,encryptedPIN));
        } catch (Exception e) {
            e.printStackTrace();
            mEncryptedPIN = null;
        }
        return mEncryptedPIN;
    }

    public static boolean haveEncryptedPIN() {
        return sharedPreferences.getString("EncryptedPIN", null) != null;
    }

    public static void deleteEncryptedPIN() {
        if (mEncryptedPIN != null && mLastUsedPIN != null && mEncryptedPIN.equals(mLastUsedPIN)) {
            mLastUsedPIN = null;
        }
        mEncryptedPIN = null;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("EncryptedPIN");
        editor.remove("EncryptedIV");
        editor.apply();
    }

    public static void saveEncryptedPIN2(Cipher cipher, String PIN) {
        try {
            byte[] iv = cipher.getIV();
            byte[] bytes = cipher.doFinal(PIN.getBytes());
            String encryptedPIN = Base64.encodeToString(bytes, Base64.NO_WRAP);
            String sIV = Base64.encodeToString(iv, Base64.NO_WRAP);

//            Log.d("PINStorage", String.format("saveEncryptedPIN: %s, encrypted: %s, iv: %s",PIN,encryptedPIN,sIV));
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("EncryptedPIN2", encryptedPIN);
            editor.putString("EncryptedIV2", sIV);
            editor.apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] loadEncryptedIV2() {
        String sIV = sharedPreferences.getString("EncryptedIV2", "");
//        Log.d("PINStorage", String.format("loadEncryptedIV: %s",sIV));

        return Base64.decode(sIV, Base64.NO_WRAP);
    }

    public static String loadEncryptedPIN2(Cipher cipher) {
        String encryptedPIN = sharedPreferences.getString("EncryptedPIN2", null);

        try {
            byte[] bytes = Base64.decode(encryptedPIN, Base64.NO_WRAP);
            mPIN2 = new String(cipher.doFinal(bytes));
//            Log.d("PINStorage", String.format("loadEncryptedPIN: %s (encrypted: %s)",mEncryptedPIN,encryptedPIN));
        } catch (Exception e) {
            e.printStackTrace();
            mPIN2 = null;
        }
        return mPIN2;
    }

    public static boolean haveEncryptedPIN2() {
        return sharedPreferences.getString("EncryptedPIN2", null) != null;
    }

    public static void deleteEncryptedPIN2() {
        mPIN2 = null;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("EncryptedPIN2");
        editor.remove("EncryptedIV2");
        editor.apply();
    }

    public static String getPIN2() {
        return mPIN2;
    }

    public static boolean isDefaultPIN(String pin) {
        return (pin != null) && (CardProtocol.DefaultPIN.equals(pin));
    }

    public static boolean isDefaultPIN2(String pin2) {
        return (pin2 != null) && (CardProtocol.DefaultPIN2.equals(pin2));
    }

    public static String getDefaultPIN() {
        return CardProtocol.DefaultPIN;
    }

    public static String getDefaultPIN2() {
        return CardProtocol.DefaultPIN2;
    }

    public static boolean needInit() {
        return sharedPreferences==null;
    }
}
