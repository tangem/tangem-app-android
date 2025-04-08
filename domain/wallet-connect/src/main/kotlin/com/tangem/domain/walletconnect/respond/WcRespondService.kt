package com.tangem.domain.walletconnect.respond

import arrow.core.Either
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest

interface WcRespondService {
    suspend fun respond(request: WcSdkSessionRequest, response: String): Either<Throwable, Unit>
    suspend fun rejectRequest(request: WcSdkSessionRequest, message: String = ""): Either<Throwable, Unit>
}