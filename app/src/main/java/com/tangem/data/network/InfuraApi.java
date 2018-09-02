package com.tangem.data.network;

import com.tangem.data.network.model.InfuraEthGasPriceBody;
import com.tangem.data.network.model.InfuraEthGasPriceResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface InfuraApi {
    @POST(Server.ApiInfura.Method.MAIN)
    Call<InfuraEthGasPriceResponse> ethGasPrice(@Header("Content-Type") String contentType, @Body InfuraEthGasPriceBody body);
}