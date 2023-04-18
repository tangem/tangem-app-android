package com.tangem.tap.domain.totalBalance.implementation

import com.tangem.tap.common.extensions.toFiatValue
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.domain.totalBalance.TotalFiatBalanceCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

internal class DefaultTotalFiatBalanceCalculator : TotalFiatBalanceCalculator {
    override suspend fun calculate(walletStores: List<WalletStoreModel>, initial: TotalFiatBalance): TotalFiatBalance {
        return calculateOrNull(walletStores) ?: initial
    }

    override suspend fun calculateOrNull(walletStores: List<WalletStoreModel>): TotalFiatBalance? {
        return if (walletStores.isEmpty()) {
            null
        } else {
            withContext(Dispatchers.Default) {
                val walletsData = walletStores
                    .asSequence()
                    .flatMap { it.walletsData }

                when (val status = walletsData.findStatus()) {
                    TotalFiatBalanceStatus.Loading -> TotalFiatBalance.Loading
                    TotalFiatBalanceStatus.Failed -> TotalFiatBalance.Failed
                    TotalFiatBalanceStatus.Warning,
                    TotalFiatBalanceStatus.Loaded,
                    -> TotalFiatBalance.Loaded(
                        amount = walletsData.calculateTotalFiatAmount(),
                        isWarning = status == TotalFiatBalanceStatus.Warning,
                    )
                }
            }
        }
    }

    private fun Sequence<WalletDataModel>.findStatus(): TotalFiatBalanceStatus {
        return this
            .mapToStatus()
            .reduce { prevStatus, newStatus ->
                getCurrentStatus(prevStatus, newStatus)
            }
    }

    private fun Sequence<WalletDataModel>.mapToStatus(): Sequence<TotalFiatBalanceStatus> {
        return this.map { walletData ->
            when (walletData.status) {
                is WalletDataModel.VerifiedOnline,
                is WalletDataModel.SameCurrencyTransactionInProgress,
                is WalletDataModel.TransactionInProgress,
                is WalletDataModel.NoAccount,
                -> if (walletData.isWarningCase()) {
                    TotalFiatBalanceStatus.Warning
                } else {
                    TotalFiatBalanceStatus.Loaded
                }
                is WalletDataModel.Unreachable,
                is WalletDataModel.MissedDerivation,
                -> TotalFiatBalanceStatus.Failed
                is WalletDataModel.Loading -> TotalFiatBalanceStatus.Loading
            }
        }
    }

    private fun Sequence<WalletDataModel>.calculateTotalFiatAmount(): BigDecimal {
        return this
            .filterNot { it.isWarningCase() }
            .map { walletData ->
                walletData.fiatRate
                    ?.takeUnless { walletData.status.isErrorStatus }
                    ?.let { walletData.status.amount.toFiatValue(it) }
                    ?: BigDecimal.ZERO
            }
            .reduce { acc, value ->
                acc + value
            }
    }

    private fun getCurrentStatus(
        prevStatus: TotalFiatBalanceStatus,
        newStatus: TotalFiatBalanceStatus,
    ): TotalFiatBalanceStatus {
        return TotalFiatBalanceStatus[minOf(prevStatus.ordinal, newStatus.ordinal)]
    }

    private fun WalletDataModel.isWarningCase(): Boolean = isCustom && fiatRate == null

    private enum class TotalFiatBalanceStatus {
        Loading,
        Failed,
        Warning,
        Loaded,
        ;

        companion object {
            private val allValues = values()

            operator fun get(index: Int) = allValues[index]
        }
    }
}
