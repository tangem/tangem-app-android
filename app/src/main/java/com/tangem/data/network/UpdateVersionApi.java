package com.tangem.data.network;

import com.tangem.data.network.model.CardVerifyBody;
import com.tangem.data.network.model.CardVerifyResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface UpdateVersionApi {
    @GET(Server.ApiUpdateVersion.Method.LAST_VERSION)
    Call<ResponseBody> getLastVersion();
}