package com.tangem.data.network;

import com.tangem.data.network.model.TezosAccountResponse;
import com.tangem.data.network.model.TezosForgeBody;
import com.tangem.data.network.model.TezosHeaderResponse;
import com.tangem.data.network.model.TezosPreapplyBody;

import java.util.List;

import io.reactivex.Single;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface TezosApi {
    @GET(ServerApiTezos.TEZOS_ADDRESS)
    Single<TezosAccountResponse> getAccount(@Path("address") String address);

    @GET(ServerApiTezos.TEZOS_HEADER)
    Call<TezosHeaderResponse> getHeader();

    @GET(ServerApiTezos.TEZOS_MANAGER_KEY)
    Single<String> getManagerKey(@Path("address") String address);

    @POST(ServerApiTezos.TEZOS_FORGE_OPERATIONS)
    Call<String> forgeOperations(@Body TezosForgeBody tezosForgeBody);

    @POST(ServerApiTezos.TEZOS_PREAPPLY_OPERATIONS)
    Call<Void> preapplyOperations(@Body List<TezosPreapplyBody> tezosPreapplyBodyList);

    @POST(ServerApiTezos.TEZOS_INJECT_OPERATIONS)
    Single<Object> injectOperations(@Body String txForSend);
}
