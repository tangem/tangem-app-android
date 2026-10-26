package com.tangem.domain.pay.datasource

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.WithdrawalSignatureResult
import com.tangem.domain.visa.model.TangemPayInitialData
import com.tangem.domain.visa.model.MoneyAccountActivationData

interface TangemPayAuthDataSource {

    suspend fun produceInitialData(userWallet: UserWallet): Either<Throwable, TangemPayInitialData>

    suspend fun produceMoneyAccountData(userWallet: UserWallet): Either<Throwable, MoneyAccountActivationData>

    suspend fun getWithdrawalSignature(
        userWallet: UserWallet,
        hash: String,
    ): Either<Throwable, WithdrawalSignatureResult>
}