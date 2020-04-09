package com.tangem.data.network

import com.tangem.data.network.model.TokenEmvTransferBody
import io.reactivex.Completable
import retrofit2.http.Body
import retrofit2.http.POST

interface TokenEmvApi {
    @POST("./")
    fun transfer(@Body tokenEmvTransferBody: TokenEmvTransferBody): Completable
}