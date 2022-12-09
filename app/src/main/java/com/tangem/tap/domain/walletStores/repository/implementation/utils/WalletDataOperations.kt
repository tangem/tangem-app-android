package com.tangem.tap.domain.walletStores.repository.implementation.utils

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Wallet
import com.tangem.common.core.TangemError
import com.tangem.tap.domain.extensions.amountToCreateAccount
import com.tangem.tap.domain.getFirstToken
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.getPendingTransactions
import java.math.BigDecimal

internal fun WalletDataModel.updateWithFiatRate(
    fiatRate: BigDecimal?,
): WalletDataModel {
    return this.copy(
        fiatRate = fiatRate,
    )
}

internal fun List<WalletDataModel>.updateWithFiatRates(
    fiatRates: Map<String, Double>,
): List<WalletDataModel> {
    return this.map { walletData ->
        val rate = fiatRates[walletData.currency.coinId]?.toBigDecimal()
        walletData.updateWithFiatRate(rate)
    }
}

internal fun WalletDataModel.updateWithAmount(wallet: Wallet): WalletDataModel {
    val pendingTransactions = wallet.getPendingTransactions()
    return this.copy(
        status = when (val currency = this.currency) {
            is Currency.Blockchain -> {
                val amount = wallet.fundsAvailable(AmountType.Coin)
                if (pendingTransactions.isEmpty()) {
                    WalletDataModel.VerifiedOnline(
                        amount = amount,
                    )
                } else {
                    WalletDataModel.TransactionInProgress(
                        amount = amount,
                        pendingTransactions = pendingTransactions,
                    )
                }
            }
            is Currency.Token -> {
                val token = currency.token
                val amount = wallet.fundsAvailable(AmountType.Token(token))
                val hasTokenPendingTransactions = pendingTransactions
                    .any { it.transactionData.amount.currencySymbol == token.symbol }
                when {
                    hasTokenPendingTransactions -> {
                        WalletDataModel.TransactionInProgress(
                            amount = amount,
                            pendingTransactions = pendingTransactions,
                        )
                    }
                    pendingTransactions.isNotEmpty() -> {
                        WalletDataModel.SameCurrencyTransactionInProgress(
                            amount = amount,
                            pendingTransactions = pendingTransactions,
                        )
                    }
                    else -> {
                        WalletDataModel.VerifiedOnline(
                            amount = amount,
                        )
                    }
                }
            }
        },
    )
}

internal fun List<WalletDataModel>.updateWithAmounts(wallet: Wallet): List<WalletDataModel> {
    return this.map { walletData ->
        walletData.updateWithAmount(wallet)
    }
}

internal fun WalletDataModel.updateWithError(
    wallet: Wallet,
    error: TangemError,
): WalletDataModel {
    return this.copy(
        status = when (error) {
            is BlockchainSdkError.AccountNotFound -> {
                val amountToCreateAccount = wallet.blockchain
                    .amountToCreateAccount(wallet.getFirstToken())

                if (amountToCreateAccount != null) {
                    WalletDataModel.NoAccount(
                        amountToCreateAccount = amountToCreateAccount,
                    )
                } else {
                    WalletDataModel.Unreachable(
                        errorMessage = error.customMessage,
                    )
                }
            }
            else -> WalletDataModel.Unreachable(
                errorMessage = error.customMessage,
            )
        },
    )
}

internal fun List<WalletDataModel>.updateWithError(
    wallet: Wallet,
    error: TangemError,
): List<WalletDataModel> {
    return this.map { walletData ->
        walletData.updateWithError(wallet, error)
    }
}

internal fun WalletDataModel.updateWithSelf(
    newWalletData: WalletDataModel,
): WalletDataModel {
    val oldWalletData = this
    val oldStatus = oldWalletData.status
    return oldWalletData.copy(
        status = when (val newStatus = newWalletData.status) {
            is WalletDataModel.Loading -> when (oldStatus) {
                is WalletDataModel.MissedDerivation -> WalletDataModel.Loading
                else -> oldStatus.asRefreshing()
            }
            is WalletDataModel.MissedDerivation,
            is WalletDataModel.Refreshing,
            is WalletDataModel.NoAccount,
            is WalletDataModel.Unreachable,
            is WalletDataModel.SameCurrencyTransactionInProgress,
            is WalletDataModel.TransactionInProgress,
            is WalletDataModel.VerifiedOnline,
            -> newStatus
        },
        walletAddresses = newWalletData.walletAddresses,
        existentialDeposit = newWalletData.existentialDeposit,
        fiatRate = newWalletData.fiatRate ?: oldWalletData.fiatRate,
    )
}

internal fun List<WalletDataModel>.updateWithMissedDerivation(): List<WalletDataModel> {
    return this.map { walletData ->
        walletData.copy(
            status = WalletDataModel.MissedDerivation,
        )
    }
}

internal fun List<WalletDataModel>.updateWithUnreachable(): List<WalletDataModel> {
    return this.map { walletData ->
        walletData.copy(
            status = WalletDataModel.Unreachable(errorMessage = null),
        )
    }
}

internal fun List<WalletDataModel>.updateWithSelf(
    walletsData: List<WalletDataModel>,
): List<WalletDataModel> {
    val oldWalletsData = this
    val updatedWalletsData = arrayListOf<WalletDataModel>()

    walletsData.forEach { newWalletData ->
        val walletDataToUpdate = oldWalletsData.find(newWalletData::isSameWalletData)
        if (walletDataToUpdate != null) {
            updatedWalletsData.add(walletDataToUpdate.updateWithSelf(newWalletData))
        } else {
            updatedWalletsData.add(newWalletData)
        }
    }

    return updatedWalletsData
}

internal fun WalletDataModel.isSameWalletData(other: WalletDataModel): Boolean {
    return currency == other.currency
}
