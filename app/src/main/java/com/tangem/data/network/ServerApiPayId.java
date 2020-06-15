package com.tangem.data.network;

import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.tangem.App;
import com.tangem.data.network.model.PayIdResponse;
import com.tangem.tangem_card.util.Log;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ServerApiPayId {
    private static String TAG = ServerApiPayId.class.getSimpleName();

    private int requestsCount = 0;

    public boolean isRequestsSequenceCompleted() {
        Log.i(TAG, String.format("isRequestsSequenceCompleted: %s (%d requests left)", String.valueOf(requestsCount <= 0), requestsCount));
        return requestsCount <= 0;
    }

    public void getAddress(String payID, SingleObserver<PayIdResponse> addressObserver) {
        requestsCount++;
        Log.i(TAG, "new getAddress request");

        String[] addressParts = payID.split("\\$");
        String user = addressParts[0];
        String domain = addressParts[1];

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://" + domain + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        PayIdApi api = retrofit.create(PayIdApi.class);

        Single<PayIdResponse> addressSingle = api.getAddress(user)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEvent((object, throwable) -> requestsCount--);

        addressSingle.subscribe(addressObserver);
    }
}
