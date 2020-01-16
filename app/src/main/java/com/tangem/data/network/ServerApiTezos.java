package com.tangem.data.network;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.tangem.data.network.model.TezosAccountResponse;
import com.tangem.data.network.model.TezosForgeBody;
import com.tangem.data.network.model.TezosHeaderResponse;
import com.tangem.data.network.model.TezosPreapplyBody;
import com.tangem.tangem_card.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ServerApiTezos {
    private static String TAG = ServerApiTezos.class.getSimpleName();

    private final String letzbakeURI = "https://teznode.letzbake.com";
    private final String tezrpcURI = "https://mainnet.tezrpc.me";

    static final String TEZOS_ADDRESS = "chains/main/blocks/head/context/contracts/{address}";
    static final String TEZOS_HEADER = "chains/main/blocks/head/header";
    static final String TEZOS_MANAGER_KEY = "chains/main/blocks/head/context/contracts/{address}/manager_key";
    static final String TEZOS_FORGE_OPERATIONS = "chains/main/blocks/head/helpers/forge/operations";
    static final String TEZOS_PREAPPLY_OPERATIONS = "chains/main/blocks/head/helpers/preapply/operations";
    static final String TEZOS_RUN_OPERATION = "chains/main/blocks/head/helpers/scripts/run_operation";
    static final String TEZOS_INJECT_OPERATIONS = "injection/operation";

    private Retrofit retrofitTezos = new Retrofit.Builder()
            .baseUrl(letzbakeURI)
            .addConverterFactory(GsonConverterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            //logging for testing
            .client(new OkHttpClient.Builder().addInterceptor(
                    new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
            ).build())

            .build();

    private TezosApi tezosApi = retrofitTezos.create(TezosApi.class);

    private int requestsCount = 0;

    public boolean isRequestsSequenceCompleted() {
        Log.i(TAG, String.format("isRequestsSequenceCompleted: %s (%d requests left)", String.valueOf(requestsCount <= 0), requestsCount));
        return requestsCount <= 0;
    }

    public void getAddress(String wallet, SingleObserver<TezosAccountResponse> accountObserver) {
        requestsCount++;
        Log.i(TAG, "new getAddress request");

        Single<TezosAccountResponse> accountSingle = tezosApi.getAccount(wallet)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent((object, throwable) -> requestsCount--);

        accountSingle.subscribe(accountObserver);
    }

    public void getMangerKey(String wallet, SingleObserver<String> accountObserver) {
        requestsCount++;
        Log.i(TAG, "new getManagerKey request");

        Single<String> managerKeySingle = tezosApi.getManagerKey(wallet)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent((object, throwable) -> requestsCount--);

        managerKeySingle.subscribe(accountObserver);
    }

    public TezosHeaderResponse getHeader() throws Exception { // TODO? not async
        requestsCount++;
        Log.i(TAG, "new getHeader request");

        Response<TezosHeaderResponse> headerResponse = tezosApi.getHeader().execute();

        requestsCount--;
        if (headerResponse.code() == 200) {
            return headerResponse.body();
        } else {
            throw new Exception("Wrong header response, code: " + headerResponse.code());
        }
    }

    public String forgeOperations(TezosForgeBody tezosForgeBody) throws Exception { // TODO? not async
        requestsCount++;
        Log.i(TAG, "new forgeOperations request");

        Response<String> forgeResponse = tezosApi.forgeOperations(tezosForgeBody).execute();

        requestsCount--;
        if (forgeResponse.code() == 200) {
            return forgeResponse.body();
        } else {
            throw new Exception("Wrong forge response, code: " + forgeResponse.code());
        }
    }

    public void peapplyOperations(TezosPreapplyBody tezosPreapplyBody) throws Exception {
        Log.i(TAG, "new peapplyOperations request");

        List<TezosPreapplyBody> tezosPreapplyBodyList = new ArrayList<>();
        tezosPreapplyBodyList.add(tezosPreapplyBody);
        Response<Void> preapplyResponse = tezosApi.preapplyOperations(tezosPreapplyBodyList).execute();

        if (preapplyResponse.code() != 200) {
            String error = "Preapply error: unknown error";
            if (preapplyResponse.errorBody() != null) {
                error = "Preapply error: " + preapplyResponse.errorBody().string();
            }
            Log.e(TAG, error);
            throw new Exception(error);
        }
    }

    public void injectOperations(String txForSend, SingleObserver<Object> injectObserver) {
        requestsCount++;
        Log.i(TAG, "new injectOperations request");

        Single<Object> injectSingle = tezosApi.injectOperations(txForSend)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent((object, throwable) -> requestsCount--);

        injectSingle.subscribe(injectObserver);
    }
}
