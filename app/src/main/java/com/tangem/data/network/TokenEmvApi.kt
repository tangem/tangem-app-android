package com.tangem.data.network

import com.tangem.data.network.model.TokenEmvGetTransferFeeAnswer
import com.tangem.data.network.model.TokenEmvGetTransferFeeBody
import com.tangem.data.network.model.TokenEmvTransferAnswer
import com.tangem.data.network.model.TokenEmvTransferBody
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST

interface TokenEmvApi {
    @POST("./card/transfer")
    fun transfer(@Body tokenEmvTransferBody: TokenEmvTransferBody): Single<TokenEmvTransferAnswer>
    @POST("./card/transfer/fee")
    fun getTransferFee(@Body tokenEmvGetTransferFeeBody: TokenEmvGetTransferFeeBody): Single<TokenEmvGetTransferFeeAnswer>
}