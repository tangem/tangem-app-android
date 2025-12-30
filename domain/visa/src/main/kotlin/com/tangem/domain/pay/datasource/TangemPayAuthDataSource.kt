package com.tangem.domain.pay.datasource

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.WithdrawalSignatureResult
import com.tangem.domain.visa.model.TangemPayInitialCredentials

interface TangemPayAuthDataSource {

    suspend fun produceInitialCredentials(userWallet: UserWallet): Either<Throwable, TangemPayInitialCredentials>

    suspend fun getWithdrawalSignature(
        userWallet: UserWallet,
        hash: String,
    ): Either<Throwable, WithdrawalSignatureResult>
}