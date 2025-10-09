package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.demo.DemoTransactionSender
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.mapToFeeError
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.walletmanager.WalletManagersFacade
import java.math.BigDecimal

/**
 * Use case to estimate transaction fee
 */
class EstimateFeeUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val demoConfig: DemoConfig,
) {
    suspend operator fun invoke(
        amount: BigDecimal,
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Either<GetFeeError, TransactionFee> {
        val amountData = amount.convertToSdkAmount(cryptoCurrencyStatus)
        val result = if (userWallet is UserWallet.Cold &&
            demoConfig.isDemoCardId(userWallet.scanResponse.card.cardId)
        ) {
            demoTransactionSender(userWallet, cryptoCurrencyStatus.currency).estimateFee(
                amount = amountData,
                destination = "",
            )
        } else {
            walletManagersFacade.estimateFee(
                amount = amountData,
                userWalletId = userWallet.walletId,
                network = cryptoCurrencyStatus.currency.network,
            )
        }

        return when (result) {
            is Result.Success -> result.data.right()
            is Result.Failure -> result.mapToFeeError().left()
            null -> GetFeeError.UnknownError.left()
        }
    }

    private suspend fun demoTransactionSender(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
    ): DemoTransactionSender {
        return DemoTransactionSender(
            walletManagersFacade
                .getOrCreateWalletManager(userWallet.walletId, cryptoCurrency.network)
                ?: error("WalletManager is null"),
        )
    }
}