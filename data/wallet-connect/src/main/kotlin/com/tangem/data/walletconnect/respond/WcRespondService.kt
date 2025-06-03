package com.tangem.data.walletconnect.respond

import arrow.core.Either
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest

interface WcRespondService {
    suspend fun respond(request: WcSdkSessionRequest, response: String): Either<Throwable, Unit>
    fun rejectRequestNonBlock(request: WcSdkSessionRequest, message: String = "")
}