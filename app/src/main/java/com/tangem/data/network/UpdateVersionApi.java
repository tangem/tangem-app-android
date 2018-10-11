package com.tangem.data.network;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

public interface UpdateVersionApi {
    @GET(Server.ApiUpdateVersion.Method.LAST_VERSION)
    Call<ResponseBody> getLastVersion();
}