package com.tangem.data.network;

import com.tangem.data.network.model.PayIdResponse;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;

public interface PayIdApi {
    @Headers({
            "Accept: application/xrpl-mainnet+json",
            "PayID-Version: 1.0"
    })
    @GET("{user}")
    Single<PayIdResponse> getAddress(@Path("user") String user);
}
