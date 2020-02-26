package com.tangem.data.network;

import com.tangem.data.network.model.AdaliteBody;
import com.tangem.data.network.model.AdaliteResponse;
import com.tangem.data.network.model.AdaliteResponseUtxo;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface AdaliteApi {
    @GET(ServerApiAdalite.ADALITE_ADDRESS)
    Call<AdaliteResponse> adaliteAddress(@Path("address") String address);

    @Headers("Content-Type: application/json")
    @POST(ServerApiAdalite.ADALITE_UNSPENT_OUTPUTS)
    Call<AdaliteResponseUtxo> adaliteUnspent(@Body String address);

//    @GET(ServerApiAdalite.ADALITE_TRANSACTION)
//    Call<AdaliteResponse> adaliteTransaction(@Path("txId") String txId);

    @Headers("Content-Type: application/json")
    @POST(ServerApiAdalite.ADALITE_SEND)
    Call<String> adaliteSend(@Body AdaliteBody adaliteBody);
}
