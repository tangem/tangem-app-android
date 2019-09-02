package com.tangem.data.network;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.tangem.tangem_card.util.Util;
import com.tangem.wallet.R;

import org.spongycastle.util.encoders.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

//import okhttp3.logging.HttpLoggingInterceptor;

/**
 * HTTP
 * Used in Kraken service
 */
public class Kraken {
    private static final String SERVER_URL = "https://api.kraken.com";

    private static class Method {
        public static final String BALANCE = SERVER_URL + "/0/private/Balance";
        public static final String WITHDRAW_INFO = SERVER_URL + "/0/private/WithdrawInfo";
        public static final String WITHDRAW = SERVER_URL + "/0/private/Withdraw";
    }

    public static class Model {

        public static class Balance {
            public static class Response {
                public String[] error;
                public Result result;

                public static class Result {
                    public String XXBT;
                    public String XETH;
                    public String BCH;
                }
            }
        }

        public static class WithdrawInfo {
            public static class Response {
                public String[] error;
                public Result result;

                public static class Result {
                    public String fee;
                    public String amount;
                }
            }
        }

        public static class Withdraw {
            public static class Response {
                public String[] error;
                public Result result;

                public static class Result {
                    public String refid;
                }
            }
        }
    }

    public interface Api {
        @FormUrlEncoded
        @POST(Method.BALANCE)
        Observable<Model.Balance.Response> getBalance(@Field("nonce") String nonce);

        @FormUrlEncoded
        @POST(Method.WITHDRAW_INFO)
        Observable<Model.WithdrawInfo.Response> WithdrawInfo(@Field("nonce") String nonce, @Field("asset") String asset, @Field("key") String key, @Field("amount") String amount);

        @FormUrlEncoded
        @POST(Method.WITHDRAW)
        Observable<Model.Withdraw.Response> Withdraw(@Field("nonce") String nonce, @Field("asset") String asset, @Field("key") String key, @Field("amount") String amount);
    }

    private Api api = null;
    private BalanceListener balanceListener;
    private WithdrawalListener withdrawalListener;
    private WithdrawalInfoListener withdrawalInfoListener;
    private ErrorListener errorListener;
    private Context context;

    public Kraken(Context context) {
        this.context = context;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        key = sp.getString(context.getResources().getString(R.string.key_kraken_key), "");
        secret = sp.getString(context.getResources().getString(R.string.key_kraken_secret), "");
        nonce = sp.getInt(context.getResources().getString(R.string.key_kraken_nonce), 0);
    }

    public String key;
    public String secret;
    private Integer nonce;

    public String getSecretDescription() {
        if (secret == null || secret.isEmpty()) return "";
        return secret.substring(0, 3) + "..." + secret.substring(secret.length() - 3, secret.length());
    }

    public Boolean haveAccountInfo() {
        return (!key.isEmpty() && !secret.isEmpty());
    }

    public void saveAccountInfo() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit()
                .putString(context.getResources().getString(R.string.key_kraken_key), key)
                .putString(context.getResources().getString(R.string.key_kraken_secret), secret)
                .putInt(context.getResources().getString(R.string.key_kraken_nonce), nonce)
                .apply();

    }

    private void incNonce() {
        nonce++;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt(context.getResources().getString(R.string.key_kraken_nonce), nonce).apply();
    }

    public interface BalanceListener {
        void onBalanceData(Model.Balance.Response response);
    }

    public interface WithdrawalInfoListener {
        void onWithdrawalInfoComplete(Model.WithdrawInfo.Response response);
    }

    public interface WithdrawalListener {
        void onWithdrawalComplete(Model.Withdraw.Response response);
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

    public void setWithdrawalInfoListener(WithdrawalInfoListener listener) {
        withdrawalInfoListener = listener;
    }

    public void setErrorListener(ErrorListener listener) {
        errorListener = listener;
    }


    private static final String HMAC_SHA512 = "HmacSHA512";


    private String bodyToString(final RequestBody request){
        try {
            final RequestBody copy = request;
            final Buffer buffer = new Buffer();
            if(copy != null)
                copy.writeTo(buffer);
            else
                return "";
            return buffer.readUtf8();
        }
        catch (final IOException e) {
            return "did not work";
        }
    }

    private String calcSignature(String url, RequestBody requestBody) throws NoSuchAlgorithmException, InvalidKeyException, IOException {

        String postData=bodyToString(requestBody);

        // create SHA-256 hash of the nonce and the POST data

        String s=nonce+postData;
        byte[] sha256 = Util.calculateSHA256(s);

        // set the API method and retrieve the path
        byte[] path = url.getBytes(StandardCharsets.UTF_8);

        // decode the API secret, it's the HMAC key
        byte[] hmacKey = Base64.decode(secret);

        // create the HMAC message from the path and the previous hash
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        outputStream.write(path);
        outputStream.write(sha256);
        byte[] hmacMessage = outputStream.toByteArray();//concatArrays(path, sha256);

        Mac mac = Mac.getInstance(HMAC_SHA512);
        mac.init(new SecretKeySpec(hmacKey, HMAC_SHA512));
        byte[] hmacSignature=mac.doFinal(hmacMessage);

        byte[] b64Signature = Base64.encode(hmacSignature);

        return new String(b64Signature, StandardCharsets.UTF_8);
    }

    @SuppressLint("CheckResult")
    public void requestBalance() throws Exception {

        initApi();

//        Log.e("kraken", "key: " + key);
//        Log.e("kraken", "secret: " + secret);
//        Log.e("kraken", "nonce: " + nonce);

        api.getBalance(nonce.toString())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> balanceListener.onBalanceData(response),
                        // handle error
                        this::FireError
                );
    }

    @SuppressLint("CheckResult")
    public void requestWithdrawInfo(String currency, String amount, String withdrawKey) throws Exception {

        initApi();

//        Log.e("kraken", "key: " + key);
//        Log.e("kraken", "secret: " + secret);
//        Log.e("kraken", "nonce: " + nonce);

        String asset=CurrencyToAsset(currency);

//        withdrawKey = "test";
        api.WithdrawInfo(nonce.toString(), asset, withdrawKey, amount)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> withdrawalInfoListener.onWithdrawalInfoComplete(response),
                        // handle error
                        this::FireError
                );
    }

    private static String CurrencyToAsset(String currency) throws Exception {
        switch (currency)
        {
            case "BTC": return "XXBT";
            case "BCH": return "BCH";
            case "ETH": return "XETH";
            default:
                throw new Exception("Unsupported currency!");
        }
    }

    @SuppressLint("CheckResult")
    public void requestWithdraw(String currency, String amount, String withdrawKey) throws Exception {
        initApi();

//        Log.e("kraken", "key: " + key);
//        Log.e("kraken", "secret: " + secret);
//        Log.e("kraken", "nonce: " + nonce);

        String asset=CurrencyToAsset(currency);

//        withdrawKey = "test";
        api.Withdraw(nonce.toString(), asset, withdrawKey, amount)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> withdrawalListener.onWithdrawalComplete(response),
                        // handle error
                        this::FireError
                );
    }

    private void FireError(Throwable e) throws IOException {
//        if (e.getClass() == HttpException.class && ((HttpException) e).code() == 500) {
//            JsonObject jsonObject = new JsonParser().parse(((HttpException) e).response().errorBody().string()).getAsJsonObject();
//            errorListener.onError(new Exception(e.getMessage() + ": " + jsonObject.get("reason").getAsString()));
//            if (jsonObject.get("reason").getAsString().equals("Invalid nonce")) nonce += 1000;
//        } else {
            errorListener.onError(e);
//        }
    }

    private void initApi() {

        if (api != null) return;

//        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
//        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient httpClient = new OkHttpClient.Builder().
                addInterceptor(new AuthorizationInterceptor()).
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

    public class AuthorizationInterceptor implements Interceptor {

        AuthorizationInterceptor() {
        }


        @Override
        public Response intercept(Chain chain) throws IOException {
            Request mainRequest=chain.request();

            try {
                mainRequest = mainRequest.newBuilder().
                        addHeader("API-Key", key).
                        addHeader("API-Sign", calcSignature(mainRequest.url().toString().substring(SERVER_URL.length()), mainRequest.body())).
                        build();
                incNonce();
            } catch (Exception e) {
                e.printStackTrace();
                throw new IOException("Can't calculate signature: "+e.getMessage());
            }

            return chain.proceed(mainRequest);

        }
    }
}