package com.tangem.data.network;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.tangem.util.Util;
import com.tangem.wallet.R;

import java.io.IOException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
//import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

/**
 * HTTP
 * Used in CryptonitOtherApi service
 */
public class CryptonitOtherApi {
    private static final String SERVER_URL = "https://api.cryptonit.net/api/";

    private static class Method {
        public static final String BALANCE = SERVER_URL + "balance/{cryptoCurrency}%2F{fiatCurrency}";
        public static final String CRYPTO_WITHDRAWAL = SERVER_URL + "crypto_withdrawal/";
    }

    public static class Response {
        public static class Balance{
            @SerializedName("btc_balance")
            public String btc_balance;
            @SerializedName("eur_balance")
            public String eur_balance;
            @SerializedName("eth_balance")
            public String eth_balance;
            @SerializedName("etn_balance")
            public String etn_balance;
            @SerializedName("btc_reserved")
            public String btc_reserved;
            @SerializedName("eur_reserved")
            public String eur_reserved;
            @SerializedName("eth_reserved")
            public String eth_reserved;
            @SerializedName("etn_reserved")
            public String etn_reserved;
            @SerializedName("btc_available")
            public String btc_available;
            @SerializedName("eur_available")
            public String eur_available;
            @SerializedName("eth_available")
            public String eth_available;
            @SerializedName("etn_available")
            public String etn_available;
            @SerializedName("btceur_fee")
            public String btceur_fee;
            @SerializedName("etheur_fee")
            public String etheur_fee;
            @SerializedName("etnbtc_fee")
            public String etnbtc_fee;
            @SerializedName("fee")
            public String fee;
        }

        public static class CryptoWithdrawal {
            @SerializedName("success")
            public Boolean success;
            @SerializedName("status")
            public String status;
            @SerializedName("reason")
            public Object reason;
        }
    }

    public interface Api {
        @Multipart
        @Headers("accept: multipart/form-data")
        @POST(Method.BALANCE)
        Observable<Response.Balance> getBalance(
                @Path("cryptoCurrency") String cryptoCurrency, @Path("fiatCurrency") String fiatCurrency,
                @Part(value = "key") RequestBody key, @Part("signature") RequestBody signature, @Part("nonce") RequestBody nonce);

        @Multipart
        @Headers("accept: multipart/form-data")
        @POST(Method.CRYPTO_WITHDRAWAL)
        Observable<Response.CryptoWithdrawal> cryptoWithdrawal(
                @Part(value = "currency") RequestBody currency, @Part("amount") RequestBody amount, @Part("address") RequestBody address,
                @Part(value = "key") RequestBody key, @Part("signature") RequestBody signature, @Part("nonce") RequestBody nonce);
    }

    private Api api = null;
    private BalanceListener balanceListener;
    private WithdrawalListener withdrawalListener;
    private ErrorListener errorListener;
    private Context context;

    public CryptonitOtherApi(Context context) {
        this.context = context;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        key = sp.getString(context.getResources().getString(R.string.key_cryptonit_key), "");
        userId = sp.getString(context.getResources().getString(R.string.key_cryptonit_user_id), "");
        secret = sp.getString(context.getResources().getString(R.string.key_cryptonit_secret), "");
        nonce = sp.getInt(context.getResources().getString(R.string.key_cryptonit_nonce), 0);
    }

    public String key;
    public String userId;
    public String secret;
    private Integer nonce;

    public String getSecretDescription() {
        if (secret == null || secret.isEmpty()) return "";
        return secret.substring(0, 3) + "..." + secret.substring(secret.length() - 3, secret.length());
    }

    public Boolean havaAccountInfo() {
        return (!userId.isEmpty() && !key.isEmpty() && !secret.isEmpty());
    }

    public void saveAccountInfo() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        nonce = 1;
        sp.edit()
                .putString(context.getResources().getString(R.string.key_cryptonit_key), key)
                .putString(context.getResources().getString(R.string.key_cryptonit_user_id), userId)
                .putString(context.getResources().getString(R.string.key_cryptonit_secret), secret)
                .putInt(context.getResources().getString(R.string.key_cryptonit_nonce), nonce)
                .apply();

    }

    private void incNonce() {
        nonce++;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt(context.getResources().getString(R.string.key_cryptonit_nonce), nonce).apply();
    }

    public interface BalanceListener {
        void onBalanceData(Response.Balance response);
    }

    public interface WithdrawalListener {
        void onWithdrawalComplete(Response.CryptoWithdrawal response);
    }

    public interface ErrorListener {
        void onError(Throwable throwable);
    }

    public void setBalanceListener(BalanceListener listener) {
        balanceListener = listener;
    }

    public void setWithdrawalListener(WithdrawalListener listener) {
        withdrawalListener = listener;
    }

    public void setErrorListener(ErrorListener listener) {
        errorListener = listener;
    }

    private String calcSignature() throws Exception {
        incNonce();
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
        sha256_HMAC.init(secret_key);

        String data = nonce.toString() + userId + key;
        return Util.bytesToHex(sha256_HMAC.doFinal(data.getBytes("UTF-8")));
    }

    @SuppressLint("CheckResult")
    public void requestBalance(String cryptoCurrency, String fiatCurrency) throws Exception {

        initApi();

        String signature = calcSignature();

//        Log.e("CRYPTONIT", "user: " + userId);
//        Log.e("CRYPTONIT", "key: " + key);
//        Log.e("CRYPTONIT", "secret: " + secret);
//        Log.e("CRYPTONIT", "nonce: " + nonce);
//        Log.e("CRYPTONIT", "signature: " + signature);

        api.getBalance(cryptoCurrency, fiatCurrency,
                RequestBody.create(MediaType.parse("text/plain"), key),
                RequestBody.create(MediaType.parse("text/plain"), signature),
                RequestBody.create(MediaType.parse("text/plain"), nonce.toString()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(balanceModel -> balanceListener.onBalanceData(balanceModel),
                        // handle error
                        this::FireError
                );
    }

    @SuppressLint("CheckResult")
    public void requestCryptoWithdrawal(String currency, String amount, String address) throws Exception {

        initApi();

        String signature = calcSignature();

//        Log.e("CRYPTONIT", "user: " + userId);
//        Log.e("CRYPTONIT", "key: " + key);
//        Log.e("CRYPTONIT", "secret: " + secret);
//        Log.e("CRYPTONIT", "nonce: " + nonce);
//        Log.e("CRYPTONIT", "signature: " + signature);
//
        api.cryptoWithdrawal(
                RequestBody.create(MediaType.parse("text/plain"), currency),
                RequestBody.create(MediaType.parse("text/plain"), amount),
                RequestBody.create(MediaType.parse("text/plain"), address),
                RequestBody.create(MediaType.parse("text/plain"), key),
                RequestBody.create(MediaType.parse("text/plain"), signature),
                RequestBody.create(MediaType.parse("text/plain"), nonce.toString()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                            if (response.success != null && response.success)
                                withdrawalListener.onWithdrawalComplete(response);
                            else {
                                LinkedTreeMap reason = (LinkedTreeMap) response.reason;
                                errorListener.onError(new Exception(reason.entrySet().toArray()[0].toString()));
                            }
                        },
                        // handle error
                        this::FireError
                );
    }

    private void FireError(Throwable e) throws IOException {
        if (e.getClass() == HttpException.class && ((HttpException) e).code() == 500) {
            JsonObject jsonObject = new JsonParser().parse(((HttpException) e).response().errorBody().string()).getAsJsonObject();
            errorListener.onError(new Exception(e.getMessage() + ": " + jsonObject.get("reason").getAsString()));
            if (jsonObject.get("reason").getAsString().equals("Invalid nonce")) nonce += 1000;
        } else {
            errorListener.onError(e);
        }
    }

    private void initApi() {

        if (api != null) return;

//        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
//        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient httpClient = new OkHttpClient.Builder().
//                addInterceptor(logging).
                build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SERVER_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(httpClient)
                .build();

        api = retrofit.create(Api.class);
    }
}