package com.tangem.data.network;

import com.tangem.data.network.model.BitcoreBalance;
import com.tangem.data.network.model.BitcoreSendBody;
import com.tangem.data.network.model.BitcoreSendResponse;
import com.tangem.data.network.model.BitcoreUtxo;

import java.util.List;

import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface DucatusApi {
    @GET(Server.ApiDucatus.Method.BALANCE)
    Single<BitcoreBalance> ducatusBalance(@Path("address") String address);

    @GET(Server.ApiDucatus.Method.UTXO)
    Single<List<BitcoreUtxo>> ducatusUnspents(@Path("address") String address);

    @POST(Server.ApiDucatus.Method.SEND)
    Single<BitcoreSendResponse> ducatusSend(@Body BitcoreSendBody body);
}
