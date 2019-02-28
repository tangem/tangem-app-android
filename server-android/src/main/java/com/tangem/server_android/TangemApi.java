package com.tangem.server_android;

import com.tangem.server_android.model.CardVerifyAndGetInfo;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface TangemApi {
    @Headers("Content-Type: application/json")
    @POST(Server.ApiTangem.Method.VERIFY_AND_GET_INFO)
    Call<CardVerifyAndGetInfo.Response> getCardVerifyAndGetInfo(@Body CardVerifyAndGetInfo.Request requestBody);

    @GET(Server.ApiTangem.Method.ARTWORK)
    Call<ResponseBody> getArtwork(@Query("artworkId") String artworkId, @Query("CID") String CID, @Query("publicKey") String publicKey);

}