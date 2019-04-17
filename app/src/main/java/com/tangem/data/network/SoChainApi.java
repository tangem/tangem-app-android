package com.tangem.data.network;


import com.tangem.data.network.model.SoChain;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface SoChainApi {
    @GET(Server.ApiSoChain.Method.ADDRESS_BALANCE)
    Call<SoChain.Response.AddressBalance> getAddressBalance(@Path("network") String network, @Path("address") String address);

    @GET(Server.ApiSoChain.Method.UNSPENT_TX)
    Call<SoChain.Response.TxUnspent> getUnspentTx(@Path("network") String network, @Path("address") String address);

    @Headers("Content-Type: application/json")
    @POST(Server.ApiSoChain.Method.SEND_TRANSACTION)
    Call<SoChain.Response.SendTx> sendTransaction(@Path("network") String network, @Body SoChain.Request.SendTx body);
}
