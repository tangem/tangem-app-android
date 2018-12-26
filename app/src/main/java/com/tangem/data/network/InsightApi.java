package com.tangem.data.network;

import com.tangem.data.network.model.InsightBody;
import com.tangem.data.network.model.InsightResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface InsightApi {
    @GET(ServerApiInsight.INSIGHT_ADDRESS)
    Call<InsightResponse> insightAddress(@Path("address") String address);

    @GET(ServerApiInsight.INSIGHT_UNSPENT_OUTPUTS)
    Call<InsightResponse> insightUnspent(@Path("address") String address);

    @GET(ServerApiInsight.INSIGHT_TRANSACTION)
    Call<InsightResponse> insightTransaction(@Path("transaction") String transaction);

    @GET(ServerApiInsight.INSIGHT_FEE)
    Call<InsightResponse> insightFee(@Query("nbBlocks") int nbBlocks);

    @Headers("Content-Type: application/json")
    @POST(ServerApiInsight.INSIGHT_SEND)
    Call<InsightResponse> insightSend(@Body InsightBody body );
}
