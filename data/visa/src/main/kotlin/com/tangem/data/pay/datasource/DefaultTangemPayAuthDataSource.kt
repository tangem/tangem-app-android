package com.tangem.data.pay.datasource

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.WithdrawalSignatureResult
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.visa.model.TangemPayInitialCredentials
import com.tangem.sdk.api.TangemSdkManager
import javax.inject.Inject

internal class DefaultTangemPayAuthDataSource @Inject constructor(
    private val tangemSdkManager: TangemSdkManager,
    private val tangemPayHotSdkManager: TangemPayHotSdkManager,
) : TangemPayAuthDataSource {

    override suspend fun produceInitialCredentials(
        userWallet: UserWallet,
    ): Either<Throwable, TangemPayInitialCredentials> {
        return when (userWallet) {
            is UserWallet.Cold -> tangemSdkManager.tangemPayProduceInitialCredentials(cardId = userWallet.cardId)
            is UserWallet.Hot -> tangemPayHotSdkManager.produceInitialCredentials(userWallet)
        }
    }

    override suspend fun getWithdrawalSignature(
        userWallet: UserWallet,
        hash: String,
    ): Either<Throwable, WithdrawalSignatureResult> {
        return when (userWallet) {
            is UserWallet.Cold -> tangemSdkManager.getWithdrawalSignature(cardId = userWallet.cardId, hash = hash)
            is UserWallet.Hot -> tangemPayHotSdkManager.getWithdrawalSignature(hotWallet = userWallet, hash = hash)
        }
    }
}