package com.tangem.wallet.eos;

import com.tangem.wallet.eos.EosPushTransactionRequest;

import io.jafka.jeos.core.response.chain.transaction.PushedTransaction;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface EosApiPush {
    @POST("/v1/chain/push_transaction")
    Call<PushedTransaction> pushTransaction(@Body EosPushTransactionRequest eosPushTransactionRequest);
}
