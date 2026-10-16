package com.tangem.data.pay.datasource

import arrow.core.Either
import com.tangem.data.wallets.cold.UserWalletIdPreflightReadFilter
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.WithdrawalSignatureResult
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.visa.model.TangemPayInitialData
import com.tangem.domain.visa.model.MoneyAccountActivationData
import com.tangem.sdk.api.TangemSdkManager
import javax.inject.Inject

internal class DefaultTangemPayAuthDataSource @Inject constructor(
    private val tangemSdkManager: TangemSdkManager,
    private val tangemPayHotSdkManager: TangemPayHotSdkManager,
) : TangemPayAuthDataSource {

    override suspend fun produceInitialData(userWallet: UserWallet): Either<Throwable, TangemPayInitialData> {
        return when (userWallet) {
            is UserWallet.Cold -> {
                val preflightReadFilter = UserWalletIdPreflightReadFilter(userWallet)
                tangemSdkManager.tangemPayProduceInitialData(preflightReadFilter = preflightReadFilter)
            }
            is UserWallet.Hot -> tangemPayHotSdkManager.produceInitialData(userWallet)
        }
    }

    override suspend fun produceMoneyAccountData(
        userWallet: UserWallet,
    ): Either<Throwable, MoneyAccountActivationData> {
        return when (userWallet) {
            is UserWallet.Cold -> {
                val preflightReadFilter = UserWalletIdPreflightReadFilter(userWallet)
                tangemSdkManager.tangemPayProduceMoneyAccountData(preflightReadFilter = preflightReadFilter)
            }
            is UserWallet.Hot -> tangemPayHotSdkManager.produceMoneyAccountData(userWallet)
        }
    }

    override suspend fun getWithdrawalSignature(
        userWallet: UserWallet,
        hash: String,
    ): Either<Throwable, WithdrawalSignatureResult> {
        return when (userWallet) {
            is UserWallet.Cold -> {
                val preflightReadFilter = UserWalletIdPreflightReadFilter(userWallet)
                tangemSdkManager.getWithdrawalSignature(hash = hash, preflightReadFilter = preflightReadFilter)
            }
            is UserWallet.Hot -> tangemPayHotSdkManager.getWithdrawalSignature(hotWallet = userWallet, hash = hash)
        }
    }
}