package com.tangem.data.network;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.LinkedTreeMap;
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.tangem.wallet.R;

import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
//import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * HTTP
 * Used in Cryptonit service
 */
public class Cryptonit {
    private static final String SERVER_URL = "https://www.cryptonit.net:443/gateway/";

    private static class Method {
        public static final String AUTHENTICATE = SERVER_URL + "public/authenticate";
        public static final String BALANCE = SERVER_URL + "private/balance";
        public static final String WITHDRAW_COINS = SERVER_URL + "private/withdrawCoins";
    }

    public static class Model {

        static class Authenticate
        {
            public static class Request
            {
                public String username;
                public String password;
            }
            public static class Response
            {
                boolean success;
                String[] missing_authenticators;
                public Object[] infos;
                public Object[] warnings;
                public Object[] errors;
                AuthenticationResult results;
            }

            static class AuthenticationResult {
//                token (string): authentication token,
//                nick (string),
//                        stayLoggedIn (boolean),
//                integer lastLogin (integer): JavaScript time OR NEVER for first login,
//                preferredLanguage (string) = ['en' or 'de']

            }

        }

        public static class Balance {
            static class Request {
                String[] currencies;
            }
            public static class Response {
                public boolean success;
                public Object[] infos;
                public Object[] warnings;
                public Object[] errors;
                public BalanceResult[] results;

                public static class BalanceResult {
                    public String currency;
                    public double balance;
                    public String receiveAddress; //the current address to receive funds for this account, if available,
                    public boolean fiat; // Is this a FIAT currency? If false, this is a CRYPTO currency.,
                    public Object[] unprocessedTransactions; // (array, optional): list of unprocessed transactions, if pass field withTransactions
                }
            }
        }

        public static class WithdrawCoins {
            public static class Request {
                public String currency;
                public Double amount;
                String toAddress;
                Double includeMinerFee;
                String password;
            }
            public static class Response {
                public Boolean success;
                public String[] missing_authenticators;
                public Object[] infos;
                public Object[] warnings;
                public Object[] errors;
            }
        }
    }

    public interface Api {
        @Headers("Content-Type: application/json")
        @POST(Method.AUTHENTICATE)
        Call<Model.Authenticate.Response> authenticate(@Body Model.Authenticate.Request request);

        @Headers("Content-Type: application/json")
        @POST(Method.BALANCE)
        Observable<Model.Balance.Response> getBalance(@Header("Auth-Token") String authToken, @Body Model.Balance.Request request);

        @Headers("Content-Type: application/json")
        @POST(Method.WITHDRAW_COINS)
        Observable<Model.WithdrawCoins.Response> withdrawCoins(@Header("Auth-Token") String authToken, @Body Model.WithdrawCoins.Request request);
    }

    private Api api = null;
    private BalanceListener balanceListener;
    private WithdrawalListener withdrawalListener;
    private ErrorListener errorListener;
    private Context context;
    private String authToken;

    public Cryptonit(Context context) {
        this.context = context;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        username = sp.getString(context.getResources().getString(R.string.key_cryptonit_username), "");
        password = sp.getString(context.getResources().getString(R.string.key_cryptonit_password), "");
        fee = sp.getString(context.getResources().getString(R.string.key_cryptonit_fee), "0.0");
    }

    public String username;
    public String password;
    private String fee;

    public String getFee() {
        return fee;
    }

    public void setFee(String value)
    {
        fee=value;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit()
                .putString(context.getResources().getString(R.string.key_cryptonit_fee), fee)
                .apply();
    }

    public Boolean haveAccountInfo() {
        return !username.isEmpty() && !password.isEmpty();
    }

    public void saveAccountInfo() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit()
                .putString(context.getResources().getString(R.string.key_cryptonit_username), username)
                .putString(context.getResources().getString(R.string.key_cryptonit_password), password)
                .apply();

    }

    public interface BalanceListener {
        void onBalanceData(Model.Balance.Response response);
    }

    public interface WithdrawalListener {
        void onWithdrawalComplete(Model.WithdrawCoins.Response response);
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

    @SuppressLint("CheckResult")
    public void requestBalance(String currency) {

        initApi();

//        Log.e("CRYPTONIT2", "username: " + username);
//        Log.e("CRYPTONIT2", "password: " + password);
//        Log.e("CRYPTONIT2", "auth-token: " + authToken);

        Model.Balance.Request request=new Model.Balance.Request();
        request.currencies=new String[] {currency};
        api.getBalance(authToken, request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(balanceModel -> balanceListener.onBalanceData(balanceModel),
                        // handle error
                        this::FireError
                );
    }

    @SuppressLint("CheckResult")
    public void requestWithdrawCoins(String currency, Double amount, String address) {

        initApi();

//        Log.e("CRYPTONIT2", "username: " + username);
//        Log.e("CRYPTONIT2", "password: " + password);
//        Log.e("CRYPTONIT2", "auth-token: " + authToken);

        Model.WithdrawCoins.Request request=new Model.WithdrawCoins.Request();
        request.currency=currency;
        request.amount=amount;
        request.toAddress=address;
        request.includeMinerFee=Double.parseDouble(fee);
        request.password=password;

        api.withdrawCoins(authToken, request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                            if (response.success != null && response.success)
                                withdrawalListener.onWithdrawalComplete(response);
                            else {
                                errorListener.onError(new Exception(((LinkedTreeMap) response.errors[0]).entrySet().toArray()[0].toString()));
                            }
                        },
                        // handle error
                        this::FireError
                );
    }

    private void FireError(Throwable e) throws IOException {
        if (e.getClass() == HttpException.class && ((HttpException) e).code() == 500) {
            JsonObject jsonObject = new JsonParser().parse(((HttpException) e).response().errorBody().string()).getAsJsonObject();
            errorListener.onError(new Exception(e.getMessage() + ": " + jsonObject.get("errors").getAsString()));
//            if (jsonObject.get("reason").getAsString().equals("Invalid nonce")) nonce += 1000;
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
                addInterceptor(new AuthorizationInterceptor()).build();

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

            if( !mainRequest.url().toString().endsWith("authenticate")&& authToken==null )
            {
                Model.Authenticate.Request authRequest=new Model.Authenticate.Request();
                authRequest.username=username;
                authRequest.password=password;
                retrofit2.Response<Model.Authenticate.Response> authResponse=api.authenticate(authRequest).execute();
                if( authResponse.isSuccessful() && authResponse.body().success)
                {
                    String newToken = authResponse.headers().get("auth-token");
                    if (newToken != null) {
                        authToken = newToken;
                    }
                }else{
                    throw new IOException("Authentication error: "+authResponse.message());
                }
            }

            if( authToken!=null ) {
                mainRequest = mainRequest.newBuilder().addHeader("auth-token", authToken).build();
            }
            Response mainResponse = chain.proceed(mainRequest);
            if (!mainResponse.isSuccessful()) {
                authToken=null;
            }
            return mainResponse;

        }
    }
}