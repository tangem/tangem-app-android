package com.tangem.domain.pay.datasource

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.WithdrawalSignatureResult

interface TangemPayAuthDataSource {

    suspend fun getWithdrawalSignature(
        userWallet: UserWallet,
        hash: String,
    ): Either<Throwable, WithdrawalSignatureResult>
}