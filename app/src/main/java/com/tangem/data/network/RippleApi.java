package com.tangem.data.network;

import com.tangem.data.network.model.RippleBody;
import com.tangem.data.network.model.RippleResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface RippleApi {
    @Headers("Content-Type: application/json")
    @POST("./")
    Call<RippleResponse> ripple(@Body RippleBody body);
}
